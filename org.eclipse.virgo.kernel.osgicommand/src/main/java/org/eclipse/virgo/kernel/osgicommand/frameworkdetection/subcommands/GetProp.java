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

import java.util.Properties;

/**
 * Handles the list operations of properties for a target frameworks.
 * If it is Equinox framework all properties will be listed,
 * for other OSGI frameworks only a subset of OSGI properties will be listed
 */
public class GetProp implements SubCommandExecutor {
    private final String NEW_LINE = FrameworkInfoUtils.NEW_LINE;

    public String execute(long frkIndex, CommandInterpreter interpreter) throws Exception {
        StringBuilder output = new StringBuilder();
        output.append("Listing all properties for framework [").append(frkIndex).append("]..").append(NEW_LINE);
        output.append("----------------------------------------------------------");
        output.append(NEW_LINE);
        FrameworkData info = FrameworkCollector.getFrameworkByID(frkIndex);
        Object bc = null;
        try {
            bc = FrameworkInfoUtils.getBundleContext(info.getBundle());
            Properties props = (Properties) FrameworkInfoUtils.getEquinoxProperties(bc.getClass().getClassLoader());
            for (Object key : props.keySet()) {
                output.append(key).append(" = ").append(FrameworkInfoUtils.getProperty(bc, (String) key)).append(NEW_LINE);
            }
        } catch (ClassNotFoundException e) {
            output = new StringBuilder();
            output.append("This is not an Equinox framework. Only standard OSGi properties will be displayed").append(NEW_LINE);
            output.append(FrameworkInfoUtils.getOSGiProps(frkIndex, bc));
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
