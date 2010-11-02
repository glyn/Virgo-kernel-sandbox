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

import junit.framework.Assert;
import org.eclipse.virgo.kernel.frameworkdetection.lib.FrameworkCollector;
import org.eclipse.virgo.kernel.frameworkdetection.lib.FrameworkData;
import org.eclipse.virgo.kernel.osgicommand.frameworkdetection.FrameworkInfoCommand;
import org.junit.After;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;

import java.util.concurrent.ConcurrentHashMap;

import static org.easymock.EasyMock.*;

/**
 * Class providing negative tests for frk shell command
 *
 * @author Borislav Kapukaranov (borislav.kapukaranov@sap.com)
 * @version 1.0
 */
public class FrameworkInfoCommandNegativeTests {

    private static final String DUMMY = "dummy";
    protected static final String FILE_SEPARATOR = System.getProperty("file.separator");
    protected static final String NEW_LINE = System.getProperty("line.separator");

    protected static final String FRAMEWORK_STATES[] = {"ACTIVE", "INSTALLED", "RESOLVED", "UNINSTALLED", "STARTING", "STOPPING"};
    protected static final String KEYS[] = {"11", "21", "31", "41", "51", "61"};
    protected static final Exception e = new Exception();
    protected static final StackTraceElement[] ORIGIN = e.getStackTrace();

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

    private static final String WRONG_BUNDLE_INDEX = "-15";
    private static final String UNSUPPORTED_BUNDLE_INDEX = "notSupportedIndex";
    private static final String INCORRECT_BUNDLE_LOCATION = "D:" + FILE_SEPARATOR + "test.jar";

    @Test
    public void testIncorrectInput() {
        FrameworkInfoCommand cmd = new FrameworkInfoCommand();
        StubCommandInterpreter cmdInterpreter = new StubCommandInterpreter("-d", WRONG_BUNDLE_INDEX, DUMMY);
        cmd._frk(cmdInterpreter);
        StringBuffer commandResult = cmdInterpreter.getCommandOutput();
        Assert.assertTrue(commandResult.toString().contains("Error"));
    }

    @Test
    public void testIncorrect_frkStopBundle() throws BundleException {
        FrameworkInfoCommand cmd = new FrameworkInfoCommand();
        StubCommandInterpreter cmdInterpreter = new StubCommandInterpreter(WRONG_BUNDLE_INDEX, "stop", WRONG_BUNDLE_INDEX, DUMMY);
        cmd._frk(cmdInterpreter);
        StringBuffer commandResult = cmdInterpreter.getCommandOutput();
        Assert.assertTrue(commandResult.toString().contains(WRONG_BUNDLE_INDEX));
        Assert.assertTrue(commandResult.toString().contains("not available"));
    }

    @Test
    public void testIncorrect_frkStopBundle2() throws BundleException {
        FrameworkInfoCommand cmd = new FrameworkInfoCommand();
        Bundle frkBundle = createMock(Bundle.class);
        Bundle bundle = createMock(Bundle.class);
        BundleContext frkContext = createMock(BundleContext.class);
        BundleContext bundleContext = createMock(BundleContext.class);
        expect(frkBundle.getBundleContext()).andReturn(frkContext);
        FrameworkCollector.addFramework(FRAMEWORK_STATES[0], KEYS[1], frkBundle, ORIGIN);
        FrameworkCollector.addFramework(FRAMEWORK_STATES[2], KEYS[2], bundle, ORIGIN);
        StubCommandInterpreter cmdInterpreter = new StubCommandInterpreter("" + (getFirstFrameworkId() - 1),
                "stop", WRONG_BUNDLE_INDEX, DUMMY);
        expect(frkContext.getBundle(Long.parseLong(WRONG_BUNDLE_INDEX))).andReturn(null);
        replay(frkBundle, bundle, frkContext, bundleContext);

        cmd._frk(cmdInterpreter);
        StringBuffer commandResult = cmdInterpreter.getCommandOutput();
        Assert.assertTrue(commandResult.toString().contains("Cannot find"));
        Assert.assertTrue(commandResult.toString().contains(WRONG_BUNDLE_INDEX));
        verify(bundle, bundleContext);
    }

