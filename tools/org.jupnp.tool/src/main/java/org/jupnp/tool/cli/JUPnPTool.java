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

import java.io.PrintStream;
import java.net.URL;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;

/**
 * @author Jochen Hiller - Initial contribution
 */
public class JUPnPTool {

	public static final int RC_OK = 0;
	public static final int RC_HELP = 1;
	public static final int RC_INVALID_OPTION = 2;

	public static final String TOOL_NAME = "jupnptool";
	// TODO how to get build number?
	public static final String TOOL_VERSION = "2.0.0.SNAPSHOT";

	private static final String COMMAND_SEARCH = "search";
	private static final String COMMAND_NOP = "nop";

	private Logger logger = LoggerFactory.getLogger(JUPnPTool.class);

	protected PrintStream outputStream;
	protected PrintStream errorStream;

	public static void main(String[] args) {
		JUPnPTool tool = new JUPnPTool();
		int rc = tool.doMain(args);
		System.exit(rc);
	}

	public JUPnPTool() {
		this(System.out, System.err);
	}

	public JUPnPTool(PrintStream out, PrintStream err) {
		this.outputStream = out;
		this.errorStream = err;
	}

	public int doMain(String[] args) {
		// parse command line arguments with jCommander
		JCommander commander = new JCommander(new CommandLineArgs());
		commander.addCommand(COMMAND_SEARCH, new SearchCommandArgs());
		commander.addCommand(COMMAND_NOP, new NopCommandArgs());
		commander.setProgramName(TOOL_NAME);
		try {
			commander.parse(args);
		} catch (ParameterException ex) {
			printStderr(ex.getLocalizedMessage());
			printToolUsage(commander);
			return RC_INVALID_OPTION;
		}
		List<Object> objs = commander.getObjects();
		CommandLineArgs cmdLineArgs = (CommandLineArgs) objs.get(0);

		// if logging enabled, use other logback XML file
		if (cmdLineArgs.isLoggingEnabled()) {
			setLogging("logback-enabled.xml", cmdLineArgs.logLevel);
		} else {
			setLogging("logback.xml", "OFF");
		}

		// dispatch commands
		if (cmdLineArgs.doHelp) {
			printToolUsage(commander);
			return RC_HELP;
		} else if (COMMAND_SEARCH.equals(commander.getParsedCommand())) {
			JCommander searchCommander = commander.getCommands().get(
					COMMAND_SEARCH);
			SearchCommandArgs searchArgs = (SearchCommandArgs) searchCommander
					.getObjects().get(0);
			int timeout = searchArgs.timeout;
			String sortBy = searchArgs.sortBy;
			String filter = searchArgs.filter;
			boolean verbose = cmdLineArgs.verbose;
			// if udn or manufacturer will be specified: auto-enable verbose
			if (("udn".equals(sortBy)) || ("manufacturer".equals(sortBy))) {
				verbose = true;
			}

			printToolStartMessage("Search for UPnP devices for " + timeout
					+ " seconds sorted by " + sortBy + " and filtered by "
					+ filter);
			SearchCommand cmd = new SearchCommand(this);
			int rc = cmd.run(timeout, sortBy, filter, verbose);
			return rc;
		} else if (COMMAND_NOP.equals(commander.getParsedCommand())) {
			return RC_OK;
		} else {
			printToolUsage(commander);
			return RC_HELP;
		}
	}

	// protected methods

	/**
	 * Sets the logger to the resource name, and reset logback configuration.
	 * 
	 * @param resourceName
	 *            either logback.xml, or logback-enabled.xml
	 */
	protected void setLogging(String resourceName, String rootAppenderLogLevel) {
		LoggerContext context = (LoggerContext) LoggerFactory
				.getILoggerFactory();
		try {
			// will assume to find logback XML files in root of JAR file
			URL url = this.getClass().getResource("/" + resourceName);
			JoranConfigurator configurator = new JoranConfigurator();
			configurator.setContext(context);

			// Call context.reset() to clear any previous configuration, e.g.
			// default configuration
			context.reset();
			configurator.doConfigure(url);

			ch.qos.logback.classic.Logger rootLogger = (ch.qos.logback.classic.Logger) LoggerFactory
					.getLogger(Logger.ROOT_LOGGER_NAME);
			Level level = Level.valueOf(rootAppenderLogLevel);
			rootLogger.setLevel(level);

		} catch (JoranException je) {
			// StatusPrinter will handle this
		}
		// see https://issues.apache.org/jira/browse/SLING-3045
		// there can by sync issues when reconfiguring logback
		long now = new Date().getTime();
		StatusPrinter.printInCaseOfErrorsOrWarnings(context, now + 1000);
	}

	// package methods

	void printStdout(String msg) {
		this.outputStream.println(msg);
	}

	void printStderr(String msg) {
		this.errorStream.println(msg);
	}

	// private methods

	private void printToolStartMessage(String msg) {
		printStdout(TOOL_NAME + " (" + TOOL_VERSION + "): " + msg);
	}

	private void printToolUsage(JCommander commander) {
		StringBuilder sb = new StringBuilder();
		commander.usage(sb);
		printStdout(sb.toString());
	}
}
