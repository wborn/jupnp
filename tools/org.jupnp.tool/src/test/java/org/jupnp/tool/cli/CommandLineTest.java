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

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class checks all command line options from tool.
 * 
 * It will use a "silent" tool which wil redirect System.out and System.err to
 * streams, and will check the "printed" result in some test cases.
 */
public class CommandLineTest extends AbstractTestCase {

	@Before
	public void setUp() {
		createSilentTool();
	}

	@After
	public void tearDown() throws Exception {
		releaseSilentTool();
	}

	@Test
	public void testShowUsage() {
		checkCommandLine(tool, JUPnPTool.RC_HELP, "-?");
		final String s = out.toString();
		System.out.println(s);
	}

	@Test
	public void testCommandLineHelpOK() {
		checkCommandLine(tool, JUPnPTool.RC_HELP, "");
		checkCommandLine(tool, JUPnPTool.RC_HELP, "-h");
		checkCommandLine(tool, JUPnPTool.RC_HELP, "-?");
		checkCommandLine(tool, JUPnPTool.RC_HELP, "--help");
		checkCommandLine(tool, JUPnPTool.RC_HELP, "-v");
		checkCommandLine(tool, JUPnPTool.RC_HELP, "-h -v");
		checkCommandLine(tool, JUPnPTool.RC_HELP, "--help --verbose");
		// check output, must contain 21x Usage
		final String s = out.toString();
		// -1 as there is a result BEFORE first Usage
		// *3 as each usage message contains 3x usage text
		Assert.assertEquals(7 * 3, s.split("Usage").length - 1);
	}

	@Test
	public void testCommandLineWrongOptionsProduceHelp() {
		checkCommandLine(tool, JUPnPTool.RC_INVALID_OPTION, "-x");
		checkCommandLine(tool, JUPnPTool.RC_INVALID_OPTION, "--xxx");
		checkCommandLine(tool, JUPnPTool.RC_INVALID_OPTION, "--blablub");
		// check output, must contain 9x usage
		final String s = out.toString();
		// -1 as there is a result BEFORE first Usage
		// *3 as each usage message contains 3x usage text
		Assert.assertEquals(3 * 3, s.split("Usage").length - 1);
		// check stderr, must contain 3x error message, and wrong options
		final String e = err.toString();
		// -1 as there is a result BEFORE first Usage
		Assert.assertEquals(3, e.split("Unknown").length - 1);
		Assert.assertTrue(e.contains("-x"));
		Assert.assertTrue(e.contains("--xxx"));
		Assert.assertTrue(e.contains("--blablub"));
	}

	@Test
	public void testCommandNop() {
		checkCommandLine(tool, JUPnPTool.RC_OK, "nop");
		// no output on console expected
		final String s = out.toString();
		Assert.assertEquals("", s);
	}

	@Test
	public void testLogLevelOK() {
		checkCommandLine(tool, JUPnPTool.RC_OK, "nop");
		checkCommandLine(tool, JUPnPTool.RC_OK, "-l=INFO nop");
		checkCommandLine(tool, JUPnPTool.RC_OK, "--loglevel=INFO nop");
		// no output on console expected
		final String s = out.toString();
		Assert.assertEquals("", s);
	}

	@Test
	public void testLogLevelWithWrongOptions() {
		checkCommandLine(tool, JUPnPTool.RC_INVALID_OPTION, "-l=XYZ nop");
		checkCommandLine(tool, JUPnPTool.RC_INVALID_OPTION,
				"--loglevel=ABC nop");
		// check output, must contain 6x usage
		final String s = out.toString();
		// -1 as there is a result BEFORE first Usage
		// *3 as each usage message contains 3x usage text
		Assert.assertEquals(2 * 3, s.split("Usage").length - 1);
	}

	/**
	 * Will check whether log messages will NOT be in stdout.
	 */
	@Test
	public void testLogLevelDisabled() {
		checkCommandLine(tool, JUPnPTool.RC_OK, "nop");
		Logger l = LoggerFactory.getLogger(this.getClass());
		l.info("Some Info");
		final String s = out.toString();
		Assert.assertEquals("", s);
	}

	/**
	 * Will check whether log messages will be in stdout.
	 */
	@Test
	public void testLogLevelEnabled() {
		checkCommandLine(tool, JUPnPTool.RC_OK, "--loglevel=INFO nop");
		Logger l = LoggerFactory.getLogger(this.getClass());
		l.info("Some Info");
		l.debug("Some Debug");
		final String s = out.toString();
		Assert.assertTrue(s
				.contains("INFO  org.jupnp.tool.cli.CommandLineTest - Some Info"));
		// DEBUG must NOT be included
		Assert.assertFalse(s.contains("DEBUG"));
	}

}
