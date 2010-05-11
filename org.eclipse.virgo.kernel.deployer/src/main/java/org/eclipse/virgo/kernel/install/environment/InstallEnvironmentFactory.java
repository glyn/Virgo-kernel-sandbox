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

package org.eclipse.virgo.kernel.install.environment;

import org.eclipse.virgo.kernel.install.artifact.InstallArtifact;

/**
 * {@link InstallEnvironmentFactory} is used to create {@link InstallEnvironment InstallEnvironments}.
 * <p />
 * 
 * <strong>Concurrent Semantics</strong><br />
 * 
 * Implementations of this interface must be thread safe.
 * 
 */
public interface InstallEnvironmentFactory {

    /**
     * Returns a new {@link InstallEnvironment}.
     * 
     * @param installArtifact the root {@link InstallArtifact} being installed
     * @return an <code>InstallEnvironment</code>
     */
    InstallEnvironment createInstallEnvironment(InstallArtifact installArtifact);

}
