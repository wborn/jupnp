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
package org.jupnp.transport.spi;

import java.net.InetAddress;

/**
 * Collection of typically needed configuration settings.
 *
 * @author Christian Bauer
 */
public interface MulticastReceiverConfiguration {

    /**
     * @return The multicast group to join.
     */
    InetAddress getGroup();

    /**
     * @return The port to listen on.
     */
    int getPort();

    /**
     * @return The maximum buffer size of received UDP datagrams.
     */
    int getMaxDatagramBytes();
}
