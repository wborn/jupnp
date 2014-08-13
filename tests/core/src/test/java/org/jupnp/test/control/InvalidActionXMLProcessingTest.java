/*
 * Copyright (C) 2013 4th Line GmbH, Switzerland
 *
 * The contents of this file are subject to the terms of either the GNU
 * Lesser General Public License Version 2 or later ("LGPL") or the
 * Common Development and Distribution License Version 1 or later
 * ("CDDL") (collectively, the "License"). You may not use this file
 * except in compliance with the License. See LICENSE.txt for more
 * information.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */

package org.jupnp.test.control;

import static org.testng.Assert.assertEquals;

import java.net.URI;

import org.jupnp.UpnpService;
import org.jupnp.mock.MockUpnpService;
import org.jupnp.model.UnsupportedDataException;
import org.jupnp.model.action.ActionInvocation;
import org.jupnp.model.message.StreamRequestMessage;
import org.jupnp.model.message.UpnpRequest;
import org.jupnp.model.message.control.IncomingActionRequestMessage;
import org.jupnp.model.message.header.ContentTypeHeader;
import org.jupnp.model.message.header.SoapActionHeader;
import org.jupnp.model.message.header.UpnpHeader;
import org.jupnp.model.meta.Action;
import org.jupnp.model.meta.LocalDevice;
import org.jupnp.model.meta.LocalService;
import org.jupnp.model.types.SoapActionType;
import org.jupnp.util.io.IO;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * @author Christian Bauer
 */
public class InvalidActionXMLProcessingTest {

    @DataProvider(name = "invalidXMLFile")
    public String[][] getInvalidXMLFile() throws Exception {
        return new String[][]{
            {"/invalidxml/control/request_missing_envelope.xml"},
            {"/invalidxml/control/request_missing_action_namespace.xml"},
            {"/invalidxml/control/request_invalid_action_namespace.xml"},
        };
    }

    @DataProvider(name = "invalidRecoverableXMLFile")
    public String[][] getInvalidRecoverableXMLFile() throws Exception {
        return new String[][]{
            {"/invalidxml/control/request_no_entityencoding.xml"},
            {"/invalidxml/control/request_wrong_termination.xml"},
        };
    }

    @DataProvider(name = "invalidUnrecoverableXMLFile")
    public String[][] getInvalidUnrecoverableXMLFile() throws Exception {
        return new String[][]{
            {"/invalidxml/control/unrecoverable/naim_unity.xml"},
        };
    }

    /* ############################## TEST FAILURE ############################ */

    @Test(dataProvider = "invalidXMLFile", expectedExceptions = UnsupportedDataException.class)
    public void readRequestDefaultFailure(String invalidXMLFile) throws Exception {
        // This should always fail!
        readRequest(invalidXMLFile, new MockUpnpService());
    }

    @Test(dataProvider = "invalidRecoverableXMLFile", expectedExceptions = UnsupportedDataException.class)
    public void readRequestRecoverableFailure(String invalidXMLFile) throws Exception {
        // This should always fail!
        readRequest(invalidXMLFile, new MockUpnpService());
    }


    protected void readRequest(String invalidXMLFile, UpnpService upnpService) throws Exception {
        LocalDevice ld = ActionSampleData.createTestDevice(ActionSampleData.LocalTestServiceExtended.class);
        LocalService svc = ld.getServices()[0];

        Action action = svc.getAction("SetSomeValue");
        ActionInvocation actionInvocation = new ActionInvocation(action);

        StreamRequestMessage message = createRequestMessage(action, invalidXMLFile);
        IncomingActionRequestMessage request = new IncomingActionRequestMessage(message, svc);

        upnpService.getConfiguration().getSoapActionProcessor().readBody(request, actionInvocation);

        assertEquals(actionInvocation.getInput()[0].toString(), "foo&bar");
    }

    public StreamRequestMessage createRequestMessage(Action action, String xmlFile) throws Exception {
        StreamRequestMessage message =
            new StreamRequestMessage(UpnpRequest.Method.POST, URI.create("http://some.uri"));

        message.getHeaders().add(
            UpnpHeader.Type.CONTENT_TYPE,
            new ContentTypeHeader(ContentTypeHeader.DEFAULT_CONTENT_TYPE_UTF8)
        );
        message.getHeaders().add(
            UpnpHeader.Type.SOAPACTION,
            new SoapActionHeader(
                new SoapActionType(
                    action.getService().getServiceType(),
                    action.getName()
                )
            )
        );
        message.setBody(IO.readLines(getClass().getResourceAsStream(xmlFile)));
        return message;
    }
}
