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
import org.eclipse.virgo.kernel.frameworkdetection.lib.FrameworkData;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles list operations in a tree output of the current framework structure. After every change the user has to invoke that method
 * so that the structure is updated with the newest frameworks and their indexes. The framework indexes are NOT CONSTANT
 * once they are registered, so every on every change of the structure the tree must be refreshed with a list
 */
public class GetFrameworksDetails implements CommandExecutor {

    private final String NEW_LINE = FrameworkInfoUtils.NEW_LINE;
    private static int fixedIndex = -1;
    private BundleContext context = null;
    private boolean needDetails = false;
    private CommandInterpreter interpreter;

    public GetFrameworksDetails(BundleContext context, CommandInterpreter interpreter, boolean details) {
        this.context = context;
        this.needDetails = details;
        this.interpreter = interpreter;
    }

    public String execute() throws Exception {
        ConcurrentHashMap<String, FrameworkData> frameworks = FrameworkCollector.getFrameworks();
        StringBuilder output = new StringBuilder();
        output.append(FrameworkInfoUtils.START_LIST_HEADER);
        output.append("Avaliable frameworks ").append(FrameworkInfoUtils.getFrameworksCount()).append(NEW_LINE);
        output.append("----------------------------------------------------------");
        output.append(NEW_LINE);
        FrameworkData farOuterFramework = null;
        try {
            ArrayList<Long> passed = new ArrayList<Long>();
            Hashtable<Long, Object> frk_origin_map = new Hashtable<Long, Object>();
            Hashtable<Long, ArrayList<Long>> relations = new Hashtable<Long, ArrayList<Long>>();
            ArrayList<Long> currentLevel = new ArrayList<Long>();
            boolean isOuterFrkDiscovered = false;
            for (FrameworkData info : frameworks.values()) {
                for (Object b : context.getBundles()) {
                    // find the outer framework
                    // if all stack trace elements can  be loaded by some bundle then this is the outer framework and that bundle is its origin
                    // commonly this is the system bundle (org.eclipse.osgi)
                    StackTraceElement[] se = info.getOrigin();
                    boolean loadall = true;
                    for (StackTraceElement aSe : se) {
                        try {
                            FrameworkInfoUtils.loadClass(b, aSe.getClassName());
                        } catch (InvocationTargetException e) {
                            // class found, save & exit..
                            loadall = false;
                            break;
                        }
                    }
                    if (loadall && (Long) FrameworkInfoUtils.getBundleId(b) == 0) {
                        // all stack trace elements are loaded, store the outer framework and mark it as passed
                        farOuterFramework = info;
                        passed.add(info.getID());
                        currentLevel.add(farOuterFramework.getID());
                        frk_origin_map.put(farOuterFramework.getID(), b);
                        isOuterFrkDiscovered = true;
                        break;
                    }
                }
                if (isOuterFrkDiscovered)
                    break;
            }
            if (farOuterFramework == null) {
                output = new StringBuilder();
                output.append("Error while building the tree structure. Outer framework could not be determined.");
                return output.toString();
            }

            // process all frameworks to determine their relations
            for (FrameworkData current : frameworks.values()) {
                // here we will store the inner frameworks of the current level
                currentLevel = new ArrayList<Long>();
                for (FrameworkData inner : frameworks.values()) {
                    // check if we already looked at this framework and it is not the current one we are processing
                    if (!passed.contains(inner.getID()) && inner.getID() != current.getID()) {
                        Object bc = FrameworkInfoUtils.getBundleContext(current.getBundle());
                        Object outerSystemBundle = FrameworkInfoUtils.getBundle(bc, 0);
                        StackTraceElement[] se = inner.getOrigin();
                        String className = se[0].getClassName();
                        // find where the classloader changes
                        for (StackTraceElement aSe : se) {
                            try {
                                FrameworkInfoUtils.loadClass(outerSystemBundle, aSe.getClassName());
                                className = aSe.getClassName();
                            } catch (InvocationTargetException e) {
                                // class name found, save it & exit..
                                className = aSe.getClassName();
                                break;
                            }
                        }

                        // now we got the className and we'll try to see which bundle can load it to determine the possible origin bundles
                        // sometimes there may be more than one possible origin bundle
                        for (Object b : FrameworkInfoUtils.getBundles(bc)) {
                            if (FrameworkInfoUtils.getResource(b, className.replace(".", "/") + ".class") != null) {
                                frk_origin_map.put(inner.getID(), b);
                                currentLevel.add(inner.getID());
                                passed.add(inner.getID());
                            }
                        }
                    }
                }
                // Add the ID no matter if it has children or not. This way we can iterate without checks for null and the structure is more consistent:
                // 1 -> [2]
                // 2 -> [empty array]
                relations.put(current.getID(), currentLevel);
            }

            // output tree
            fixedIndex = -1;
            // create a preliminary framework tree
            String tempTree = processTree(farOuterFramework.getID(), "\\___", relations, frk_origin_map);
            fixedIndex = -1;
            // normalize the framework indexes so that all are positive numbers and are in ascending order
            normalizeFrameworkIds();
            //remove the " \\___" for the root level
            output.append(tempTree.substring(4));
            output.append(NEW_LINE);

            // check if further data has to be displayed
            if (needDetails) {
                for (FrameworkData info : frameworks.values()) {
                    if (context != null) {
                        // detailed info in list
                        output.append(NEW_LINE).append("[ ").append(info.getID()).append(" ] - [").append(info.getFrameworkState()).append("]").append(NEW_LINE);
                        output.append("- system bundle classname: [").append(info.getBundle().getClass().getName()).append("]").append(NEW_LINE);
                        Bundle systemBundle = context.getBundle(0);
                        StackTraceElement[] se = info.getOrigin();
                        String className = se[0].getClassName();
                        boolean loadedAll = true;
                        // find where the classloader changes
                        for (StackTraceElement aSe : se) {
                            try {
                                systemBundle.loadClass(aSe.getClassName());
                                className = aSe.getClassName();
                            } catch (ClassNotFoundException e) {
                                // class found, save & exit..
                                className = aSe.getClassName();
                                loadedAll = false;
                                break;
                            }
                        }
                        ArrayList<Bundle> originBundles = new ArrayList<Bundle>();
                        if (loadedAll) {
                            originBundles.add(context.getBundle(0));
                        } else {
                            for (Bundle b : context.getBundles()) {
                                if (FrameworkInfoUtils.getResource(b, className.replace(".", "/") + ".class") != null) {
                                    originBundles.add(b);
                                }
                            }
                        }
                        if (originBundles.size() > 0) {
                            for (Bundle originBundle : originBundles) {
                                output.append("- possibly started from bundle: [").append(originBundle.getSymbolicName()).append("_").append(originBundle.getVersion()).append("]").append(NEW_LINE);
                            }
                        } else {
                            output.append("- possibly started from bundle: [ERROR: could not determine the origin bundle]").append(NEW_LINE);
                        }

                        output.append("- on [").append(info.getDate()).append("]").append(NEW_LINE);
                        if (info.getBundle().getClass().getClassLoader() != null) {
                            output.append("- inner frk sys.bundle loader: [").append(info.getBundle().getClass().getClassLoader()).append("]").append(NEW_LINE);
                        } else {
                            output.append("- inner frk sys.bundle loader: [ data not available ]").append(NEW_LINE);
                        }
                        if (info.getBundle().getClass().getClassLoader().getParent() != null) {
                            output.append("- inner frk sys.bundle parent loader: [").append(info.getBundle().getClass().getClassLoader().getParent()).append("]").append(NEW_LINE);
                        } else {
                            output.append("- inner frk sys.bundle parent loader: [ data not available ]").append(NEW_LINE);
                        }

                        output.append("Startup Call stack:").append(NEW_LINE);
                        for (StackTraceElement el : info.getOrigin()) {
                            output.append(el.toString()).append("").append(NEW_LINE);
                        }

                    } else {
                        // use the default toString with no origin bundle found
                        output.append(info.toString(true));
                    }
                }
            }
            return output.toString();
        } catch (Exception e) {
            String errorMsg = FrameworkInfoUtils.handleException(e);
            if (errorMsg != null) {
                output.append(errorMsg);
                boolean isInDebugMode = false;
                String debugOpt = interpreter.nextArgument();
                if (debugOpt != null && debugOpt.equals("-debug")) {
                    isInDebugMode = true;
                }
                if (isInDebugMode) {
                    interpreter.println(errorMsg);
                    interpreter.printStackTrace(e);
                }
                return output.toString();
            } else {
                throw e;
            }
        }
    }