    @Test
    public void testIncorrect_frkStopBundle3() throws BundleException {
        FrameworkInfoCommand cmd = new FrameworkInfoCommand();
        Bundle frkBundle = createMock(Bundle.class);
        Bundle bundle = createMock(Bundle.class);
        BundleContext frkContext = createMock(BundleContext.class);
        BundleContext bundleContext = createMock(BundleContext.class);
        expect(frkBundle.getBundleContext()).andReturn(frkContext);
        FrameworkCollector.addFramework(FRAMEWORK_STATES[0], KEYS[1], frkBundle, ORIGIN);
        FrameworkCollector.addFramework(FRAMEWORK_STATES[2], KEYS[2], bundle, ORIGIN);
        StubCommandInterpreter cmdInterpreter = new StubCommandInterpreter("" + (getFirstFrameworkId() - 1),
                "stop", UNSUPPORTED_BUNDLE_INDEX, DUMMY);
        replay(frkBundle, bundle, frkContext, bundleContext);

        cmd._frk(cmdInterpreter);
        StringBuffer commandResult = cmdInterpreter.getCommandOutput();
        System.out.println(commandResult);
        Assert.assertTrue(commandResult.toString().contains("Wrong index"));
        verify(bundle, bundleContext);
    }

    @Test
    public void testIncorrect_frkStartBundle() throws BundleException {
        FrameworkInfoCommand cmd = new FrameworkInfoCommand();
        StubCommandInterpreter cmdInterpreter = new StubCommandInterpreter(WRONG_BUNDLE_INDEX, "start", WRONG_BUNDLE_INDEX, DUMMY);
        cmd._frk(cmdInterpreter);
        StringBuffer commandResult = cmdInterpreter.getCommandOutput();
        Assert.assertTrue(commandResult.toString().contains(WRONG_BUNDLE_INDEX));
        Assert.assertTrue(commandResult.toString().contains("not available"));
    }

    @Test
    public void testIncorrect_frkStartBundle2() throws BundleException {
        FrameworkInfoCommand cmd = new FrameworkInfoCommand();
        Bundle frkBundle = createMock(Bundle.class);
        Bundle bundle = createMock(Bundle.class);
        BundleContext frkContext = createMock(BundleContext.class);
        BundleContext bundleContext = createMock(BundleContext.class);
        expect(frkBundle.getBundleContext()).andReturn(frkContext);
        FrameworkCollector.addFramework(FRAMEWORK_STATES[0], KEYS[1], frkBundle, ORIGIN);
        FrameworkCollector.addFramework(FRAMEWORK_STATES[2], KEYS[2], bundle, ORIGIN);
        StubCommandInterpreter cmdInterpreter = new StubCommandInterpreter("" + (getFirstFrameworkId() - 1),
                "start", WRONG_BUNDLE_INDEX, DUMMY);
        expect(frkContext.getBundle(Long.parseLong(WRONG_BUNDLE_INDEX))).andReturn(null);
        replay(frkBundle, bundle, frkContext, bundleContext);

        cmd._frk(cmdInterpreter);
        StringBuffer commandResult = cmdInterpreter.getCommandOutput();
        Assert.assertTrue(commandResult.toString().contains("Cannot find"));
        Assert.assertTrue(commandResult.toString().contains(WRONG_BUNDLE_INDEX));
        verify(bundle, bundleContext);
    }

    @Test
    public void testIncorrect_frkStartBundle3() throws BundleException {

        FrameworkInfoCommand cmd = new FrameworkInfoCommand();
        Bundle frkBundle = createMock(Bundle.class);
        Bundle bundle = createMock(Bundle.class);
        BundleContext frkContext = createMock(BundleContext.class);
        BundleContext bundleContext = createMock(BundleContext.class);
        expect(frkBundle.getBundleContext()).andReturn(frkContext);
        FrameworkCollector.addFramework(FRAMEWORK_STATES[0], KEYS[1], frkBundle, ORIGIN);
        FrameworkCollector.addFramework(FRAMEWORK_STATES[0], KEYS[1], frkBundle, ORIGIN);
        StubCommandInterpreter cmdInterpreter = new StubCommandInterpreter("" + getFirstFrameworkId(),
                "start", UNSUPPORTED_BUNDLE_INDEX, DUMMY);
        replay(frkBundle, bundle, frkContext, bundleContext);
        cmd._frk(cmdInterpreter);
        StringBuffer commandResult = cmdInterpreter.getCommandOutput();
        Assert.assertTrue(commandResult.toString().contains("Wrong index"));
        verify(bundle, bundleContext);

    }


