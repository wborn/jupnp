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

import java.net.URI;
import java.net.URISyntaxException;

/**
 * @author Christian Bauer
 */
public class URIDatatype extends AbstractDatatype<URI> {

    public URIDatatype() {
    }

    @Override
    public URI valueOf(String s) throws InvalidValueException {
        if (s.isEmpty()) {
            return null;
        }
        try {
            return new URI(s);
        } catch (URISyntaxException e) {
            throw new InvalidValueException(e.getMessage(), e);
        }
    }
}
