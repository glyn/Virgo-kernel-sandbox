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
 * Handles the listing of bundles in a target framework
 */
public class ListBundles implements SubCommandExecutor {
    private final String NEW_LINE = FrameworkInfoUtils.NEW_LINE;

    public String execute(long index, CommandInterpreter interpreter) throws Exception {
        StringBuilder output = new StringBuilder();
        FrameworkData info = FrameworkCollector.getFrameworkByID(index);
        output.append("Listing bundles in framework [").append(index).append("]..").append(NEW_LINE);
        if (info == null) {
            output.append("Inner framework [").append(index).append("] is not available, check if it is stopped").append(NEW_LINE);
            return output.toString();
        }
        output.append("----------------------------------------------------------").append(NEW_LINE);
        output.append("id\tState       Bundle").append(NEW_LINE);
        Object innerCtx = null;
        try {
            innerCtx = FrameworkInfoUtils.getBundleContext(info.getBundle());
            Object[] innerBundles = FrameworkInfoUtils.getBundles(innerCtx);
            for (Object bnd : innerBundles) {
                output.append(FrameworkInfoUtils.getBundleId(bnd)).append("\t").append(FrameworkInfoUtils.getStateName((Integer) FrameworkInfoUtils.getState(bnd))).append(FrameworkInfoUtils.getSymbolicName(bnd)).append("_").append(FrameworkInfoUtils.getVersion(bnd)).append(NEW_LINE);
            }
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
