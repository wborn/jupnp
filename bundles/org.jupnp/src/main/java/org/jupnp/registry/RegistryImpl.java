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

package org.jupnp.registry;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jupnp.UpnpService;
import org.jupnp.UpnpServiceConfiguration;
import org.jupnp.model.DiscoveryOptions;
import org.jupnp.model.ExpirationDetails;
import org.jupnp.model.ServiceReference;
import org.jupnp.model.gena.LocalGENASubscription;
import org.jupnp.model.gena.RemoteGENASubscription;
import org.jupnp.model.meta.Device;
import org.jupnp.model.meta.LocalDevice;
import org.jupnp.model.meta.RemoteDevice;
import org.jupnp.model.meta.RemoteDeviceIdentity;
import org.jupnp.model.meta.Service;
import org.jupnp.model.resource.Resource;
import org.jupnp.model.types.DeviceType;
import org.jupnp.model.types.ServiceType;
import org.jupnp.model.types.UDN;
import org.jupnp.protocol.ProtocolFactory;

/**
 * Default implementation of {@link Registry}.
 *
 * @author Christian Bauer
 */
public class RegistryImpl implements Registry {

    private Logger log = Logger.getLogger(Registry.class.getName());

    protected UpnpService upnpService;
    protected RegistryMaintainer registryMaintainer;
    protected final Set<RemoteGENASubscription> pendingSubscriptionsLock = new HashSet();
    protected Object lock = new Object();

    public RegistryImpl() {
    }

    /**
     * Starts background maintenance immediately.
     */
    public RegistryImpl(UpnpService upnpService) {
        log.fine("Creating Registry: " + getClass().getName());

        this.upnpService = upnpService;

        log.fine("Starting registry background maintenance...");
        registryMaintainer = createRegistryMaintainer();
        if (registryMaintainer != null) {
            getConfiguration().getRegistryMaintainerExecutor().execute(registryMaintainer);
        }
    }

    public UpnpService getUpnpService() {
        return upnpService;
    }

    public UpnpServiceConfiguration getConfiguration() {
        return getUpnpService().getConfiguration();
    }

    public ProtocolFactory getProtocolFactory() {
        return getUpnpService().getProtocolFactory();
    }

    protected RegistryMaintainer createRegistryMaintainer() {
        return new RegistryMaintainer(
                this,
                getConfiguration().getRegistryMaintenanceIntervalMillis()
        );
    }

    // #################################################################################################

    protected final Set<RegistryListener> registryListeners = new HashSet();
    protected final Set<RegistryItem<URI, Resource>> resourceItems = new HashSet();
    protected final List<Runnable> pendingExecutions = new ArrayList();

    protected final RemoteItems remoteItems = new RemoteItems(this);
    protected final LocalItems localItems = new LocalItems(this);

    // #################################################################################################

    public void addListener(RegistryListener listener) {
        synchronized(registryListeners) {
            registryListeners.add(listener);
        }
    }

    public void removeListener(RegistryListener listener) {
        synchronized(registryListeners) {
            registryListeners.remove(listener);
        }
    }

    public Collection<RegistryListener> getListeners() {
        synchronized(registryListeners) {
            return Collections.unmodifiableCollection(registryListeners);
        }
    }

    public boolean notifyDiscoveryStart(final RemoteDevice device) {
        // Exit if we have it already, this is atomic inside this method, finally
        if (getUpnpService().getRegistry().getRemoteDevice(device.getIdentity().getUdn(), true) != null) {
            log.finer("Not notifying listeners, already registered: " + device);
            return false;
        }
        
        for (final RegistryListener listener : getListeners()) {
            getConfiguration().getRegistryListenerExecutor().execute(
                    new Runnable() {
                        public void run() {
                            listener.remoteDeviceDiscoveryStarted(RegistryImpl.this, device);
                        }
                    }
            );
        }
        
        return true;
    }

    public void notifyDiscoveryFailure(final RemoteDevice device, final Exception ex) {
        for (final RegistryListener listener : getListeners()) {
            getConfiguration().getRegistryListenerExecutor().execute(
                    new Runnable() {
                        public void run() {
                            listener.remoteDeviceDiscoveryFailed(RegistryImpl.this, device, ex);
                        }
                    }
            );
        }
    }

    // #################################################################################################

    public void addDevice(LocalDevice localDevice) {
        synchronized(localItems) {
            localItems.add(localDevice);
        }
    }

    public void addDevice(LocalDevice localDevice, DiscoveryOptions options) {
        synchronized(localItems) {
            localItems.add(localDevice, options);
        }
    }

    public void setDiscoveryOptions(UDN udn, DiscoveryOptions options) {
        synchronized(localItems) {
            localItems.setDiscoveryOptions(udn, options);
        }
    }

