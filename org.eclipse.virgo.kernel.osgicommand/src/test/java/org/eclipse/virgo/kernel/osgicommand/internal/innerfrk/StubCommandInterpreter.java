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

import org.eclipse.osgi.framework.console.CommandInterpreter;
import org.osgi.framework.Bundle;

import java.util.Dictionary;

public class StubCommandInterpreter implements CommandInterpreter {

    private static final String NEW_LINE = System.getProperty("line.separator");

    private StringBuffer commandOutput = new StringBuffer("");
    private String[] arguments;
    private int index;
    private String nxtArgument;

    public StubCommandInterpreter(String... args) {
        this.arguments = args;
    }

    public String nextArgument() {
        if (arguments.length == 1) {
            nxtArgument = arguments[index];
        } else {
            nxtArgument = arguments[index];
            ++index;
        }
        return nxtArgument;
    }

    public void println() {
        commandOutput.append(NEW_LINE);
    }

    public void println(Object o) {
        commandOutput.append(nxtArgument).append(NEW_LINE).append(o);
    }

    public StringBuffer getCommandOutput() {
        return commandOutput;
    }

    public Object execute(String cmd) {
        return null;
    }

    public void print(Object o) {
        commandOutput.append(nxtArgument).append(o);
    }

    public void printStackTrace(Throwable t) {
        commandOutput.append(t.getStackTrace());
    }

    public void printDictionary(Dictionary dic, String title) {
        commandOutput.append(title).append(dic.toString());
    }

    public void printBundleResource(Bundle bundle, String resource) {
        commandOutput.append(bundle.toString()).append(NEW_LINE).append(resource);
    }

}