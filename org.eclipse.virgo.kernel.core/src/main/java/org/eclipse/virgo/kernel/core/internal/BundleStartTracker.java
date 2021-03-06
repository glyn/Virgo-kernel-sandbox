/*******************************************************************************
 * Copyright (c) 2008, 2010 VMware Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   VMware Inc. - initial contribution
 *******************************************************************************/

package org.eclipse.virgo.kernel.core.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.virgo.kernel.core.BundleUtils;
import org.eclipse.virgo.kernel.core.Signal;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.framework.SynchronousBundleListener;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.core.task.TaskExecutor;
import org.springframework.osgi.extender.support.ApplicationContextConfiguration;


/**
 * <code>BundleStartTracker</code> tracks the startup of bundles, including any asynchronous portion of the startup,
 * notifying a {@link Signal} upon completion (successful or otherwise).
 * 
 * <p/>
 * 
 * <strong>Note</strong> if the synchronous portion of startup fails, i.e. {@link Bundle#start()} does not return
 * successfully the <code>Signal</code> is <strong>not</strong> driven and it is the responsibility of the caller of
 * <code>start</code> to handle the failure.
 * 
 * <p/>
 * 
 * <strong>Concurrent Semantics</strong><br />
 * 
 * Thread-safe.
 * 
 */
final class BundleStartTracker implements EventHandler {

    private static final String TOPIC_BLUEPRINT_EVENTS = "org/osgi/service/blueprint/container/";

    private static final String EVENT_REGION_STARTING = "org/eclipse/virgo/kernel/region/STARTING";

    private static final String EVENT_CREATED = TOPIC_BLUEPRINT_EVENTS + "CREATED";

    private static final String EVENT_FAILURE = TOPIC_BLUEPRINT_EVENTS + "FAILURE";

    private static final Logger LOGGER = LoggerFactory.getLogger(BundleStartTracker.class);

    private final Object monitor = new Object();

    private final List<Bundle> bundlesWithCreatedApplicationContexts = new ArrayList<Bundle>();

    private final Map<Bundle, Throwable> failureMap = new HashMap<Bundle, Throwable>();

    private final Map<Bundle, List<Signal>> signalMap = new HashMap<Bundle, List<Signal>>();

    private final BundleListener bundleListener = new StartupTrackerBundleListener();
    
    private final TaskExecutor signalExecutor;
    
    BundleStartTracker(TaskExecutor signalExecutor) {
        this.signalExecutor = signalExecutor;
    }

    void initialize(BundleContext bundleContext) {
        bundleContext.addBundleListener(this.bundleListener);
    }

    private void recordApplicationContextCreation(Bundle bundle) {
        LOGGER.info("Recording created application context for bundle '{}'", bundle);

        synchronized (this.monitor) {
            this.bundlesWithCreatedApplicationContexts.add(bundle);
        }
    }

    private void driveSignalsIfStartCompleted(Bundle bundle, boolean springDmPowered) {

        List<Signal> signals = null;
        Throwable failure = null;
        boolean isActive = isBundleActive(bundle);
        
        synchronized (this.monitor) {
            if (springDmPowered) {
                boolean created = this.bundlesWithCreatedApplicationContexts.contains(bundle);
                failure = this.failureMap.get(bundle);
                
                if (created && failure != null) {
                    throw new IllegalStateException("Spring DM has notified an application context both successfully constructed and failed: " + failure);
                }
                
                if (created) {
                    LOGGER.info("Bundle '{}' has started and its application context is available", bundle);
                    signals = this.signalMap.remove(bundle);
                } else if (failure != null) {
                    LOGGER.info("Bundle '{}' failed to start, the failure was '{}'", bundle, failure);
                    signals = this.signalMap.remove(bundle);
                }
            }
            else {
                if (isActive) {
                    signals = this.signalMap.remove(bundle);
                }
            }
        }
        // signals to drive
        if (signals != null) {
            if (!springDmPowered && isActive) {
                LOGGER.info("Non-Spring DM powered bundle '{}' has started. Driving signals '{}'.", bundle, signals);
                driveSignals(signals, null);
            }
            else {
                driveSignals(signals, failure);
            }
        }
    }

    private void driveSignals(final List<Signal> signals, final Throwable cause) {
        this.signalExecutor.execute(new Runnable() {
            public void run() {
                for (Signal signal : signals) {
                    LOGGER.info("Driving signal '{}'", signal);
                    if (cause == null) {
                        signal.signalSuccessfulCompletion();
                    } else {
                        signal.signalFailure(cause);
                    }
                }
            }
        });        
    }