    public DiscoveryOptions getDiscoveryOptions(UDN udn) {
        synchronized(localItems) {
            return localItems.getDiscoveryOptions(udn);
        }
    }

    public void addDevice(RemoteDevice remoteDevice) {
        synchronized(remoteItems) {
            remoteItems.add(remoteDevice);
        }
    }

    public boolean update(RemoteDeviceIdentity rdIdentity) {
        synchronized(remoteItems) {
            return remoteItems.update(rdIdentity);
        }
    }

    public boolean removeDevice(LocalDevice localDevice) {
        synchronized(localItems) {
            return localItems.remove(localDevice);
        }
    }

    public boolean removeDevice(RemoteDevice remoteDevice) {
        synchronized(remoteItems) {
            return remoteItems.remove(remoteDevice);
        }
    }

    public void removeAllLocalDevices() {
        synchronized(localItems) {
            localItems.removeAll();
        }
    }

    public void removeAllRemoteDevices() {
        synchronized(remoteItems) {
            remoteItems.removeAll();
        }
    }

    public boolean removeDevice(UDN udn) {
        Device device = getDevice(udn, true);
        if (device != null && device instanceof LocalDevice)
            return removeDevice((LocalDevice) device);
        if (device != null && device instanceof RemoteDevice)
            return removeDevice((RemoteDevice) device);
        return false;
    }

    public Device getDevice(UDN udn, boolean rootOnly) {
        Device device;
        synchronized(localItems) {
            if ((device = localItems.get(udn, rootOnly)) != null) return device;
        }
        synchronized(remoteItems) {
            if ((device = remoteItems.get(udn, rootOnly)) != null) return device;
        }
        
        return null;
    }

    public LocalDevice getLocalDevice(UDN udn, boolean rootOnly) {
        synchronized(localItems) {
            return localItems.get(udn, rootOnly);
        }
    }

    public RemoteDevice getRemoteDevice(UDN udn, boolean rootOnly) {
        synchronized(remoteItems) {
            return remoteItems.get(udn, rootOnly);
        }
    }

    public Collection<LocalDevice> getLocalDevices() {
        synchronized(localItems) {
            return Collections.unmodifiableCollection(localItems.get());
        }
    }

    public Collection<RemoteDevice> getRemoteDevices() {
        synchronized(remoteItems) {
            return Collections.unmodifiableCollection(remoteItems.get());
        }
    }

    public Collection<Device> getDevices() {
        Set all = new HashSet();
        synchronized(localItems) {
            all.addAll(localItems.get());
        }
        
        synchronized(remoteItems) {
            all.addAll(remoteItems.get());
        }
        
        return Collections.unmodifiableCollection(all);
    }

    public Collection<Device> getDevices(DeviceType deviceType) {
        Collection<Device> devices = new HashSet();

        synchronized(localItems) {
            devices.addAll(localItems.get(deviceType));
        }
        
        synchronized(remoteItems) {
            devices.addAll(remoteItems.get(deviceType));
        }

        return Collections.unmodifiableCollection(devices);
    }

    public Collection<Device> getDevices(ServiceType serviceType) {
        Collection<Device> devices = new HashSet();

        synchronized(localItems) {
            devices.addAll(localItems.get(serviceType));
        }
        
        synchronized(remoteItems) {
            devices.addAll(remoteItems.get(serviceType));
        }

        return Collections.unmodifiableCollection(devices);
    }

    public Service getService(ServiceReference serviceReference) {
        Device device;
        if ((device = getDevice(serviceReference.getUdn(), false)) != null) {
            return device.findService(serviceReference.getServiceId());
        }
        return null;
    }

    // #################################################################################################

    public Resource getResource(URI pathQuery) throws IllegalArgumentException {
        if (pathQuery.isAbsolute()) {
            throw new IllegalArgumentException("Resource URI can not be absolute, only path and query:" + pathQuery);
        }

        // Note: Uses field access on resourceItems for performance reasons

        synchronized(resourceItems) {
    		for (RegistryItem<URI, Resource> resourceItem : resourceItems) {
            	Resource resource = resourceItem.getItem();
            	if (resource.matches(pathQuery)) {
                    return resource;
                }
            }
        }

        // TODO: UPNP VIOLATION: Fuppes on my ReadyNAS thinks it's a cool idea to add a slash at the end of the callback URI...
        // It also cuts off any query parameters in the callback URL - nice!
        synchronized(resourceItems) {
            if (pathQuery.getPath().endsWith("/")) {
                URI pathQueryWithoutSlash = URI.create(pathQuery.toString().substring(0, pathQuery.toString().length() - 1));
    
     			for (RegistryItem<URI, Resource> resourceItem : resourceItems) {
                	Resource resource = resourceItem.getItem();
                	if (resource.matches(pathQueryWithoutSlash)) {
                        return resource;
                    }
                }
            }
        }

        return null;
    }

