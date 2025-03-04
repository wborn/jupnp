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
package org.jupnp.model.types;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * @author Christian Bauer
 */
public class DateTimeDatatype extends AbstractDatatype<Calendar> {

    protected String[] readFormats;
    protected String writeFormat;

    public DateTimeDatatype(String[] readFormats, String writeFormat) {
        this.readFormats = readFormats;
        this.writeFormat = writeFormat;
    }

    @Override
    public Calendar valueOf(String s) throws InvalidValueException {
        if (s.isEmpty()) {
            return null;
        }

        Date d = getDateValue(s, readFormats);
        if (d == null) {
            throw new InvalidValueException("Can't parse date/time from: " + s);
        }

        Calendar c = Calendar.getInstance(getTimeZone());
        c.setTime(d);

        return c;
    }

    @Override
    public String getString(Calendar value) throws InvalidValueException {
        if (value == null) {
            return "";
        }
        SimpleDateFormat sdt = new SimpleDateFormat(writeFormat);
        sdt.setTimeZone(getTimeZone());
        return sdt.format(value.getTime());
    }

    protected String normalizeTimeZone(String value) {
        if (value.endsWith("Z")) {
            value = value.substring(0, value.length() - 1) + "+0000";
        } else if ((value.length() > 7) && (value.charAt(value.length() - 3) == ':')
                && ((value.charAt(value.length() - 6) == '-') || (value.charAt(value.length() - 6) == '+'))) {

            value = value.substring(0, value.length() - 3) + value.substring(value.length() - 2);
        }
        return value;
    }

    protected Date getDateValue(String value, String[] formats) {

        value = normalizeTimeZone(value);

        Date d = null;
        for (String format : formats) {
            SimpleDateFormat sdt = new SimpleDateFormat(format);
            sdt.setTimeZone(getTimeZone());
            try {
                d = sdt.parse(value);
                // Continue, last match is the one we need
            } catch (ParseException e) {
                // Just continue
            }
        }
        return d;
    }

    protected TimeZone getTimeZone() {
        return TimeZone.getDefault();
    }
}
