/**
 * Copyright (C) 2014 4th Line GmbH, Switzerland and others
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
 */

package org.jupnp.osgi.present;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.upnp.UPnPDevice;
import org.osgi.service.upnp.UPnPEventListener;
import org.osgi.service.upnp.UPnPService;
import org.jupnp.internal.compat.java.beans.PropertyChangeSupport;
import org.jupnp.model.meta.LocalDevice;
import org.jupnp.model.meta.LocalService;
import org.jupnp.osgi.Activator;

import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.logging.Logger;

/**
 * @author Bruce Green
 * @author Jochen Hiller - Changed to use Compact2 compliant Java Beans
 */
public class DataAdapter implements UPnPEventListener {

    final private static Logger log = Logger.getLogger(DataAdapter.class.getName());

    private PropertyChangeSupport propertyChangeSupport;

    public PropertyChangeSupport getPropertyChangeSupport() {
        return propertyChangeSupport;
    }

    public DataAdapter(LocalService<LocalDevice> service) {
        propertyChangeSupport = new PropertyChangeSupport(this);
        LocalDevice device = service.getDevice();
        String string = String.format(
                "(&(%s=%s)(%s=%s))",
                UPnPDevice.UDN, device.getIdentity().getUdn().getIdentifierString(),
                UPnPService.ID, service.getServiceId()
        );
        log.finer(String.format("filter: %s", string));

        try {
            BundleContext context = Activator.getPlugin().getContext();
            Filter filter = context.createFilter(string);

            Dictionary<String, Object> properties = new Hashtable<String, Object>();
            properties.put(UPnPEventListener.UPNP_FILTER, filter);
            context.registerService(UPnPEventListener.class.getName(), this, properties);
        } catch (InvalidSyntaxException e) {
            log.severe(String.format("Cannot create DataAdapter (%s).", service.getServiceId()));
            log.severe(e.getMessage());
        }
    }

    @Override
    public void notifyUPnPEvent(String deviceId, String serviceId, Dictionary events) {
        log.entering(this.getClass().getName(), "notifyUPnPEvent", new Object[]{deviceId, serviceId, events});

        for (String key : (List<String>) Collections.list(events.keys())) {
            Object value = events.get(key);
            propertyChangeSupport.firePropertyChange(key, null, value);
        }
    }
}