    @Test
    public void testNotSupportedSubcommand() throws BundleException {
        FrameworkInfoCommand cmd = new FrameworkInfoCommand();
        Bundle frkBundle = createMock(Bundle.class);
        Bundle bundle = createMock(Bundle.class);
        BundleContext frkContext = createMock(BundleContext.class);
        BundleContext bundleContext = createMock(BundleContext.class);
        expect(frkBundle.getBundleContext()).andReturn(frkContext);
        FrameworkCollector.addFramework(FRAMEWORK_STATES[0], KEYS[3], frkBundle, ORIGIN);
        FrameworkCollector.addFramework(FRAMEWORK_STATES[2], KEYS[4], bundle, ORIGIN);
        long id = getFirstFrameworkId() - 1;
        StubCommandInterpreter cmdInterpreter = new StubCommandInterpreter("" + id, "notSupportedCommand", DUMMY);
        expect(frkContext.getBundle(id)).andReturn(bundle);
        replay(frkBundle, bundle, frkContext, bundleContext);
        cmd._frk(cmdInterpreter);
        StringBuffer commandResult = cmdInterpreter.getCommandOutput(); //
        System.out.println(commandResult);
        Assert.assertTrue(commandResult.toString().contains("notSupportedCommand"));
        Assert.assertTrue(commandResult.toString().contains("not recognized"));
        verify(bundle, bundleContext);
    }

    @Test
    public void testMissingSubcommandAfterFrkIndex() throws BundleException {
        FrameworkInfoCommand cmd = new FrameworkInfoCommand();
        Bundle frkBundle = createMock(Bundle.class);
        Bundle bundle = createMock(Bundle.class);
        BundleContext frkContext = createMock(BundleContext.class);
        BundleContext bundleContext = createMock(BundleContext.class);
        FrameworkCollector.addFramework(FRAMEWORK_STATES[0], KEYS[4], frkBundle, ORIGIN);
        FrameworkCollector.addFramework(FRAMEWORK_STATES[2], KEYS[5], bundle, ORIGIN);
        StubCommandInterpreter cmdInterpreter = new StubCommandInterpreter("" + (getFirstFrameworkId() - 1), null, null, DUMMY);
        replay(frkBundle, bundle, frkContext, bundleContext);
        cmd._frk(cmdInterpreter);
        StringBuffer commandResult = cmdInterpreter.getCommandOutput();
        Assert.assertTrue(commandResult.toString().contains("Missing subcommand"));
        verify(bundle, bundleContext, frkBundle, frkContext);
    }

    @Test
    public void testIncorrect_frkUpdateBundle() throws BundleException {
        FrameworkInfoCommand cmd = new FrameworkInfoCommand();
        StubCommandInterpreter cmdInterpreter = new StubCommandInterpreter(WRONG_BUNDLE_INDEX, "update", WRONG_BUNDLE_INDEX, DUMMY);
        cmd._frk(cmdInterpreter);
        StringBuffer commandResult = cmdInterpreter.getCommandOutput();
        Assert.assertTrue(commandResult.toString().contains("ERROR: Inner framework [" + WRONG_BUNDLE_INDEX + "] is not available, check if it is stopped"));
    }

    @Test
    public void testIncorrect_frkUpdateBundle2() throws BundleException {
        FrameworkInfoCommand cmd = new FrameworkInfoCommand();
        Bundle frkBundle = createMock(Bundle.class);
        Bundle bundle = createMock(Bundle.class);
        BundleContext frkContext = createMock(BundleContext.class);
        BundleContext bundleContext = createMock(BundleContext.class);
        expect(frkBundle.getBundleContext()).andReturn(frkContext);
        FrameworkCollector.addFramework(FRAMEWORK_STATES[0], KEYS[4], frkBundle, ORIGIN);
        FrameworkCollector.addFramework(FRAMEWORK_STATES[2], KEYS[5], bundle, ORIGIN);
        String frkIdToString = "" + (getFirstFrameworkId() - 1);
        StubCommandInterpreter cmdInterpreter = new StubCommandInterpreter(frkIdToString, "update", WRONG_BUNDLE_INDEX, DUMMY);
        expect(frkContext.getBundle(Long.parseLong(WRONG_BUNDLE_INDEX))).andReturn(null);
        replay(frkBundle, bundle, frkContext, bundleContext);

        cmd._frk(cmdInterpreter);
        StringBuffer commandResult = cmdInterpreter.getCommandOutput();
        //System.out.print(commandResult);
        Assert.assertTrue(commandResult.toString().contains("Cannot find"));
        Assert.assertTrue(commandResult.toString().contains(WRONG_BUNDLE_INDEX));
        Assert.assertTrue(commandResult.toString().contains(frkIdToString));
        verify(bundle, bundleContext);
    }

