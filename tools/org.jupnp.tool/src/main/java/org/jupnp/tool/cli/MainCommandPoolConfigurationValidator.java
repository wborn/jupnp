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

import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.ParameterException;

/**
 * @author Jochen Hiller - Initial contribution
 */
public class MainCommandPoolConfigurationValidator implements IParameterValidator {

	private final static String ERROR_MSG = "Paramer --pool must be of format "
			+ "'<corePoolSize>,<maxPoolSize>,<queueSize>,<timeout>{ms|s|m}[,stats]'";

	public void validate(String name, String value) throws ParameterException {
		if (name.equals("--pool")) {
			// pool config is sth like "20,40,1000,stats"
			StringTokenizer tokenizer = new StringTokenizer(value, ",");
			// must have 3..4 args
			if ((tokenizer.countTokens() < 4) || (tokenizer.countTokens() > 5)) {
				throw new ParameterException(ERROR_MSG + " (not 4 or 5 parameters)");
			} else {
				try {
					int corePoolSize = new Integer(tokenizer.nextToken()).intValue();
					int maxPoolSize = new Integer(tokenizer.nextToken()).intValue();
					int queueSize = new Integer(tokenizer.nextToken()).intValue();

					String timeoutAsString = tokenizer.nextToken().trim();
					long timeout = 10000L; // in ms
					if (timeoutAsString.endsWith("ms")) {
						String s = timeoutAsString.substring(0, timeoutAsString.indexOf("ms")).trim();
						timeout = Integer.valueOf(s) * 1L;
					} else if (timeoutAsString.endsWith("s")) {
						String s = timeoutAsString.substring(0, timeoutAsString.indexOf("s")).trim();
						timeout = Integer.valueOf(s) * 1000L;
					} else if (timeoutAsString.endsWith("m")) {
						String s = timeoutAsString.substring(0, timeoutAsString.indexOf("m")).trim();
						timeout = Integer.valueOf(s) * 60L * 1000L;
					} else {
						// we assume ms as default
						timeout = Integer.valueOf(timeoutAsString) * 1L;
					}

					// all >0
					if ((corePoolSize <= 0) || (maxPoolSize <= 0) || (queueSize <= 0)) {
						throw new ParameterException(ERROR_MSG + " (all values must be greater than 0)");
					}
					// core < max
					if (corePoolSize > maxPoolSize) {
						throw new ParameterException(ERROR_MSG + " (max must be greater than core)");
					}
					// timeout >0
					if (timeout <= 0L) {
						throw new ParameterException(ERROR_MSG + " (timeout must be greater than 0)");
					}
					// one token left?
					if (tokenizer.countTokens() == 1) {
						String option = tokenizer.nextToken();
						if (!CommandLineArgs.POOL_CONFIG_STATS_OPTION.equalsIgnoreCase(option)) {
							throw new ParameterException(ERROR_MSG + " (only "
									+ CommandLineArgs.POOL_CONFIG_STATS_OPTION + " allowed as last option)");
						}
					}
					// all fine otherwise
				} catch (NoSuchElementException ex) {
					// hmm, not enough tokens, should never happen
					throw new ParameterException(ERROR_MSG + " (not enough arguments)");
				} catch (NumberFormatException ex) {
					// must be valid numbers
					throw new ParameterException(ERROR_MSG + " (numbers wrong)");
				}
			}
		}
	}
}
