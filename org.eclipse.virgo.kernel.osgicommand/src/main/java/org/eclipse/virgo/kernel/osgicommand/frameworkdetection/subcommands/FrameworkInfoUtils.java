/*
 * This file is part of the Virgo Web Server.
 *
 * Copyright (c) 2010 Eclipse Foundation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Borislav Kapukaranov, SAP AG - initial contribution
 */

package org.eclipse.virgo.kernel.osgicommand.frameworkdetection.subcommands;

import org.eclipse.osgi.framework.console.CommandInterpreter;
import org.eclipse.virgo.kernel.frameworkdetection.lib.FrameworkCollector;
import org.osgi.framework.Bundle;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * This is a helper class needed for the operation of the Inner Frameworks Detection commands
 *
 * @author Borislav Kapukaranov (borislav.kapukaranov@sap.com)
 */
public class FrameworkInfoUtils {

    public static int fixedIndex = -1;
    public static final String NEW_LINE = "\r\n";
    public static final String START_LIST_HEADER = "Current state of the OSGi Frameworks:" + NEW_LINE;


    // some default osgi properties
    public static final String[] osgiProps = {
            "org.osgi.framework.system.packages",
            "org.osgi.framework.bootdelegation",
            "osgi.parentClassloader",
            "osgi.install.area",
            "osgi.configuration.area",
            "osgi.sharedConfiguration.area",
            "osgi.instance.area",
            "osgi.instance.area.default",
            "osgi.user.area",
            "osgi.user.area.default",
            "osgi.manifest.cache",
            "user.home",
            "user.dir",
            "osgi.noShutdown",
            "osgi.compatibility.bootdelegation",
            "org.osgi.framework.vendor",
            "osgi.bundlefile.limit",
            "osgi.logfile",
            "osgi.framework.extensions",
            "osgi.frameworkClassPath"
    };

    protected static String getOSGiProps(long frkIndex, Object bundleContext) throws Exception {
        StringBuilder output = new StringBuilder();
        for (String key : osgiProps) {
            try {
                if (getProperty(bundleContext, key) != null) {
                    output.append(key).append(" = ").append(getProperty(bundleContext, key)).append(NEW_LINE);
                }
            } catch (Exception e) {
                throw e;
            }
        }
        return output.toString();
    }

    /**
     * Helper method
     *
     * @return the current registered framework count
     */
    public static int getFrameworksCount() {
        return FrameworkCollector.getFrameworks().values().size();
    }

    //Utility methods below

    protected static String getStateName(int state) {
        switch (state) {
            case Bundle.UNINSTALLED:
                return "UNINSTALLED "; //$NON-NLS-1$

            case Bundle.INSTALLED:
                return "INSTALLED   "; //$NON-NLS-1$

            case Bundle.RESOLVED:
                return "RESOLVED    "; //$NON-NLS-1$

            case Bundle.STARTING:
                return "STARTING    "; //$NON-NLS-1$

            case Bundle.STOPPING:
                return "STOPPING    "; //$NON-NLS-1$

            case Bundle.ACTIVE:
                return "ACTIVE      "; //$NON-NLS-1$

            default:
                return Integer.toHexString(state);
        }
    }

    protected static Object loadClass(Object bundle, String name) throws SecurityException, NoSuchMethodException, IllegalArgumentException, IllegalAccessException, InvocationTargetException {
        Method m = bundle.getClass().getMethod("loadClass", new Class[]{String.class});
        m.setAccessible(true);
        return m.invoke(bundle, new Object[]{name});
    }

    protected static Object getResource(Object bundle, String name) throws SecurityException, NoSuchMethodException, IllegalArgumentException, IllegalAccessException, InvocationTargetException {
        Method m = bundle.getClass().getMethod("getResource", new Class[]{String.class});
        m.setAccessible(true);
        return m.invoke(bundle, new Object[]{name});
    }

    protected static Object getProperty(Object bundleContext, String key) throws SecurityException, NoSuchMethodException, IllegalArgumentException, IllegalAccessException, InvocationTargetException {
        Method m = bundleContext.getClass().getMethod("getProperty", new Class[]{String.class});
        m.setAccessible(true);
        return m.invoke(bundleContext, new Object[]{key});
    }

