/**
 * Copyright (C) 2014 4th Line GmbH, Switzerland and others
 *
 * The contents of this file are subject to the terms of either the
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

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

/**
 * @author Jochen Hiller - Initial contribution
 */
@Parameters(separators = "=", commandDescription = "jupnptool")
public class CommandLineArgs {

	@Parameter(names = { "--help", "-h", "-?" }, description = "Help how to use this tool", help = true)
	public Boolean doHelp = Boolean.FALSE;

	@Parameter(names = { "--loglevel", "-l" }, description = "Set LogLevel to {DEBUG|INFO|WARN|ERROR|OFF}", validateWith = MainCommandLogLevelValidator.class)
	public String logLevel = "DISABLED";

	@Parameter(names = { "--verbose", "-v" }, description = "Enable verbose messages")
	public Boolean verbose = Boolean.FALSE;

	public boolean isLoggingEnabled() {
		return !logLevel.equals("DISABLED");
	}

}

/**
 * @author Jochen Hiller - Initial contribution
 */
@Parameters(separators = "=", commandDescription = "Search for UPnP devices")
class SearchCommandArgs {

	@Parameter(names = { "--timeout", "-t" }, description = "The timeout when search will be finished")
	public Integer timeout = 10;

	@Parameter(names = { "--sort", "-s" }, description = "Sort using {none|ip|model|serialNumber|manufacturer|udn}", validateWith = SearchCommandSortByValidator.class)
	public String sortBy = "none";

	@Parameter(names = { "--filter", "-f" }, description = "Filter for devices containing this text (in some description)")
	public String filter = "*";
}

/**
 * @author Jochen Hiller - Initial contribution
 */
@Parameters(separators = "=", commandDescription = "No operation")
class NopCommandArgs {

}
