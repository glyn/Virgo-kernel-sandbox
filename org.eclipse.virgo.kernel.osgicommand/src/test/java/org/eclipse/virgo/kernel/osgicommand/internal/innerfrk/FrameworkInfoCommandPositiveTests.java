/*******************************************************************************
 * Copyright (c) 2010 SAP AG
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Borislav Kapukaranov, SAP AG - initial contribution
 ******************************************************************************/

package org.eclipse.virgo.kernel.osgicommand.internal.innerfrk;

import org.eclipse.virgo.kernel.osgicommand.frameworkdetection.FrameworkInfoCommand;
import org.eclipse.virgo.kernel.frameworkdetection.lib.FrameworkCollector;
import org.eclipse.virgo.kernel.frameworkdetection.lib.FrameworkData;
import junit.framework.Assert;
import org.junit.Test;
import org.junit.After;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Version;

import static org.easymock.EasyMock.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Class providing positive tests for frk shell command
 *
 * @author Borislav Kapukaranov (borislav.kapukaranov@sap.com)
 * @version 1.0
 */
public class FrameworkInfoCommandPositiveTests {

    protected static final String FILE_SEPARATOR = System.getProperty("file.separator");
    protected static final String NEW_LINE       = System.getProperty("line.separator");

    protected static final String              FRAMEWORK_STATES[] = {"ACTIVE", "INSTALLED", "RESOLVED", "UNINSTALLED", "STARTING", "STOPPING"};
    protected static final String              KEYS[]             = {"11", "21", "31", "41", "51", "61"};
    protected static final Exception           e                  = new Exception();
    protected static final StackTraceElement[] ORIGIN             = e.getStackTrace();

    protected long getFirstFrameworkId() {
        long id = 1;
        ConcurrentHashMap<String, FrameworkData> frkd = FrameworkCollector.getFrameworks();
        for (FrameworkData d : frkd.values()) {
            long tmpId = d.getID();
            if (id < tmpId) {
                id = tmpId;
            }
        }
        return id;
    }

    private static final String   BUNDLE_SYMBOLIC_NAME = "test";
    private static final String   BUNDLE_LOCATION      = "file:D:" + FILE_SEPARATOR + "test.jar";
    private static final String[] OSGI_PROPERTIES      = {"org.osgi.framework.system.packages",
                                                          "org.osgi.framework.bootdelegation", "osgi.parentClassloader",
                                                          "osgi.install.area", "osgi.configuration.area",
                                                          "osgi.sharedConfiguration.area", "osgi.instance.area",
                                                          "osgi.instance.area.default", "osgi.user.area",
                                                          "osgi.user.area.default", "osgi.manifest.cache", "user.home",
                                                          "user.dir", "osgi.noShutdown", "osgi.compatibility.bootdelegation",
                                                          "org.osgi.framework.vendor", "osgi.bundlefile.limit",
                                                          "osgi.logfile", "osgi.framework.extensions",
                                                          "osgi.frameworkClassPath"};

    @Test
    public void test_frkListBundlesCommand() {
        FrameworkInfoCommand cmd = new FrameworkInfoCommand();
        Bundle bundle = createMock(Bundle.class);
        BundleContext bundleContext = createMock(BundleContext.class);
        expect(bundle.getBundleContext()).andReturn(bundleContext);
        expect(bundleContext.getBundles()).andReturn(new Bundle[]{bundle});
        FrameworkCollector.addFramework(FRAMEWORK_STATES[1], KEYS[0], bundle, ORIGIN);

        long id = getFirstFrameworkId();
        expect(bundle.getBundleId()).andReturn(id);
        expect(bundle.getState()).andReturn(Bundle.RESOLVED);
        expect(bundle.getSymbolicName()).andReturn(BUNDLE_SYMBOLIC_NAME);
        expect(bundle.getVersion()).andReturn(Version.emptyVersion);
        replay(bundle, bundleContext);
        String bndIdToString = "" + id;
        String frkIdToString = "" + id;
        StubCommandInterpreter cmdInterpreter = new StubCommandInterpreter(frkIdToString, "ss", bndIdToString);

        cmd._frk(cmdInterpreter);
        StringBuffer commandResult = cmdInterpreter.getCommandOutput();
        Assert.assertTrue(commandResult.toString().contains("Listing bundles in framework"));
        Assert.assertTrue(commandResult.toString().contains(id + ""));
        Assert.assertTrue(commandResult.toString().contains("RESOLVED"));
        Assert.assertTrue(commandResult.toString().contains("test_0.0.0"));
        verify(bundle, bundleContext);
    }