    @Test
    public void testIncorrect_frkUpdateBundle3() throws BundleException {

        FrameworkInfoCommand cmd = new FrameworkInfoCommand();
        Bundle frkBundle = createMock(Bundle.class);
        Bundle bundle = createMock(Bundle.class);
        BundleContext frkContext = createMock(BundleContext.class);
        BundleContext bundleContext = createMock(BundleContext.class);
        expect(frkBundle.getBundleContext()).andReturn(frkContext);
        FrameworkCollector.addFramework(FRAMEWORK_STATES[0], KEYS[1], frkBundle, ORIGIN);
        FrameworkCollector.addFramework(FRAMEWORK_STATES[0], KEYS[1], frkBundle, ORIGIN);
        StubCommandInterpreter cmdInterpreter = new StubCommandInterpreter("" + getFirstFrameworkId(),
                "update", UNSUPPORTED_BUNDLE_INDEX, DUMMY);
        replay(frkBundle, bundle, frkContext, bundleContext);
        cmd._frk(cmdInterpreter);
        StringBuffer commandResult = cmdInterpreter.getCommandOutput();
        Assert.assertTrue(commandResult.toString().contains("Wrong index"));
        verify(bundle, bundleContext);

    }

    @Test
    public void testIncorrect_frkInstallBundle() throws BundleException {
        FrameworkInfoCommand cmd = new FrameworkInfoCommand();
        StubCommandInterpreter cmdInterpreter = new StubCommandInterpreter(WRONG_BUNDLE_INDEX, "install", WRONG_BUNDLE_INDEX, DUMMY);
        cmd._frk(cmdInterpreter);
        StringBuffer commandResult = cmdInterpreter.getCommandOutput();
        Assert.assertTrue(commandResult.toString().contains(WRONG_BUNDLE_INDEX));
        Assert.assertTrue(commandResult.toString().contains("not available"));
    }

    @Test
    public void testIncorrect_frkInstallBundle2() throws BundleException {
        FrameworkInfoCommand cmd = new FrameworkInfoCommand();
        Bundle frkBundle = createMock(Bundle.class);
        BundleContext frkContext = createMock(BundleContext.class);
        Bundle bundle = createMock(Bundle.class);
        BundleContext bundleContext = createMock(BundleContext.class);
        expect(bundle.getBundleContext()).andReturn(bundleContext);
        FrameworkCollector.addFramework(FRAMEWORK_STATES[1], KEYS[2], bundle, ORIGIN);
        FrameworkCollector.addFramework(FRAMEWORK_STATES[1], KEYS[3], frkBundle, ORIGIN);
        replay(bundle, bundleContext, frkBundle, frkContext);
        StubCommandInterpreter cmdInterpreter = new StubCommandInterpreter("" + (getFirstFrameworkId() - 1),
                "install", INCORRECT_BUNDLE_LOCATION, DUMMY);
        cmd._frk(cmdInterpreter);
        StringBuffer commandResult = cmdInterpreter.getCommandOutput();
        Assert.assertTrue(commandResult.toString().contains(
                "Installation failed"));
        verify(bundle, bundleContext, frkBundle, frkContext);
    }

    @Test
    public void testIncorrect_frkUninstallBundle() throws BundleException {
        FrameworkInfoCommand cmd = new FrameworkInfoCommand();
        StubCommandInterpreter cmdInterpreter = new StubCommandInterpreter(WRONG_BUNDLE_INDEX, "uninstall", WRONG_BUNDLE_INDEX, DUMMY);
        cmd._frk(cmdInterpreter);
        StringBuffer commandResult = cmdInterpreter.getCommandOutput();
        Assert.assertTrue(commandResult.toString().contains(WRONG_BUNDLE_INDEX));
        Assert.assertTrue(commandResult.toString().contains("not available"));
    }

