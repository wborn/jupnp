/*
 * Copyright (C) 2011-2024 4th Line GmbH, Switzerland and others
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
package example.localservice;

import org.jupnp.binding.AllowedValueProvider;
import org.jupnp.binding.annotations.UpnpAction;
import org.jupnp.binding.annotations.UpnpInputArgument;
import org.jupnp.binding.annotations.UpnpOutputArgument;
import org.jupnp.binding.annotations.UpnpService;
import org.jupnp.binding.annotations.UpnpServiceId;
import org.jupnp.binding.annotations.UpnpServiceType;
import org.jupnp.binding.annotations.UpnpStateVariable;

@UpnpService(serviceId = @UpnpServiceId("MyService"), serviceType = @UpnpServiceType(namespace = "mydomain", value = "MyService"))
public class MyServiceWithAllowedValueProvider {

    public static class MyAllowedValueProvider implements AllowedValueProvider {
        @Override
        public String[] getValues() {
            return new String[] { "Foo", "Bar", "Baz" };
        }
    }

    @UpnpStateVariable(allowedValueProvider = MyAllowedValueProvider.class)
    private String restricted;

    @UpnpAction(out = @UpnpOutputArgument(name = "Out"))
    public String getRestricted() {
        return restricted;
    }

    @UpnpAction
    public void setRestricted(@UpnpInputArgument(name = "In") String restricted) {
        this.restricted = restricted;
    }
}