    @Test
    public void test_frkStop1Bundle() throws BundleException {
        FrameworkInfoCommand cmd = new FrameworkInfoCommand();
        Bundle frkBundle = createMock(Bundle.class);
        Bundle bundle = createMock(Bundle.class);
        BundleContext frkContext = createMock(BundleContext.class);
        BundleContext bundleContext = createMock(BundleContext.class);
        expect(frkBundle.getBundleContext()).andReturn(frkContext);
        FrameworkCollector.addFramework(FRAMEWORK_STATES[0], KEYS[1], frkBundle, ORIGIN);
        FrameworkCollector.addFramework(FRAMEWORK_STATES[2], KEYS[2], bundle, ORIGIN);

        long id = getFirstFrameworkId();
        String bndIdToString = "" + id;
        String frkIdToString = "" + (id - 1);
        StubCommandInterpreter cmdInterpreter = new StubCommandInterpreter(frkIdToString, "stop", bndIdToString);

        expect(frkContext.getBundle(id)).andReturn(bundle);
        bundle.stop();
        expect(bundle.getState()).andReturn(Bundle.STOPPING).times(2);
        expect(bundle.getSymbolicName()).andReturn(BUNDLE_SYMBOLIC_NAME);
        expect(bundle.getVersion()).andReturn(Version.emptyVersion);
        replay(frkBundle, bundle, frkContext, bundleContext);

        cmd._frk(cmdInterpreter);
        StringBuffer commandResult = cmdInterpreter.getCommandOutput();
        Assert.assertTrue(commandResult.toString().contains("Bundle [" + BUNDLE_SYMBOLIC_NAME + "_" + Version.emptyVersion + "] stopped successfully in framework ["));
        verify(bundle, bundleContext);
    }

    @Test
    public void test_frkStop2Bundle() throws BundleException {
        FrameworkInfoCommand cmd = new FrameworkInfoCommand();
        Bundle frkBundle = createMock(Bundle.class);
        Bundle bundle = createMock(Bundle.class);
        BundleContext frkContext = createMock(BundleContext.class);
        BundleContext bundleContext = createMock(BundleContext.class);
        expect(frkBundle.getBundleContext()).andReturn(frkContext);
        FrameworkCollector.addFramework(FRAMEWORK_STATES[2], KEYS[1],
                                        frkBundle, ORIGIN);
        FrameworkCollector.addFramework(FRAMEWORK_STATES[2], KEYS[2], bundle,
                                        ORIGIN);
        long id = getFirstFrameworkId();
        String bndIdToString = "" + id;
        String frkIdToString = "" + (id - 1);
        StubCommandInterpreter cmdInterpreter = new StubCommandInterpreter(frkIdToString, "stop", bndIdToString);

        expect(frkContext.getBundle(id)).andReturn(bundle);
        expect(bundle.getState()).andReturn(Bundle.RESOLVED).times(1);
        expect(bundle.getSymbolicName()).andReturn(BUNDLE_SYMBOLIC_NAME);
        expect(bundle.getVersion()).andReturn(Version.emptyVersion);
        replay(frkBundle, bundle, frkContext, bundleContext);

        cmd._frk(cmdInterpreter);
        StringBuffer commandResult = cmdInterpreter.getCommandOutput();
        Assert.assertTrue(commandResult.toString().contains("Bundle [" + BUNDLE_SYMBOLIC_NAME + "_" + Version.emptyVersion + "] already stopped in framework ["));
        verify(bundle, bundleContext);
    }