    public <T extends Resource> T getResource(Class<T> resourceType, URI pathQuery) throws IllegalArgumentException {
        Resource resource = getResource(pathQuery);
        if (resource != null && resourceType.isAssignableFrom(resource.getClass())) {
            return (T) resource;
        }
        return null;
    }

    public Collection<Resource> getResources() {
        Collection<Resource> s = new HashSet();
        synchronized(resourceItems) {
            for (RegistryItem<URI, Resource> resourceItem : resourceItems) {
                s.add(resourceItem.getItem());
            }
        }
        return s;
    }

    public <T extends Resource> Collection<T> getResources(Class<T> resourceType) {
        Collection<T> s = new HashSet();
        synchronized(resourceItems) {
            for (RegistryItem<URI, Resource> resourceItem : resourceItems) {
                if (resourceType.isAssignableFrom(resourceItem.getItem().getClass()))
                    s.add((T) resourceItem.getItem());
            }
        }
        return s;
    }

    public void addResource(Resource resource) {
        addResource(resource, ExpirationDetails.UNLIMITED_AGE);
    }

    public void addResource(Resource resource, int maxAgeSeconds) {
        RegistryItem resourceItem = new RegistryItem(resource.getPathQuery(), resource, maxAgeSeconds);
        synchronized(resourceItems) {
            resourceItems.remove(resourceItem);
            resourceItems.add(resourceItem);
        }
    }

    public boolean removeResource(Resource resource) {
        synchronized(resourceItems) {
            return resourceItems.remove(new RegistryItem(resource.getPathQuery()));
        }
    }

    // #################################################################################################

    public void addLocalSubscription(LocalGENASubscription subscription) {
        synchronized(localItems) {
            localItems.addSubscription(subscription);
        }
    }

    public LocalGENASubscription getLocalSubscription(String subscriptionId) {
        synchronized(localItems) {
            return localItems.getSubscription(subscriptionId);
        }
    }

    public boolean updateLocalSubscription(LocalGENASubscription subscription) {
        synchronized(localItems) {
            return localItems.updateSubscription(subscription);
        }
    }

    public boolean removeLocalSubscription(LocalGENASubscription subscription) {
        synchronized(localItems) {
            return localItems.removeSubscription(subscription);
        }
    }

    public void addRemoteSubscription(RemoteGENASubscription subscription) {
        synchronized(remoteItems) {
            remoteItems.addSubscription(subscription);
        }
    }

    public RemoteGENASubscription getRemoteSubscription(String subscriptionId) {
        synchronized(remoteItems) {
            return remoteItems.getSubscription(subscriptionId);
        }
    }

    public void updateRemoteSubscription(RemoteGENASubscription subscription) {
        synchronized(remoteItems) {
            remoteItems.updateSubscription(subscription);
        }
    }

    public void removeRemoteSubscription(RemoteGENASubscription subscription) {
        synchronized(remoteItems) {
            remoteItems.removeSubscription(subscription);
        }
    }

    /* ############################################################################################################ */

   	public void advertiseLocalDevices() {
       	 synchronized(localItems) {
       		localItems.advertiseLocalDevices();
       	 }
   	}

    /* ############################################################################################################ */

    // When you call this, make sure you have the Router lock before this lock is obtained!
    public void shutdown() {
        log.fine("Shutting down registry...");

        synchronized(lock) {
            if (registryMaintainer != null)
                registryMaintainer.stop();
        }
        
        // Final cleanup run to flush out pending executions which might
        // not have been caught by the maintainer before it stopped
        synchronized(pendingExecutions) {
            log.finest("Executing final pending operations on shutdown: " + pendingExecutions.size());
            runPendingExecutions(false);
        }

        synchronized(registryListeners) {
            for (RegistryListener listener : registryListeners) {
                listener.beforeShutdown(this);
            }
        }

        synchronized(resourceItems) {
            RegistryItem<URI, Resource>[] resources = resourceItems.toArray(new RegistryItem[resourceItems.size()]);
            for (RegistryItem<URI, Resource> resourceItem : resources) {
                resourceItem.getItem().shutdown();
            }
        }

        
        synchronized(remoteItems) {
            remoteItems.shutdown();
        }
        
        synchronized(localItems) {
            localItems.shutdown();
        }

        synchronized(registryListeners) {
            for (RegistryListener listener : registryListeners) {
                listener.afterShutdown();
            }
        }
    }

