/*
 * Copyright (C) 2011-2025 4th Line GmbH, Switzerland and others
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
 *
 * SPDX-License-Identifier: CDDL-1.0
 */
package org.jupnp.protocol;

/**
 * Recoverable error, thrown when no protocol is available to handle a UPnP message.
 *
 * @author Christian Bauer
 */
public class ProtocolCreationException extends Exception {

    public ProtocolCreationException(String s) {
        super(s);
    }

    public ProtocolCreationException(String s, Throwable throwable) {
        super(s, throwable);
    }
}
