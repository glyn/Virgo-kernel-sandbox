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

import org.eclipse.core.runtime.internal.adaptor.URLConverterImpl;
import org.eclipse.osgi.framework.console.CommandInterpreter;
import org.eclipse.virgo.kernel.frameworkdetection.lib.FrameworkCollector;
import org.eclipse.virgo.kernel.frameworkdetection.lib.FrameworkData;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

/**
 * Handles output of some debug and supportability information about a target framework,
 * such as what are its bootDelegation and parentClassLoader property values,
 * as well as what are the resources of its system bundle loaders
 */
public class CheckVisibility implements SubCommandExecutor {
    private final String NEW_LINE = FrameworkInfoUtils.NEW_LINE;
    private String[] osgiProps = FrameworkInfoUtils.osgiProps;

    public String execute(long frkIndex, CommandInterpreter interpreter) throws Exception {
        StringBuilder output = new StringBuilder();
        FrameworkData info = FrameworkCollector.getFrameworkByID(frkIndex);
        try {
            output.append(NEW_LINE);
            output.append("This framework's:").append(NEW_LINE);
            output.append("----------------------------------------------------------").append(NEW_LINE);
            Object frkParentLoader = FrameworkInfoUtils.getProperty(FrameworkInfoUtils.getBundleContext(info.getBundle()), osgiProps[2]);
            output.append(osgiProps[1]).append(" = ").append(FrameworkInfoUtils.getProperty(FrameworkInfoUtils.getBundleContext(info.getBundle()), osgiProps[1])).append(NEW_LINE);
            output.append(osgiProps[2]).append(" = ").append(frkParentLoader == null ? "boot" : frkParentLoader).append(NEW_LINE);
            output.append(NEW_LINE);
            output.append("bundle loader resources:").append(NEW_LINE);
            URLConverterImpl converter = new URLConverterImpl();
            ClassLoader cl = info.getBundle().getClass().getClassLoader();
            if (cl instanceof URLClassLoader) {
                URL[] jars = ((URLClassLoader) cl).getURLs();
                for (URL jar : jars) {
                    output.append(jar).append(NEW_LINE);
                }
            } else {
                Enumeration<URL> jars = cl.getResources("/META-INF/MANIFEST.MF");
                Set<String> res = new HashSet<String>();
                while (jars.hasMoreElements()) {
                    res.add(converter.toFileURL(jars.nextElement()).toString().substring(6));
                }
                for (String strRes : res) {
                    output.append(strRes.substring(0, strRes.length() - "/META-INF/MANIFEST.MF".length())).append(NEW_LINE);
                }
            }
            output.append(NEW_LINE);
            output.append("bundle parent loader resources:").append(NEW_LINE);
            ClassLoader parent = cl.getParent();
            //if the parent is null print no resources
            if (parent != null) {
                if (cl.getParent() instanceof URLClassLoader) {
                    URL[] jars = ((URLClassLoader) cl.getParent()).getURLs();
                    for (URL jar : jars) {
                        output.append(jar).append(NEW_LINE);
                    }
                } else {
                    Set<String> res = new HashSet<String>();
                    Enumeration<URL> jars_enum = cl.getParent().getResources("/META-INF/MANIFEST.MF");
                    res.clear();
                    while (jars_enum.hasMoreElements()) {
                        res.add(converter.toFileURL(jars_enum.nextElement()).toString().substring(6));
                    }
                    for (String strRes : res) {
                        output.append(strRes.substring(0, strRes.length() - "/META-INF/MANIFEST.MF".length())).append(NEW_LINE);
                    }
                }
            } else {
                output.append("No parent loader detected").append(NEW_LINE);
            }
        } catch (IOException e) {
            interpreter.println("IO Error occured while trying to get the resources of a classloader");
            interpreter.printStackTrace(e);
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
        return output.toString();
    }
}
