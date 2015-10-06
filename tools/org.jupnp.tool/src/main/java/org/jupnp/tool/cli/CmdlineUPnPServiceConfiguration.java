/**
 * Copyright (C) 2014 4th Line GmbH, Switzerland and others
 *
 * The contents of this file are subject to the terms of the
 * Common Development and Distribution License Version 1 or later
 * ("CDDL") (collectively, the "License"). You may not use this file
 * except in compliance with the License. See LICENSE.txt for more
 * information.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */

package org.jupnp.tool.cli;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.jupnp.DefaultUpnpServiceConfiguration;
import org.jupnp.transport.impl.NetworkAddressFactoryImpl;
import org.jupnp.transport.spi.NetworkAddressFactory;
import org.jupnp.util.Exceptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class configures the behavior of jUPnP used in junpnptool.
 * 
 * It will allow to observe the processing by getting statistical information
 * about processing. It will also log rejected execution in a smarter way to not
 * flood logging with too much log messages.
 * 
 * Note: It allows to configure the behavior by static attributes, which can NOT
 * be changed after constructing the services.
 */
public class CmdlineUPnPServiceConfiguration extends DefaultUpnpServiceConfiguration {

	private static final transient Logger logger = LoggerFactory.getLogger(DefaultUpnpServiceConfiguration.class);
	private static final transient Logger statsLogger = LoggerFactory.getLogger("org.jupnp.tool.cli.stats");

	private static boolean DEBUG_STATISTICS = false;

	private static int THREAD_CORE_POOL_SIZE = 100;
	private static int THREAD_MAX_POOL_SIZE = THREAD_CORE_POOL_SIZE * 2;
	private static int THREAD_QUEUE_SIZE = 1000;

	private static long TIMEOUT_SECONDS = 10L;

	private static int MULTICAST_RESPONSE_LISTEN_PORT = NetworkAddressFactoryImpl.DEFAULT_MULTICAST_RESPONSE_LISTEN_PORT;

	// class methods to configure behavior

	public static void setDebugStatistics(boolean onOrOff) {
		DEBUG_STATISTICS = onOrOff;
	}

	public static void setTimeout(long seconds) {
		TIMEOUT_SECONDS = seconds;
	}

	public static void setPoolConfiguration(int core, int max, int queue) {
		THREAD_CORE_POOL_SIZE = core;
		THREAD_MAX_POOL_SIZE = max;
		THREAD_QUEUE_SIZE = queue;
	}

	public static void setMulticastResponsePort(Integer port) {
		MULTICAST_RESPONSE_LISTEN_PORT = port.intValue();
	}

	// instance methods

	public CmdlineUPnPServiceConfiguration() {
		super();
	}

	/**
	 * We create a network address factory based on configured multicast port.
	 */
	@Override
	public NetworkAddressFactory createNetworkAddressFactory() {
		return super.createNetworkAddressFactory(NetworkAddressFactoryImpl.DEFAULT_TCP_HTTP_LISTEN_PORT,
				MULTICAST_RESPONSE_LISTEN_PORT);
	}

	/**
	 * We create our own executor service, which collects statistics and does
	 * smart reject logging.
	 */
	protected ExecutorService createDefaultExecutorService() {
		return new JUPnPExecutor();
	}

	// inner classes

	/**
	 * This class executes threads and collects statistics information.
	 */
	public static class JUPnPExecutor extends ThreadPoolExecutor {

		/** Statistical data collected. */
		private Statistics stats;

		public JUPnPExecutor() {
			this(new JUPnPThreadFactory(), new SmartLoggingDiscardPolicy());
		}

		public JUPnPExecutor(ThreadFactory threadFactory, RejectedExecutionHandler rejectedHandler) {
			// This is the same as Executors.newCachedThreadPool
			// define timeout when tasks can not be executed to given timeout
			super(THREAD_CORE_POOL_SIZE, THREAD_MAX_POOL_SIZE, TIMEOUT_SECONDS, TimeUnit.SECONDS,
					new ArrayBlockingQueue<Runnable>(THREAD_QUEUE_SIZE), threadFactory, rejectedHandler);
			allowCoreThreadTimeOut(true);
			logger.debug("Created Executor with core=" + THREAD_CORE_POOL_SIZE + ", max=" + THREAD_MAX_POOL_SIZE
					+ ", queue=" + THREAD_QUEUE_SIZE + ", timeout=" + TIMEOUT_SECONDS);
			if (DEBUG_STATISTICS) {
				stats = new Statistics();
			}
		}

		@Override
		protected void beforeExecute(Thread t, Runnable r) {
			if (DEBUG_STATISTICS) {
				stats.addCurrentPoolSize(this);
				stats.addExcecutor(r);
			}
			// TODO why so much executors?
			// if (getQueue().size() > 100) {
			// System.out.println("ALERT");
			// }
			super.beforeExecute(t, r);
		}

		@Override
		protected void afterExecute(Runnable runnable, Throwable throwable) {
			super.afterExecute(runnable, throwable);
			if (throwable != null) {
				Throwable cause = Exceptions.unwrap(throwable);
				if ((cause instanceof InterruptedException) && isTerminating()) {
					// Ignore this, might happen when we shutdownNow() the
					// executor. We can't
					// log at this point as the logging system might be stopped
					// already (e.g. if it's a CDI component).
					return;
				}
				// Log only
				logger.warn("Thread terminated " + runnable + " abruptly with exception: " + throwable);
				logger.warn("  Root cause: " + cause);
			}
		}

