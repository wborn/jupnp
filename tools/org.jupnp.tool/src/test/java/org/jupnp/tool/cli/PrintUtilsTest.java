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

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

/**
 * Test cases for PrintUtils test class.
 */
public class PrintUtilsTest {

	@Test
	public void testPrintTable() {
		List<String[]> table = new ArrayList<String[]>();
		table.add(new String[] { "IP", "Model", "SerialNumber" });
		table.add(new String[] { "==", "=====", "============" });
		table.add(new String[] { "192.168.2.1", "QIVICON", "3690012345" });
		table.add(new String[] { "192.168.2.2", "FRITZ!Box 7490", "-" });
		table.add(new String[] { "192.168.2.3", "DCS-932L", "-" });
		table.add(new String[] { "192.168.2.4", "Philips hue bridge 2012",
				"001788169b88" });
		table.add(new String[] { "192.168.2.5", "Sonos PLAY:3", "-" });
		String s = PrintUtils.printTable(table, 4);
		System.out.println(s);
	}

}
