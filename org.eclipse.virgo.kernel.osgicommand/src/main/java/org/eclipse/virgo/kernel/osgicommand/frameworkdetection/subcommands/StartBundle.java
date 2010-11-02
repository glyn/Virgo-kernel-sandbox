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

/**
 * Handles the start operations of bundles in a target framework
 */
public class StartBundle implements SubCommandExecutor {
    private final String NEW_LINE = FrameworkInfoUtils.NEW_LINE;

    public String execute(long frkIndex, CommandInterpreter interpreter) throws Exception {
        StringBuilder output = new StringBuilder();
        try {
            long bndIndex = FrameworkInfoUtils.extractLongArg(interpreter);

            FrameworkData info = FrameworkCollector.getFrameworkByID(frkIndex);
            output.append("Starting bundle [").append(bndIndex).append("] in framework [").append(frkIndex).append("]..").append(NEW_LINE);
            if (info == null) {
                output.append("ERROR: Inner framework [").append(frkIndex).append("] is not available, check if it is stopped").append(NEW_LINE);
                return output.toString();
            }

            Object innerCtx = FrameworkInfoUtils.getBundleContext(info.getBundle());
            Object bnd = FrameworkInfoUtils.getBundle(innerCtx, bndIndex);
            if (bnd != null) {
                if (FrameworkInfoUtils.getState(bnd) != Bundle.ACTIVE) {
                    FrameworkInfoUtils.start(bnd);
                    output.append("Bundle [").append(FrameworkInfoUtils.getSymbolicName(bnd)).append("_").append(FrameworkInfoUtils.getVersion(bnd)).append("] started successfully in framework [").append(frkIndex).append("]!").append(NEW_LINE);
                } else {
                    output.append("Bundle [").append(FrameworkInfoUtils.getSymbolicName(bnd)).append("_").append(FrameworkInfoUtils.getVersion(bnd)).append("] already started in framework [").append(frkIndex).append("]!").append(NEW_LINE);
                }
            } else {
                output.append("ERROR: Cannot find bundle [").append(bndIndex).append("] in framework [").append(frkIndex).append("]");
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
