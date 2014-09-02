/**
 * Copyright (C) 2014 4th Line GmbH, Switzerland and others
 *
 * The contents of this file are subject to the terms of either the
 * Common Development and Distribution License Version 1 or later
 * ("CDDL") (collectively, the "License"). You may not use this file
 * except in compliance with the License. See LICENSE.txt for more
 * information.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */

package org.jupnp.osgi.discover;

import org.osgi.service.upnp.UPnPDevice;
import org.osgi.service.upnp.UPnPEventListener;
import org.jupnp.controlpoint.SubscriptionCallback;
import org.jupnp.model.UnsupportedDataException;
import org.jupnp.model.gena.CancelReason;
import org.jupnp.model.gena.GENASubscription;
import org.jupnp.model.gena.RemoteGENASubscription;
import org.jupnp.model.message.UpnpResponse;
import org.jupnp.model.state.StateVariableValue;
import org.jupnp.osgi.impl.UPnPDeviceImpl;
import org.jupnp.osgi.impl.UPnPServiceImpl;
import org.jupnp.osgi.util.OSGiDataConverter;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;

/**
 * Adapter between jUPnP GENA subscription and OSGi UPnPEventListener.
 *
 * @author Bruce Green
 */
public class UPnPEventListenerSubscriptionCallback extends SubscriptionCallback {

    private UPnPDeviceImpl device;
    private UPnPServiceImpl service;
    private UPnPEventListener listener;

    protected UPnPEventListenerSubscriptionCallback(UPnPDeviceImpl device, UPnPServiceImpl service, UPnPEventListener listener) {
        super(service.getService());

        this.device = device;
        this.service = service;
        this.listener = listener;
    }

    private String getDeviceId() {
        return (String) device.getDescriptions(null).get(UPnPDevice.UDN);
    }

    private String getServiceId() {
        return service.getId();
    }

    @Override
    protected void failed(GENASubscription subscription, UpnpResponse responseStatus, Exception exception, String defaultMsg) {
        log.severe(String.format("Failed to establish subscription for device %s service %s.",
                                 getDeviceId(),
                                 getServiceId()
        ));

        if (responseStatus != null) {
            log.severe(String.format("Response status code: %d", responseStatus.getStatusCode()));
            log.severe(String.format("Response status message: %s", responseStatus.getStatusMessage()));
            log.severe(String.format("Response details: %s", responseStatus.getResponseDetails()));
        }
        if (exception != null) {
            log.severe(String.format("Exception: %s", exception.getMessage()));
        }
        log.severe(String.format("Default message: %s", defaultMsg));
    }

    @Override
    protected void established(GENASubscription subscription) {
        log.finer(String.format(
                "Established subscription %s for device %s service %s.",
                subscription.getSubscriptionId(),
                getDeviceId(),
                getServiceId()
        ));
    }

    @Override
    protected void ended(GENASubscription subscription, CancelReason reason, UpnpResponse responseStatus) {
        if (reason == null) {
            log.finer(String.format(
                    "Subscription %s for device %s service %s ended.",
                    subscription.getSubscriptionId(),
                    getDeviceId(),
                    getServiceId()
            ));
        } else {
            log.severe(String.format(
                    "Subscription %s for device %s service %s ended with reason %s.",
                    subscription.getSubscriptionId(),
                    getDeviceId(),
                    getServiceId(),
                    reason.toString()
            ));
        }
    }

    @Override
    protected void eventReceived(GENASubscription subscription) {
        log.finer(String.format(
                "Subscription %s for device %s service %s received event.",
                subscription.getSubscriptionId(),
                getDeviceId(),
                getServiceId()
        ));
        Map<String, StateVariableValue> values = subscription.getCurrentValues();
        Dictionary dictionary = new Hashtable();

        for (String key : values.keySet()) {
            StateVariableValue variable = values.get(key);

            Object value = OSGiDataConverter.toOSGiValue(variable.getDatatype(), variable.getValue());

            if (value == null) {
                log.severe(String.format(
                        "Cannot convert variable %s to OSGi type %s.",
                        variable.getStateVariable().getName(),
                        variable.getDatatype().getDisplayString())
                );
                // TODO: throw an exception
            }

            dictionary.put(variable.getStateVariable().getName(), value);
        }

        listener.notifyUPnPEvent(
                getDeviceId(),
                getServiceId(),
                dictionary
        );
    }

    @Override
    protected void eventsMissed(GENASubscription subscription, int numberOfMissedEvents) {
        log.warning(String.format("Subscription %s for device %s service %s missed %d events.",
                                  subscription.getSubscriptionId(),
                                  getDeviceId(),
                                  getServiceId(),
                                  numberOfMissedEvents
        ));
    }


    @Override
    protected void invalidMessage(RemoteGENASubscription subscription,
                                  UnsupportedDataException ex) {
        log.finer(String.format(
            "Subscription %s for device %s service %s received invalid XML message causing exception %s.",
            subscription.getSubscriptionId(),
            getDeviceId(),
            getServiceId(),
            ex.toString()
        ));
    }
}
