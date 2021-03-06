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

package org.eclipse.virgo.kernel.userregion.internal.quasi;

import java.util.List;

import org.eclipse.osgi.service.resolver.BundleDescription;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Version;

import org.eclipse.virgo.kernel.osgi.quasi.QuasiBundle;
import org.eclipse.virgo.kernel.osgi.quasi.QuasiExportPackage;
import org.eclipse.virgo.kernel.osgi.quasi.QuasiImportPackage;
import org.eclipse.virgo.kernel.userregion.internal.quasi.StandardQuasiBundle;
import org.eclipse.virgo.kernel.userregion.internal.quasi.StandardQuasiExportPackage;

/**
 */
public class StandardQuasiExportPackageTests {

    private static final String BSN = "bsn";

    private static final String PN = "p";

    private static final Version VERSION = new Version("5.4.3");

    private StubBundleDescription bundleDescription;

    private StubExportPackageDescription exportPackage;

    private QuasiBundle qb;

    private StubStateHelper stateHelper;

    @Before
    public void setUp() {
        this.bundleDescription = new StubBundleDescription();
        this.bundleDescription.setBundleSymbolicName(BSN);
        this.stateHelper = new StubStateHelper();
        this.qb = new StandardQuasiBundle(this.bundleDescription, null, this.stateHelper);
        this.exportPackage = new StubExportPackageDescription(PN);
    }

    @Test
    public void testPackageName() {
        QuasiExportPackage qep = new StandardQuasiExportPackage(this.exportPackage, this.qb);
        Assert.assertEquals(PN, qep.getPackageName());
    }

    @Test
    public void testVersion() {
        this.exportPackage.setVersion(VERSION);
        QuasiExportPackage qep = new StandardQuasiExportPackage(this.exportPackage, this.qb);
        Assert.assertEquals(PN, qep.getPackageName());
    }

    @Test
    public void testExportingBundle() {
        QuasiExportPackage qep = new StandardQuasiExportPackage(this.exportPackage, this.qb);
        Assert.assertEquals(BSN, qep.getExportingBundle().getSymbolicName());
    }

    @Test
    public void testConsumers() {
        StubBundleDescription dependentBundle = new StubBundleDescription();
        StubImportPackageSpecification ips = new StubImportPackageSpecification(PN);
        dependentBundle.addImportPackage(ips);
        ips.setSupplier(this.exportPackage);
        this.stateHelper.setDependentBundles(new BundleDescription[] { dependentBundle });
        this.exportPackage.setExporter(this.bundleDescription);
        QuasiExportPackage qep = new StandardQuasiExportPackage(this.exportPackage, this.qb);
        List<QuasiImportPackage> consumers = qep.getConsumers();
        Assert.assertEquals(1, consumers.size());
        Assert.assertEquals(PN, consumers.get(0).getPackageName());
    }

}