    @Test
    public void test_frkInstallBundle() throws BundleException {
        FrameworkInfoCommand cmd = new FrameworkInfoCommand();
        Bundle frkBundle = createMock(Bundle.class);
        BundleContext frkContext = createMock(BundleContext.class);
        Bundle bundle = createMock(Bundle.class);
        BundleContext bundleContext = createMock(BundleContext.class);
        expect(bundle.getBundleContext()).andReturn(bundleContext);
        expect(bundleContext.installBundle(BUNDLE_LOCATION)).andReturn(bundle);
        expect(bundle.getSymbolicName()).andReturn(BUNDLE_SYMBOLIC_NAME);
        expect(bundle.getVersion()).andReturn(Version.emptyVersion);
        FrameworkCollector.addFramework(FRAMEWORK_STATES[1], KEYS[2], bundle, ORIGIN);
        FrameworkCollector.addFramework(FRAMEWORK_STATES[1], KEYS[3], frkBundle, ORIGIN);

        long id = getFirstFrameworkId();
        expect(bundle.getBundleId()).andReturn(id);
        replay(bundle, bundleContext, frkBundle, frkContext);
        String frkIdToString = "" + (id - 1);
        StubCommandInterpreter cmdInterpreter = new StubCommandInterpreter(frkIdToString, "install", BUNDLE_LOCATION);

        cmd._frk(cmdInterpreter);
        StringBuffer commandResult = cmdInterpreter.getCommandOutput();
        Assert.assertTrue(commandResult.toString().contains(BUNDLE_SYMBOLIC_NAME));
        Assert.assertTrue(commandResult.toString().contains(BUNDLE_SYMBOLIC_NAME + "_" + Version.emptyVersion));
        Assert.assertTrue(commandResult.toString().contains("" + id));
        verify(bundle, bundleContext, frkBundle, frkContext);
    }

    @Test
    public void test_frkUninstallBundle() throws BundleException {
        FrameworkInfoCommand cmd = new FrameworkInfoCommand();
        Bundle frkBundle = createMock(Bundle.class);
        Bundle bundle = createMock(Bundle.class);
        BundleContext frkContext = createMock(BundleContext.class);
        BundleContext bundleContext = createMock(BundleContext.class);
        expect(frkBundle.getBundleContext()).andReturn(frkContext);
        FrameworkCollector.addFramework(FRAMEWORK_STATES[0], KEYS[4], frkBundle, ORIGIN);
        FrameworkCollector.addFramework(FRAMEWORK_STATES[2], KEYS[5], bundle, ORIGIN);
        long id = getFirstFrameworkId();
        String bndIdToString = "" + id;
        String frkIdToString = "" + (id - 1);
        StubCommandInterpreter cmdInterpreter = new StubCommandInterpreter(frkIdToString, "uninstall", bndIdToString);

        expect(frkContext.getBundle(id)).andReturn(bundle);
        bundle.uninstall();
        expect(bundle.getSymbolicName()).andReturn(BUNDLE_SYMBOLIC_NAME);
        expect(bundle.getVersion()).andReturn(Version.emptyVersion);
        replay(frkBundle, bundle, frkContext, bundleContext);

        cmd._frk(cmdInterpreter);
        StringBuffer commandResult = cmdInterpreter.getCommandOutput();
        Assert.assertTrue(commandResult.toString().contains("Bundle [" + BUNDLE_SYMBOLIC_NAME + "_" + Version.emptyVersion + "] uninstalled successfully in framework"));
        verify(bundle, bundleContext, frkBundle, frkContext);
    }


