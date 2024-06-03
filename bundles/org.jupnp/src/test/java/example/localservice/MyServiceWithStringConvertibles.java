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

import java.net.URI;
import java.net.URL;
import java.util.List;

import org.jupnp.binding.annotations.*;
import org.jupnp.model.types.csv.CSV;
import org.jupnp.model.types.csv.CSVInteger;

@UpnpService(serviceId = @UpnpServiceId("MyService"), serviceType = @UpnpServiceType(namespace = "mydomain", value = "MyService"), stringConvertibleTypes = MyStringConvertible.class)
public class MyServiceWithStringConvertibles {

    @UpnpStateVariable
    private URL myURL;

    @UpnpStateVariable
    private URI myURI;

    @UpnpStateVariable(datatype = "string")
    private List<Integer> myNumbers;

    @UpnpStateVariable
    private MyStringConvertible myStringConvertible;

    @UpnpAction(out = @UpnpOutputArgument(name = "Out"))
    public URL getMyURL() {
        return myURL;
    }

    @UpnpAction
    public void setMyURL(@UpnpInputArgument(name = "In") URL myURL) {
        this.myURL = myURL;
    }

    @UpnpAction(out = @UpnpOutputArgument(name = "Out"))
    public URI getMyURI() {
        return myURI;
    }

    @UpnpAction
    public void setMyURI(@UpnpInputArgument(name = "In") URI myURI) {
        this.myURI = myURI;
    }

    @UpnpAction(out = @UpnpOutputArgument(name = "Out"))
    public CSV<Integer> getMyNumbers() {
        CSVInteger wrapper = new CSVInteger();
        if (myNumbers != null) {
            wrapper.addAll(myNumbers);
        }
        return wrapper;
    }

    @UpnpAction
    public void setMyNumbers(@UpnpInputArgument(name = "In") CSVInteger myNumbers) {
        this.myNumbers = myNumbers;
    }

    @UpnpAction(out = @UpnpOutputArgument(name = "Out"))
    public MyStringConvertible getMyStringConvertible() {
        return myStringConvertible;
    }

    @UpnpAction
    public void setMyStringConvertible(@UpnpInputArgument(name = "In") MyStringConvertible myStringConvertible) {
        this.myStringConvertible = myStringConvertible;
    }
}
