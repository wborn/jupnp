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

package org.jupnp.transport.impl.osgi;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Properties;
import java.util.concurrent.ExecutorService;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.jupnp.transport.spi.ServletContainerAdapter;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a servlet container adapter for an OSGi http service.
 * 
 * @author Kai Kreuzer
 *
 */
public class HttpServiceServletContainerAdapter implements
		ServletContainerAdapter {

	private final static Logger logger = LoggerFactory.getLogger(HttpServiceServletContainerAdapter.class);
	
	protected HttpService httpService;
	private String contextPath;
	
	public HttpServiceServletContainerAdapter(HttpService httpService) {
		this.httpService = httpService;
	}
	
	@Override
	public void setExecutorService(ExecutorService executorService) {
	}

	@Override
	public int addConnector(String host, int port) throws IOException {
		return 0;
	}

	@Override
	public void registerServlet(String contextPath, Servlet servlet) {
		Dictionary<?, ?> params = new Properties();
		try {
			logger.info("Registering UPnP callback servlet as {}", contextPath);
			httpService.registerServlet(contextPath, servlet, params, httpService.createDefaultHttpContext());
			this.contextPath = contextPath;
		} catch (ServletException e) {
			logger.error("Failed to register UPnP servlet!", e);
		} catch (NamespaceException e) {
			logger.error("Failed to register UPnP servlet!", e);
		}
	}

	@Override
	public void startIfNotRunning() {
	}

	@Override
	public void stopIfRunning() {
		if(contextPath!=null) {
			httpService.unregister(contextPath);
			contextPath = null;
		}
	}

}