    private static void normalizeFrameworkIds() {
        Map<String, FrameworkData> frameworks = FrameworkCollector.getFrameworks();
        for (FrameworkData info : frameworks.values()) {
            info.setID(info.getID() * (-1));
        }
    }

    private static String processTree(long innerId, String offset, Hashtable<Long, ArrayList<Long>> relations, Hashtable<Long, Object> frkOriginMap) throws SecurityException, IllegalArgumentException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        StringBuilder result = new StringBuilder();
        FrameworkData frkData = FrameworkCollector.getFrameworkByID(innerId);
        if (frkData != null) {
            String osgi = frkData.getBundle().getClass().getName();
            frkData.setID(fixedIndex);
            if (osgi.contains("eclipse")) {
                osgi = "Equinox";
            } else {
                if (osgi.contains("felix")) {
                    osgi = "Felix";
                } else {
                    osgi = "OSGi";
                }
            }
            result.append(offset).append("[ ").append(fixedIndex * (-1)).append(" ] ").append(osgi).append(" [").append(FrameworkInfoUtils.getSymbolicName(frkOriginMap.get(innerId))).append("_").append(FrameworkInfoUtils.getVersion(frkOriginMap.get(innerId))).append("]").append(FrameworkInfoUtils.NEW_LINE);
            for (Long lvl : relations.get(innerId)) {
                fixedIndex--;
                String frkResult = processTree(lvl, "     " + offset, relations, frkOriginMap);
                if (frkResult != null) {
                    result.append(frkResult);
                }
            }
            return result.toString();
        } else {
            return null;
        }
    }

}