    protected static Object getBundleContext(Object systemBundle) throws IllegalArgumentException, SecurityException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        Method m = systemBundle.getClass().getMethod("getBundleContext", new Class[]{});
        m.setAccessible(true);
        return m.invoke(systemBundle, new Object[]{});
    }

    protected static Object[] getBundles(Object bundleContext) throws SecurityException, NoSuchMethodException, IllegalArgumentException, IllegalAccessException, InvocationTargetException {
        Method m = bundleContext.getClass().getMethod("getBundles", new Class[]{});
        m.setAccessible(true);
        return (Object[]) m.invoke(bundleContext, new Object[]{});
    }

    protected static Object getBundle(Object bundleContext, long bndIndex) throws SecurityException, NoSuchMethodException, IllegalArgumentException, IllegalAccessException, InvocationTargetException {
        Method m = bundleContext.getClass().getMethod("getBundle", new Class[]{long.class});
        m.setAccessible(true);
        return m.invoke(bundleContext, new Object[]{bndIndex});
    }

    protected static Object getBundleId(Object bundle) throws SecurityException, NoSuchMethodException, IllegalArgumentException, IllegalAccessException, InvocationTargetException {
        Method m = bundle.getClass().getMethod("getBundleId", new Class[]{});
        m.setAccessible(true);
        return m.invoke(bundle, new Object[]{});
    }

    protected static Object getSymbolicName(Object bundle) throws SecurityException, NoSuchMethodException, IllegalArgumentException, IllegalAccessException, InvocationTargetException {
        Method m = bundle.getClass().getMethod("getSymbolicName", new Class[]{});
        m.setAccessible(true);
        return m.invoke(bundle, new Object[]{});
    }

    protected static int getState(Object bundle) throws SecurityException, NoSuchMethodException, IllegalArgumentException, IllegalAccessException, InvocationTargetException {
        Method m = bundle.getClass().getMethod("getState", new Class[]{});
        m.setAccessible(true);
        return (Integer) m.invoke(bundle, new Object[]{});
    }

    protected static Object start(Object bundle) throws SecurityException, NoSuchMethodException, IllegalArgumentException, IllegalAccessException, InvocationTargetException {
        Method m = bundle.getClass().getMethod("start", new Class[]{});
        m.setAccessible(true);
        return m.invoke(bundle, new Object[]{});
    }

    protected static Object stop(Object bundle) throws SecurityException, NoSuchMethodException, IllegalArgumentException, IllegalAccessException, InvocationTargetException {
        Method m = bundle.getClass().getMethod("stop", new Class[]{});
        m.setAccessible(true);
        return m.invoke(bundle, new Object[]{});
    }

    protected static Object update(Object bundle) throws SecurityException, NoSuchMethodException, IllegalArgumentException, IllegalAccessException, InvocationTargetException {
        Method m = bundle.getClass().getMethod("update", new Class[]{});
        m.setAccessible(true);
        return m.invoke(bundle, new Object[]{});
    }

    protected static Object uninstall(Object bundle) throws SecurityException, NoSuchMethodException, IllegalArgumentException, IllegalAccessException, InvocationTargetException {
        Method m = bundle.getClass().getMethod("uninstall", new Class[]{});
        m.setAccessible(true);
        return m.invoke(bundle, new Object[]{});
    }

    protected static Object install(Object bundleContext, String bndUrl) throws SecurityException, NoSuchMethodException, IllegalArgumentException, IllegalAccessException, InvocationTargetException {
        Method m = bundleContext.getClass().getMethod("installBundle", new Class[]{String.class});
        m.setAccessible(true);
        return m.invoke(bundleContext, new Object[]{bndUrl});
    }

    protected static Object getEquinoxProperties(ClassLoader cl) throws SecurityException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, ClassNotFoundException, InstantiationException {
        Class fp = Class.forName("org.eclipse.osgi.framework.internal.core.FrameworkProperties", true, cl);
        Method m = fp.getMethod("getProperties", new Class[]{});
        m.setAccessible(true);
        return m.invoke(fp.newInstance(), new Object[]{});
    }

    protected static Object getVersion(Object bundle) throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        Method m;
        Object result = null;
        m = bundle.getClass().getMethod("getVersion", new Class[]{});
        m.setAccessible(true);
        result = m.invoke(bundle, new Object[]{});
        return result;
    }

    public static long extractLongArg(CommandInterpreter interpreter) throws NumberFormatException {
        long bndIndex = -1;
        String temp = interpreter.nextArgument();
        bndIndex = Long.valueOf(temp);
        return bndIndex;
    }

    /**
     * @param e the exception to be processed
     * @return an error message depending on the exception type or <b>null</b> if the exception type is not recognized
     */
    protected static String handleException(Exception e) {
        if (e instanceof NumberFormatException) {
            return "Wrong index or not a number, try again with correct input" + NEW_LINE;
        }
        if (e instanceof SecurityException) {
            return "Error while trying to invoke a method with reflection, the error message is: " + e.getMessage() + NEW_LINE;
        }
        if (e instanceof IllegalArgumentException) {
            return "Error while executing operation, the error message is: " + e.getMessage() + NEW_LINE;
        }
        if (e instanceof NoSuchMethodException) {
            return "Error while trying to invoke a method with reflection, the invoked method is missing" + NEW_LINE;
        }
        if (e instanceof IllegalAccessException) {
            return "Error while trying to invoke a method with reflection, happens if the caller method enforces Java language access control and the underlying method is inaccessible" + NEW_LINE;
        }
        if (e instanceof InvocationTargetException) {
            return "Error while trying to invoke a method with reflection, the underlying method threw an exception" + NEW_LINE;
        }
        return null;
    }

}
