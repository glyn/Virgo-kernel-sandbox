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

package org.eclipse.virgo.kernel.install.artifact.internal;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.virgo.kernel.install.artifact.InstallArtifact;
import org.eclipse.virgo.kernel.install.artifact.PlanInstallArtifact;
import org.eclipse.virgo.util.common.Tree;
import org.eclipse.virgo.util.common.Tree.TreeVisitor;

/**
 * A simple helper class that can be used to collect all of the members of a plan. Collection is performed by visiting
 * the entire tree beneath the plan.
 * <p />
 * 
 * <strong>Concurrent Semantics</strong><br />
 * 
 * Thread-safe.
 * 
 */
final class PlanMemberCollector {

    /**
     * Collects all of the members of the given <code>plan</code>, including any nested plans and their members. Note that the
     * supplied <code>plan</code> will not be included in the returned <code>List</code>.
     * 
     * @param plan the plan for which the members are to be collected
     * @return all the members of the plan, not including the plan itself
     */
    List<InstallArtifact> collectPlanMembers(PlanInstallArtifact plan) {
        ArtifactCollectingTreeVisitor visitor = new ArtifactCollectingTreeVisitor(plan);
        plan.getTree().visit(visitor);
        return visitor.getMembers();
    }

    private static final class ArtifactCollectingTreeVisitor implements TreeVisitor<InstallArtifact> {

        private final InstallArtifact root;

        private final List<InstallArtifact> members = new ArrayList<InstallArtifact>();

        private final Object monitor = new Object();

        /**
         * @param root
         */
        public ArtifactCollectingTreeVisitor(InstallArtifact root) {
            this.root = root;
        }

        /**
         * {@inheritDoc}
         */
        public boolean visit(Tree<InstallArtifact> tree) {
            InstallArtifact artifact = tree.getValue();

            if (!root.equals(artifact)) {
                synchronized (this.monitor) {
                    this.members.add(artifact);
                }
            }

            return true;
        }

        List<InstallArtifact> getMembers() {
            synchronized (this.monitor) {
                return new ArrayList<InstallArtifact>(this.members);
            }
        }
    }
}