    public void pause() {
        synchronized(lock) {
            if (registryMaintainer != null) {
                log.fine("Pausing registry maintenance");
                runPendingExecutions(true);
                registryMaintainer.stop();
                registryMaintainer = null;
            }
        }
    }

    public void resume() {
        synchronized(lock) {
            if (registryMaintainer == null) {
                log.fine("Resuming registry maintenance");
                synchronized(remoteItems) {
                    remoteItems.resume();
                }
                
                registryMaintainer = createRegistryMaintainer();
                if (registryMaintainer != null) {
                    getConfiguration().getRegistryMaintainerExecutor().execute(registryMaintainer);
                }
            }
        }
    }

    public boolean isPaused() {
        synchronized(lock) {
            return registryMaintainer == null;
        }
    }

    /* ############################################################################################################ */

    void maintain() {

        if (log.isLoggable(Level.FINEST))
            log.finest("Maintaining registry...");

        // Remove expired resources
        synchronized (resourceItems) {
            Iterator<RegistryItem<URI, Resource>> it = resourceItems.iterator();
            while (it.hasNext()) {
                RegistryItem<URI, Resource> item = it.next();
                if (item.getExpirationDetails().hasExpired()) {
                    if (log.isLoggable(Level.FINER))
                        log.finer("Removing expired resource: " + item);
                    it.remove();
                }
            }
            
            // Let each resource do its own maintenance
            synchronized(pendingExecutions) {
                for (RegistryItem<URI, Resource> resourceItem : resourceItems) {
                    resourceItem.getItem().maintain(
                            pendingExecutions,
                            resourceItem.getExpirationDetails()
                    );
                }
            }
        }




        // These add all their operations to the pendingExecutions queue
        synchronized(remoteItems) {
            remoteItems.maintain();
        }
        
        synchronized (localItems) {
            localItems.maintain();
        }

        // We now run the queue asynchronously so the maintenance thread can continue its loop undisturbed
        runPendingExecutions(true);
    }

    void executeAsyncProtocol(Runnable runnable) {
        synchronized (pendingExecutions) {
            pendingExecutions.add(runnable);
        }
    }

    void runPendingExecutions(boolean async) {
        synchronized (pendingExecutions) {
            if (log.isLoggable(Level.FINEST))
                log.finest("Executing pending operations: " + pendingExecutions.size());
            for (Runnable pendingExecution : pendingExecutions) {
                if (async)
                    getConfiguration().getAsyncProtocolExecutor().execute(pendingExecution);
                else
                    pendingExecution.run();
            }
            if (pendingExecutions.size() > 0) {
                pendingExecutions.clear();
            }
        }
    }

    /* ############################################################################################################ */

    public void printDebugLog() {
        if (log.isLoggable(Level.FINE)) {
            log.fine("====================================    REMOTE   ================================================");

            synchronized(remoteItems) {
                for (RemoteDevice remoteDevice : remoteItems.get()) {
                    log.fine(remoteDevice.toString());
                }
            }

            log.fine("====================================    LOCAL    ================================================");

            synchronized(localItems) {
                for (LocalDevice localDevice : localItems.get()) {
                    log.fine(localDevice.toString());
                }
            }

            log.fine("====================================  RESOURCES  ================================================");

            synchronized(resourceItems) {
                for (RegistryItem<URI, Resource> resourceItem : resourceItems) {
                    log.fine(resourceItem.toString());
                }
            }

            log.fine("=================================================================================================");

        }

    }
    
 	@Override
	public void registerPendingRemoteSubscription(RemoteGENASubscription subscription) {
		synchronized (pendingSubscriptionsLock) {
            pendingSubscriptionsLock.add(subscription);
        }
	}
	
	@Override
	public void unregisterPendingRemoteSubscription(RemoteGENASubscription subscription) {
        synchronized (pendingSubscriptionsLock) {
            if(pendingSubscriptionsLock.remove(subscription)) {
                pendingSubscriptionsLock.notifyAll();
            }
        }
	}

    @Override
    public RemoteGENASubscription getWaitRemoteSubscription(String subscriptionId) {
        synchronized (pendingSubscriptionsLock) {
            do {
                RemoteGENASubscription subscription = getRemoteSubscription(subscriptionId);
                if (subscription != null) {
                    return subscription;
                }
                if (!pendingSubscriptionsLock.isEmpty()) {
                    try {
                        log.finest("Subscription not found, waiting for pending subscription procedure to terminate." );
                        pendingSubscriptionsLock.wait();
                    } catch (InterruptedException e) {
                    }
                }
            } while (!pendingSubscriptionsLock.isEmpty());
        }
        return null;
    }

}
