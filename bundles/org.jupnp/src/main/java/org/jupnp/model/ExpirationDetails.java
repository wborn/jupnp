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
package org.jupnp.model;

import java.util.Date;

/**
 * @author Christian Bauer
 */
public class ExpirationDetails {

    public static final int UNLIMITED_AGE = 0;

    private int maxAgeSeconds = UNLIMITED_AGE;
    private long lastRefreshTimestampSeconds = getCurrentTimestampSeconds();

    private int renewAttempts;

    public ExpirationDetails() {
    }

    public ExpirationDetails(int maxAgeSeconds) {
        this.maxAgeSeconds = maxAgeSeconds;
    }

    public int getMaxAgeSeconds() {
        return maxAgeSeconds;
    }

    public long getLastRefreshTimestampSeconds() {
        return lastRefreshTimestampSeconds;
    }

    public void setLastRefreshTimestampSeconds(long lastRefreshTimestampSeconds) {
        this.lastRefreshTimestampSeconds = lastRefreshTimestampSeconds;
    }

    public void stampLastRefresh() {
        setLastRefreshTimestampSeconds(getCurrentTimestampSeconds());
    }

    public boolean hasExpired() {
        return hasExpired(false);
    }

    public void renewAttempted() {
        renewAttempts++;
    }

    public int getRenewAttempts() {
        return renewAttempts;
    }

    /**
     * @param halfTime If <code>true</code> then half maximum age is used to determine expiration.
     * @return <code>true</code> if the maximum age has been reached.
     */
    public boolean hasExpired(boolean halfTime) {
        // Note: Uses direct field access for performance reasons on Android
        return maxAgeSeconds != UNLIMITED_AGE
                && (lastRefreshTimestampSeconds + (maxAgeSeconds / (halfTime ? 2 : 1))) < getCurrentTimestampSeconds();
    }

    public long getSecondsUntilExpiration() {
        // Note: Uses direct field access for performance reasons on Android
        return maxAgeSeconds == UNLIMITED_AGE ? Integer.MAX_VALUE
                : (lastRefreshTimestampSeconds + maxAgeSeconds) - getCurrentTimestampSeconds();
    }

    protected long getCurrentTimestampSeconds() {
        return new Date().getTime() / 1000;
    }

    // Performance optimization on Android
    private static String simpleName = ExpirationDetails.class.getSimpleName();

    @Override
    public String toString() {
        return "(" + simpleName + ")" + " MAX AGE: " + maxAgeSeconds;
    }
}
