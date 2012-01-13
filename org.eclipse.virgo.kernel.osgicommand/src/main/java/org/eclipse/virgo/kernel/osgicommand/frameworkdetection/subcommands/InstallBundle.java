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

/**
 * Handles the install operations of bundles in a target framework
 */
public class InstallBundle implements SubCommandExecutor {
    private final String NEW_LINE = FrameworkInfoUtils.NEW_LINE;

    public String execute(long frkIndex, CommandInterpreter interpreter) throws Exception {
        StringBuilder output = new StringBuilder();
        try {
            String url = interpreter.nextArgument();
            if (url == null || url.equals("")) {
                throw new IllegalArgumentException("Mandatory target bundle's URL is missing");
            }

            FrameworkData info = FrameworkCollector.getFrameworkByID(frkIndex);
            output.append("Installing bundle [").append(url).append("] in framework [").append(frkIndex).append("]..").append(NEW_LINE);
            if (info == null) {
                output.append("ERROR: Inner framework [").append(frkIndex).append("] is not available, check if it is stopped").append(NEW_LINE);
                return output.toString();
            }

            Object innerCtx = FrameworkInfoUtils.getBundleContext(info.getBundle());
            String bndUrl = url;
            if (!url.startsWith("file:")) {
                bndUrl = "file:" + url;
            }
            Object installedBundle = FrameworkInfoUtils.install(innerCtx, bndUrl);
            output.append("Bundle [").append(FrameworkInfoUtils.getSymbolicName(installedBundle)).append("_").append(FrameworkInfoUtils.getVersion(installedBundle)).append("] installed successfuly with id [").append(FrameworkInfoUtils.getBundleId(installedBundle)).append("]").append(NEW_LINE);
        } catch (Exception e) {
            String errorMsg = FrameworkInfoUtils.handleException(e);
            if (errorMsg != null) {
                output.append("Installation failed: " + errorMsg + ", execute the command with '-debug' option at the end to see the cause");
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