    @Test
    public void test_frkUpdateBundle() throws BundleException {
        FrameworkInfoCommand cmd = new FrameworkInfoCommand();
        Bundle frkBundle = createMock(Bundle.class);
        Bundle bundle = createMock(Bundle.class);
        BundleContext frkContext = createMock(BundleContext.class);
        BundleContext bundleContext = createMock(BundleContext.class);
        expect(frkBundle.getBundleContext()).andReturn(frkContext);
        FrameworkCollector.addFramework(FRAMEWORK_STATES[0], KEYS[4], frkBundle, ORIGIN);
        FrameworkCollector.addFramework(FRAMEWORK_STATES[2], KEYS[5], bundle, ORIGIN);

        long id = getFirstFrameworkId();
        String bndIdToString = "" + id;
        String frkIdToString = "" + (id - 1);
        StubCommandInterpreter cmdInterpreter = new StubCommandInterpreter(frkIdToString, "update", bndIdToString);

        expect(frkContext.getBundle(id)).andReturn(bundle);
        bundle.update();
        expect(bundle.getSymbolicName()).andReturn(BUNDLE_SYMBOLIC_NAME);
        expect(bundle.getVersion()).andReturn(Version.emptyVersion);
        replay(frkBundle, bundle, frkContext, bundleContext);

        cmd._frk(cmdInterpreter);
        StringBuffer commandResult = cmdInterpreter.getCommandOutput();
        Assert.assertTrue(commandResult.toString().contains("Bundle [" + BUNDLE_SYMBOLIC_NAME + "_" + Version.emptyVersion + "] updated successfully in framework"));
        verify(bundle, bundleContext);
    }

    @Test
    public void test_frkStart1Bundle() throws BundleException {
        FrameworkInfoCommand cmd = new FrameworkInfoCommand();
        Bundle frkBundle = createMock(Bundle.class);
        Bundle bundle = createMock(Bundle.class);
        BundleContext frkContext = createMock(BundleContext.class);
        BundleContext bundleContext = createMock(BundleContext.class);
        expect(frkBundle.getBundleContext()).andReturn(frkContext);
        FrameworkCollector.addFramework(FRAMEWORK_STATES[0], KEYS[3], frkBundle, ORIGIN);
        FrameworkCollector.addFramework(FRAMEWORK_STATES[2], KEYS[4], bundle, ORIGIN);

        long id = getFirstFrameworkId();
        String bndIdToString = "" + id;
        String frkIdToString = "" + (id - 1);
        StubCommandInterpreter cmdInterpreter = new StubCommandInterpreter(frkIdToString, "start", bndIdToString);

        expect(frkContext.getBundle(id)).andReturn(bundle);
        bundle.start();
        expect(bundle.getState()).andReturn(Bundle.STARTING);
        expect(bundle.getSymbolicName()).andReturn(BUNDLE_SYMBOLIC_NAME);
        expect(bundle.getVersion()).andReturn(Version.emptyVersion);
        replay(frkBundle, bundle, frkContext, bundleContext);

        cmd._frk(cmdInterpreter);
        StringBuffer commandResult = cmdInterpreter.getCommandOutput();
        Assert.assertTrue(commandResult.toString().contains("Bundle [" + BUNDLE_SYMBOLIC_NAME + "_" + Version.emptyVersion + "] started successfully in framework"));
        verify(bundle, bundleContext);
    }

