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
package org.jupnp.support.model.item;

import static org.jupnp.support.model.DIDLObject.Property.UPNP;

import org.jupnp.support.model.Res;

/**
 * @author Christian Bauer
 */
public class AudioBroadcast extends AudioItem {

    public static final Class CLASS = new Class("object.item.audioItem.audioBroadcast");

    public AudioBroadcast() {
        setClazz(CLASS);
    }

    public AudioBroadcast(Item other) {
        super(other);
    }

    public AudioBroadcast(String id, String parentID, String title, String creator, Res... resource) {
        super(id, parentID, title, creator, resource);
        setClazz(CLASS);
    }

    public String getRegion() {
        return getFirstPropertyValue(UPNP.REGION.class);
    }

    public AudioBroadcast setRegion(String region) {
        replaceFirstProperty(new UPNP.REGION(region));
        return this;
    }

    public String getRadioCallSign() {
        return getFirstPropertyValue(UPNP.RADIO_CALL_SIGN.class);
    }

    public AudioBroadcast setRadioCallSign(String radioCallSign) {
        replaceFirstProperty(new UPNP.RADIO_CALL_SIGN(radioCallSign));
        return this;
    }

    public String getRadioStationID() {
        return getFirstPropertyValue(UPNP.RADIO_STATION_ID.class);
    }

    public AudioBroadcast setRadioStationID(String radioStationID) {
        replaceFirstProperty(new UPNP.RADIO_STATION_ID(radioStationID));
        return this;
    }

    public String getRadioBand() {
        return getFirstPropertyValue(UPNP.RADIO_BAND.class);
    }

    public AudioBroadcast setRadioBand(String radioBand) {
        replaceFirstProperty(new UPNP.RADIO_BAND(radioBand));
        return this;
    }

    public Integer getChannelNr() {
        return getFirstPropertyValue(UPNP.CHANNEL_NR.class);
    }

    public AudioBroadcast setChannelNr(Integer channelNr) {
        replaceFirstProperty(new UPNP.CHANNEL_NR(channelNr));
        return this;
    }
}