    /**
     * {@inheritDoc}
     */
    public void handleEvent(Event event) {

        LOGGER.info("Handling event '{}'", event);

        Throwable cause = null;
        List<Signal> signals = null;

        Bundle bundle = (Bundle) event.getProperty("bundle");
        if (EVENT_FAILURE.equals(event.getTopic())) {
            cause = (Throwable) event.getProperty("exception");
            if (cause != null) {
                synchronized (this.monitor) {
                    LOGGER.error("Recording application context construction failure '{}' for bundle '{}'", cause, bundle);
                    this.failureMap.put(bundle, cause);
                    signals = this.signalMap.remove(bundle);
                }
            }
        } else if (EVENT_CREATED.equals(event.getTopic())) {
            synchronized (this.monitor) {
                recordApplicationContextCreation(bundle);
                signals = this.signalMap.remove(bundle);
            }
        } else if (EVENT_REGION_STARTING.equals(event.getTopic())) {
            initialize((BundleContext) event.getProperty("region.bundleContext"));
        }

        if (signals != null) {
            driveSignals(signals, cause);
        }
    }

    public void trackStart(Bundle bundle, Signal signal) {
        if (BundleUtils.isFragmentBundle(bundle)) {
            throw new IllegalArgumentException("Cannot track the start of a fragment bundle.");
        }

        boolean springDmPowered = isSpringDmPoweredBundle(bundle);

        boolean bundleActive = isBundleActive(bundle);
        
        if (signal != null) {
            if (springDmPowered || !bundleActive) {
                List<Signal> queue;
                synchronized (this.monitor) {
                    queue = this.signalMap.get(bundle);
                    if (queue == null) {
                        queue = new ArrayList<Signal>();
                        this.signalMap.put(bundle, queue);
                    }
                    LOGGER.info("Adding signal '{}' for bundle '{}'", signal, bundle);
                    queue.add(signal);
                }
            } else {
                // !springDmPowered && bundleActive
                driveSignals(Arrays.asList(signal), null);
            }
        }
        driveSignalsIfStartCompleted(bundle, springDmPowered);
    }

    private static boolean isBundleActive(Bundle bundle) {
        if (bundle!=null) {
            return ( bundle.getState() == Bundle.ACTIVE );
        }
        return false;
    }

    private static boolean isSpringDmPoweredBundle(Bundle bundle) {
        ApplicationContextConfiguration springDmConfiguration = new ApplicationContextConfiguration(bundle);
        boolean springDmPowered = springDmConfiguration.isSpringPoweredBundle();
        return springDmPowered;
    }

    private final class StartupTrackerBundleListener implements SynchronousBundleListener {

        /**
         * {@inheritDoc}
         */
        public void bundleChanged(BundleEvent event) {
            Bundle bundle = event.getBundle();
            if (event.getType() == BundleEvent.STARTED) {
                List<Signal> signals = null;
                if (!isSpringDmPoweredBundle(bundle)) {
                    synchronized (BundleStartTracker.this.monitor) {
                        signals = BundleStartTracker.this.signalMap.remove(bundle);
                    }
                    if (signals != null) {
                        LOGGER.info("Non-Spring DM powered bundle '{}' has started. Driving signals '{}'.", bundle, signals);
                        driveSignals(signals, null);
                    }
                }
            }
            if (event.getType() == BundleEvent.STOPPED) {
                LOGGER.info("Bundle '{}' has stopped. Removing its related tracking state.", bundle);
                BundleStartTracker.this.cleanup(bundle, new RuntimeException("bundle stopped"));
            }
        }
    }

    /**
     * Remove tracking state associated with this bundle
     * @param bundle whose tracking state is removed
     * @param cause reason for cleaning up
     */
    public void cleanup(Bundle bundle, Throwable cause) {
        List<Signal> danglingSignals = null;
        synchronized (BundleStartTracker.this.monitor) {
            if (bundle != null) {
                BundleStartTracker.this.bundlesWithCreatedApplicationContexts.remove(bundle);
                BundleStartTracker.this.failureMap.remove(bundle);
                danglingSignals = BundleStartTracker.this.signalMap.remove(bundle);
            }
        }
        if (danglingSignals != null) {
            driveSignals(danglingSignals, cause);
        }
    }

    /**
     * 
     */
    public void stop() {
        if (this.signalExecutor instanceof DisposableBean) {
            try {
                ((DisposableBean) this.signalExecutor).destroy();
            } catch (Exception e) {
                LOGGER.debug("Failure when attempting to destroy signal executor", e);
            }
        }
    }
}
