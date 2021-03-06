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

package org.eclipse.virgo.kernel.deployer.hot;

import java.io.File;

import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import org.eclipse.virgo.kernel.deployer.core.ApplicationDeployer;
import org.eclipse.virgo.kernel.deployer.core.DeployerConfiguration;
import org.eclipse.virgo.kernel.serviceability.NonNull;
import org.eclipse.virgo.medic.eventlog.EventLogger;
import org.eclipse.virgo.util.io.FileSystemChecker;
import org.eclipse.virgo.util.io.PathReference;

/**
 * Handles hot deployment of application artefacts.
 * <p/>
 * 
 * <strong>Concurrent Semantics</strong><br />
 * 
 * Threadsafe.
 * 
 */
public final class HotDeployer implements EventHandler {

    private static final String EXCLUDE_PATTERN = "\\.DS_Store";

    private static final String TOPIC_RECOVERY_COMPLETED = "org/eclipse/virgo/kernel/deployer/recovery/COMPLETED";

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    private final Object lifecycleLock = new Object();

    private final File pickupDir;
    
    private final Thread thread;

    /**
     * Creates a new <code>HotDeployer</code>.
     * 
     * @param deployerConfiguration the {@link DeployerConfiguration} parameters.
     * @param deployer the {@link ApplicationDeployer} to deploy to.
     * @param eventLogger where to log events
     */
    public HotDeployer(@NonNull DeployerConfiguration deployerConfiguration, @NonNull ApplicationDeployer deployer,
         EventLogger eventLogger) {
        this.pickupDir = createHotDeployDir(deployerConfiguration.getDeploymentPickupDirectory());
        FileSystemChecker checker = createFileSystemChecker(deployer, eventLogger);
        this.thread = new Thread(new WatchTask(checker, this.pickupDir), "fs-watcher");
    }

	private FileSystemChecker createFileSystemChecker(
			ApplicationDeployer deployer, EventLogger eventLogger) {
		FileSystemChecker checker = new FileSystemChecker(this.pickupDir, EXCLUDE_PATTERN, this.logger);
        checker.addListener(new HotDeploymentFileSystemListener(deployer, eventLogger));
		return checker;
	}

    /**
     * Creates the hot deployment directory.
     * 
     * @param pickUpDirectoryPath the {@link PathReference} location of the pickup directory.
     * @return the {@link File} of the hot deployment directory.
     */
    private File createHotDeployDir(@NonNull PathReference pickUpDirectoryPath) {
        if (pickUpDirectoryPath.isFile()) {
            logger.debug("Deleting stray file from hot deployment directory location '{}'.", pickUpDirectoryPath.getAbsolutePath());
            pickUpDirectoryPath.delete();
        }
        if (!pickUpDirectoryPath.exists()) {
            logger.info("Creating hot deployment directory at '{}'.", pickUpDirectoryPath.getAbsolutePath());
            pickUpDirectoryPath.createDirectory();
        } else {
            logger.info("Using hot deployment directory at '{}'.", pickUpDirectoryPath.getAbsolutePath());
        }
        return pickUpDirectoryPath.toFile();
    }

    /**
     * Start the <code>FileSystemWatcher</code>.
     */
    private void doStart() {
        synchronized (this.lifecycleLock) {
            if (this.thread != null) {
                this.thread.start();
                logger.info("Started hot deployer on '{}'.", this.pickupDir);
            }
        }
    }

    /**
     * Stop the <code>FileSystemWatcher</code>,
     */
    public void stop() {
        synchronized (this.lifecycleLock) {
            if (this.thread != null) {
                logger.info("Stopping hot deployer");
                this.thread.interrupt();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return String.format("Hot Deployer [pickupDir = %s]", this.pickupDir.getAbsolutePath());
    }

    /** 
     * {@inheritDoc}
     */
    public void handleEvent(Event event) {
        if (TOPIC_RECOVERY_COMPLETED.equals(event.getTopic())) {
            doStart();
        }    
    }
}