    @Test
    public void testIncorrect_frkUninstallBundle2() throws BundleException {
        FrameworkInfoCommand cmd = new FrameworkInfoCommand();
        Bundle frkBundle = createMock(Bundle.class);
        Bundle bundle = createMock(Bundle.class);
        BundleContext frkContext = createMock(BundleContext.class);
        BundleContext bundleContext = createMock(BundleContext.class);
        expect(frkBundle.getBundleContext()).andReturn(frkContext);
        FrameworkCollector.addFramework(FRAMEWORK_STATES[0], KEYS[4], frkBundle, ORIGIN);
        FrameworkCollector.addFramework(FRAMEWORK_STATES[2], KEYS[5], bundle, ORIGIN);
        String frkIdToString = "" + (getFirstFrameworkId() - 1);
        StubCommandInterpreter cmdInterpreter = new StubCommandInterpreter(frkIdToString, "uninstall", WRONG_BUNDLE_INDEX, DUMMY);
        expect(frkContext.getBundle(Long.parseLong(WRONG_BUNDLE_INDEX))).andReturn(null);
        replay(frkBundle, bundle, frkContext, bundleContext);

        cmd._frk(cmdInterpreter);
        StringBuffer commandResult = cmdInterpreter.getCommandOutput();
        Assert.assertTrue(commandResult.toString().contains("Cannot find"));
        Assert.assertTrue(commandResult.toString().contains(WRONG_BUNDLE_INDEX));
        Assert.assertTrue(commandResult.toString().contains(frkIdToString));
        verify(bundle, bundleContext, frkBundle, frkContext);
    }

    @Test
    public void testIncorrect_frkUninstallBundle3() throws BundleException {
        FrameworkInfoCommand cmd = new FrameworkInfoCommand();
        Bundle frkBundle = createMock(Bundle.class);
        Bundle bundle = createMock(Bundle.class);
        BundleContext frkContext = createMock(BundleContext.class);
        BundleContext bundleContext = createMock(BundleContext.class);
        expect(frkBundle.getBundleContext()).andReturn(frkContext);
        FrameworkCollector.addFramework(FRAMEWORK_STATES[0], KEYS[1], frkBundle, ORIGIN);
        FrameworkCollector.addFramework(FRAMEWORK_STATES[0], KEYS[1], frkBundle, ORIGIN);
        StubCommandInterpreter cmdInterpreter = new StubCommandInterpreter("" + getFirstFrameworkId(),
                "uninstall", UNSUPPORTED_BUNDLE_INDEX, DUMMY);
        replay(frkBundle, bundle, frkContext, bundleContext);
        cmd._frk(cmdInterpreter);
        StringBuffer commandResult = cmdInterpreter.getCommandOutput();
        Assert.assertTrue(commandResult.toString().contains("Wrong index"));
        verify(bundle, bundleContext);
    }

    @Test
    public void testWrongArgument() {
        FrameworkInfoCommand cmd = new FrameworkInfoCommand();
        StubCommandInterpreter cmdInterpreter = new StubCommandInterpreter("notSupportedCommand", DUMMY);
        cmd._frk(cmdInterpreter);
        StringBuffer commandResult = cmdInterpreter.getCommandOutput();
        System.out.println(commandResult);
        Assert.assertTrue(commandResult.toString().contains("Wrong index or argument"));
        Assert.assertTrue(commandResult.toString().contains("notSupportedCommand"));
    }

    @Test
    public void testNullArgument() {
        FrameworkInfoCommand cmd = new FrameworkInfoCommand();
        StubCommandInterpreter cmdInterpreter = new StubCommandInterpreter(new String[]{null});
        cmd._frk(cmdInterpreter);
        StringBuffer commandResult = cmdInterpreter.getCommandOutput();
        Assert.assertTrue(commandResult.toString().contains("Error"));
    }

    @Test
    public void testIncorrect_frkListBundlesCommand() {
        FrameworkInfoCommand cmd = new FrameworkInfoCommand();
        StubCommandInterpreter cmdInterpreter = new StubCommandInterpreter(WRONG_BUNDLE_INDEX, "ss", WRONG_BUNDLE_INDEX, DUMMY);
        cmd._frk(cmdInterpreter);
        StringBuffer commandResult = cmdInterpreter.getCommandOutput();
        Assert.assertTrue(commandResult.toString().contains(WRONG_BUNDLE_INDEX));
        Assert.assertTrue(commandResult.toString().contains("not available"));
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
