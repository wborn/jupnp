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
package org.jupnp.support.renderingcontrol.lastchange;

import org.jupnp.support.model.Channel;

/**
 * @author Christian Bauer
 */
public class ChannelVolumeDB {

    protected Channel channel;
    protected Integer volumeDB;

    public ChannelVolumeDB(Channel channel, Integer volumeDB) {
        this.channel = channel;
        this.volumeDB = volumeDB;
    }

    public Channel getChannel() {
        return channel;
    }

    public Integer getVolumeDB() {
        return volumeDB;
    }

    @Override
    public String toString() {
        return "VolumeDB: " + getVolumeDB() + " (" + getChannel() + ")";
    }
}