		@Override
		public void shutdown() {
			logger.info("shutdown");
			super.shutdown();
			if (DEBUG_STATISTICS) {
				stats.dumpPoolStats();
				stats.dumpExecutorsStats();
				stats.release();
			}
			logger.info("shutdown done");
		}

		@Override
		public List<Runnable> shutdownNow() {
			logger.info("shutdownNow");
			List<Runnable> res = super.shutdownNow();
			if (DEBUG_STATISTICS) {
				stats.dumpPoolStats();
				stats.dumpExecutorsStats();
				stats.release();
			}
			logger.info("shutdownNow done");
			return res;
		}

		// inner classes for statistics

		static class Statistics {

			static class PoolStatPoint {
				public long timestamp, completedTasks;
				public int corePoolSize, poolSize, maxPoolSize, activeCounts, queueSize;
			}

			/** Thread safe collection for points. */
			private List<PoolStatPoint> points = new CopyOnWriteArrayList<PoolStatPoint>();

			/** Thread safe collection for executors. */
			private ConcurrentHashMap<String, AtomicInteger> executors = new ConcurrentHashMap<String, AtomicInteger>();

			/**
			 * Add info about current pool status.
			 */
			private void addCurrentPoolSize(ThreadPoolExecutor pool) {
				PoolStatPoint p = new PoolStatPoint();
				p.timestamp = System.currentTimeMillis();
				p.corePoolSize = pool.getCorePoolSize();
				p.poolSize = pool.getPoolSize();
				p.maxPoolSize = pool.getMaximumPoolSize();
				p.activeCounts = pool.getActiveCount();
				p.queueSize = pool.getQueue().size();
				p.completedTasks = pool.getCompletedTaskCount();
				points.add(p);
			}

			/**
			 * Increase number of calls to this runnable (by class name).
			 */
			public void addExcecutor(Runnable r) {
				executors.putIfAbsent(r.getClass().getName(), new AtomicInteger(0));
				executors.get(r.getClass().getName()).incrementAndGet();
			}

			public void release() {
				points = null;
				executors = null;
			}

			public void dumpPoolStats() {
				statsLogger.info("Dump Pool Statistics:");
				statsLogger.info("[timestamp,corePoolSize,poolSize,maxPoolSize,activeCounts,queueSize,completedTasks]");
				for (Iterator<PoolStatPoint> iter = points.iterator(); iter.hasNext();) {
					PoolStatPoint p = iter.next();
					statsLogger.info("" + p.timestamp + "," + p.corePoolSize + "," + p.poolSize + "," + p.maxPoolSize
							+ "," + p.activeCounts + "," + p.queueSize + "," + p.completedTasks);
				}
			}

			public void dumpExecutorsStats() {
				statsLogger.info("Dump Pool Executors:");

				List<ConcurrentHashMap.Entry<String, AtomicInteger>> entries = new ArrayList<ConcurrentHashMap.Entry<String, AtomicInteger>>(
						executors.entrySet());
				// sort the entries by number of calls
				Collections.sort(entries, new Comparator<ConcurrentHashMap.Entry<String, AtomicInteger>>() {
					public int compare(ConcurrentHashMap.Entry<String, AtomicInteger> a,
							ConcurrentHashMap.Entry<String, AtomicInteger> b) {
						return Integer.compare(b.getValue().get(), a.getValue().get());
					}
				});

				statsLogger.info("[executorClassName,numberOfExecutes]");
				for (ConcurrentHashMap.Entry<String, AtomicInteger> e : entries) {
					statsLogger.info(e.getKey() + "," + e.getValue().get());
				}
			}

		}
	}

	/**
	 * This class implements a rejection handler which logs rejects smart. Logs
	 * once happens one error message, and further messages report only number
	 * of rejections.
	 * 
	 * This class locks against multiple usage to have clea log messages. This
	 * may have influence during runtime, as logging will be done during holder
	 * a lock.
	 */
	public static class SmartLoggingDiscardPolicy extends ThreadPoolExecutor.DiscardPolicy {

		private static Lock rejectLock = new ReentrantLock();

		private static boolean displayedErrorOnce = false;
		private static String lastRejectedClass = null;
		private static final int MAX_NO_OF_REJECTS_TO_LOG = 20;
		private static int noOfRejects = 0;

		// The pool is bounded and rejections will happen during shutdown
		@Override
		public void rejectedExecution(Runnable runnable, ThreadPoolExecutor threadPoolExecutor) {
			if (threadPoolExecutor.isTerminating()) {
				// do log rejects during shutdown in debug level only
				logger.debug("Thread pool rejected during termination execution of " + runnable.toString());
			} else {
				try {
					rejectLock.lock();
					if (displayedErrorOnce == false) {
						logger.error("Thread pool rejected executions, consider to resize pool sizing");
						displayedErrorOnce = true;
					}
					// check for changed runnable class names
					if ((lastRejectedClass == null) || (!lastRejectedClass.equals(runnable.getClass().getName()))) {
						logger.warn("Thread pool rejected execution of " + runnable.toString());
						noOfRejects = 0;
						lastRejectedClass = runnable.getClass().getName();
					} else {
						// same runnable class name, increment number of calls
						noOfRejects = noOfRejects + 1;
						if (noOfRejects >= MAX_NO_OF_REJECTS_TO_LOG) {
							logger.warn("Thread pool rejected execution of (" + noOfRejects + " times) "
									+ runnable.toString());
							noOfRejects = 0;
						}
					}
				} finally {
					rejectLock.unlock();
				}
			}
			super.rejectedExecution(runnable, threadPoolExecutor);
		}
	}

}