    @Test
    public void test_frkStart2Bundle() throws BundleException {
        FrameworkInfoCommand cmd = new FrameworkInfoCommand();
        Bundle frkBundle = createMock(Bundle.class);
        Bundle bundle = createMock(Bundle.class);
        BundleContext frkContext = createMock(BundleContext.class);
        BundleContext bundleContext = createMock(BundleContext.class);
        expect(frkBundle.getBundleContext()).andReturn(frkContext);
        FrameworkCollector.addFramework(FRAMEWORK_STATES[0], KEYS[3], frkBundle, ORIGIN);
        FrameworkCollector.addFramework(FRAMEWORK_STATES[0], KEYS[4], bundle, ORIGIN);

        long id = getFirstFrameworkId();
        String bndIdToString = "" + id;
        String frkIdToString = "" + (id - 1);
        StubCommandInterpreter cmdInterpreter = new StubCommandInterpreter(frkIdToString, "start", bndIdToString);

        expect(frkContext.getBundle(id)).andReturn(bundle);
        expect(bundle.getState()).andReturn(Bundle.ACTIVE);
        expect(bundle.getSymbolicName()).andReturn(BUNDLE_SYMBOLIC_NAME);
        expect(bundle.getVersion()).andReturn(Version.emptyVersion);
        replay(frkBundle, bundle, frkContext, bundleContext);

        cmd._frk(cmdInterpreter);
        StringBuffer commandResult = cmdInterpreter.getCommandOutput();
        Assert.assertTrue(commandResult.toString().contains(BUNDLE_SYMBOLIC_NAME + "_" + Version.emptyVersion));
        Assert.assertTrue(commandResult.toString().contains("already started"));
        verify(bundle, bundleContext);
    }

    @Test
    public void testGetHelp() {
        FrameworkInfoCommand cmd = new FrameworkInfoCommand();
        Assert.assertNotNull(cmd.getHelp());
    }

    @Test
    public void test_frkGetpropStandardProperties() {
        FrameworkInfoCommand cmd = new FrameworkInfoCommand();
        Bundle frkBundle = createMock(Bundle.class);
        BundleContext frkContext = createMock(BundleContext.class);
        expect(frkBundle.getBundleContext()).andReturn(frkContext);
        int i = 0;
        for (Object prop : System.getProperties().keySet()) {
          expect(frkContext.getProperty((String)prop)).andReturn("property_" + (i + 1)).times(1);
          i++;
      }
        FrameworkCollector.addFramework(FRAMEWORK_STATES[0], KEYS[4], frkBundle, ORIGIN);
        replay(frkBundle, frkContext);
        StubCommandInterpreter cmdInterpreter = new StubCommandInterpreter("" + getFirstFrameworkId(), "getprop");
        cmd._frk(cmdInterpreter);
        StringBuffer commandResult = cmdInterpreter.getCommandOutput();
        System.out.println(commandResult);
        for (int j = 0; j < OSGI_PROPERTIES.length; j++) {
            Assert.assertTrue(commandResult.toString().contains("property_" + (j + 1)));
        }
        verify(frkBundle, frkContext);
    }

    @Test
    public void test_frkVisibility() {
        FrameworkInfoCommand cmd = new FrameworkInfoCommand();
        Bundle bundle = createMock(Bundle.class);
        BundleContext bundleContext = createMock(BundleContext.class);
        expect(bundle.getBundleContext()).andReturn(bundleContext).times(2);
        expect(bundleContext.getProperty(OSGI_PROPERTIES[1])).andReturn("property_" + 1);
        expect(bundleContext.getProperty(OSGI_PROPERTIES[2])).andReturn("property_" + 2);
        FrameworkCollector.addFramework(FRAMEWORK_STATES[1], KEYS[1], bundle, ORIGIN);
        StubCommandInterpreter cmdInterpreter = new StubCommandInterpreter("" + getFirstFrameworkId(), "visibility");
        replay(bundle, bundleContext);
        cmd._frk(cmdInterpreter);
        StringBuffer commandResult = cmdInterpreter.getCommandOutput();
        Assert.assertTrue(commandResult.toString().contains("org.osgi.framework.bootdelegation = property_1" + NEW_LINE +
                                                            "osgi.parentClassloader = property_2"));
        verify(bundle, bundleContext);
    }

	@After
    public void cleanUp() {
        for (String KEY : KEYS) {
            FrameworkCollector.removeFramework(KEY);
        }
        int frameworksCount = FrameworkCollector.getFrameworks().size();
        Assert.assertTrue("There should not be any frameworks available.", frameworksCount == 0);
    }
}
