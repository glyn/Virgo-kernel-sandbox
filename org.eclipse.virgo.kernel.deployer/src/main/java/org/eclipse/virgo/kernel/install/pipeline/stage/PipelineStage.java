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

package org.eclipse.virgo.kernel.install.pipeline.stage;

import org.eclipse.virgo.kernel.osgi.framework.UnableToSatisfyBundleDependenciesException;

import org.eclipse.virgo.kernel.deployer.core.DeploymentException;
import org.eclipse.virgo.kernel.install.artifact.InstallArtifact;
import org.eclipse.virgo.kernel.install.environment.InstallEnvironment;
import org.eclipse.virgo.kernel.install.pipeline.Pipeline;
import org.eclipse.virgo.util.common.Tree;

/**
 * {@link PipelineStage} is a stage of a {@link Pipeline}.
 * <p />
 * 
 * <strong>Concurrent Semantics</strong><br />
 * 
 * Implementations of this interface must be thread safe.
 * 
 */
public interface PipelineStage {

    /**
     * Pass the given install tree through this pipeline stage.
     * 
     * @param installTree the tree to be processed
     * @param installEnvironment the processing environment common to all stages in a pipeline
     * @throws DeploymentException if a failure occurred
     * @throws UnableToSatisfyBundleDependenciesException if a bundle's dependencies cannot be satisfied
     */
    void process(Tree<InstallArtifact> installTree, InstallEnvironment installEnvironment) throws DeploymentException, UnableToSatisfyBundleDependenciesException;

}
