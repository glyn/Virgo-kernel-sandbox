package org.eclipse.virgo.kernel.osgicommand.frameworkdetection;
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


import org.eclipse.osgi.framework.console.CommandInterpreter;
import org.eclipse.osgi.framework.console.CommandProvider;
import org.eclipse.virgo.kernel.frameworkdetection.lib.FrameworkCollector;
import org.eclipse.virgo.kernel.frameworkdetection.lib.FrameworkData;
import org.eclipse.virgo.kernel.osgicommand.frameworkdetection.subcommands.*;
import org.osgi.framework.BundleContext;

import java.util.Hashtable;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The entry point for the Inner Framework Detection commands
 *
 * @author Borislav Kapukaranov (borislav.kapukaranov@sap.com)
 */

public class FrameworkInfoCommand implements CommandProvider {

    public static String NEW_LINE = FrameworkInfoUtils.NEW_LINE;
    private BundleContext context = null;

    /**
     * Used only for test purposes
     */
    public FrameworkInfoCommand() {
    }

    /**
     * The constructor that is used by the commands bundle activator
     *
     * @param context
     */
    public FrameworkInfoCommand(BundleContext context) {
        this.context = context;
    }

    public void initCmdExecutors(Hashtable<String, CommandExecutor> executors, CommandInterpreter interpreter) {
        executors.put("frk", new GetFrameworksDetails(context, interpreter, false));
        executors.put("-d", new GetFrameworksDetails(context, interpreter, true));
    }

    public void initSubCmdExecutors(Hashtable<String, SubCommandExecutor> executors) {
        executors.put("install", new InstallBundle());
        executors.put("uninstall", new UninstallBundle());
        executors.put("start", new StartBundle());
        executors.put("stop", new StopBundle());
        executors.put("update", new UpdateBundle());
        executors.put("ss", new ListBundles());
        executors.put("getprop", new GetProp());
        executors.put("visibility", new CheckVisibility());
    }

    /**
     * The command method. the entry point for inner framework detection commands execution
     *
     * @param interpreter
     * @return
     */
    public void _frk(CommandInterpreter interpreter) {
        try {
            ConcurrentHashMap<String, FrameworkData> frameworks = FrameworkCollector.getFrameworks();
        } catch (NoClassDefFoundError err) {
            interpreter.println("The FrameworkInfo Supportability commands are not activated.");
            interpreter.println("To activate them start Virgo with '-frk' option");
            return;
        }

        String arg = interpreter.nextArgument();

        Hashtable<String, CommandExecutor> cmdExecutors = new Hashtable<String, CommandExecutor>();
        Hashtable<String, SubCommandExecutor> subCmdExecutors = new Hashtable<String, SubCommandExecutor>();

        //init the command executors and subcommand executors
        initCmdExecutors(cmdExecutors, interpreter);
        initSubCmdExecutors(subCmdExecutors);
        try {
            if (arg != null) {
                if (arg.equals("-d")) {
                    String out = cmdExecutors.get(arg).execute();
                    interpreter.println(out);
                } else {
                    long frkIndex = 0;
                    try {
                        frkIndex = Long.valueOf(arg);
                    } catch (NumberFormatException nfe) {
                        interpreter.println("Wrong index or argument [" + arg + "], try again with correct input");
                        interpreter.println(getHelp());
                        return;
                    }

                    String operation = interpreter.nextArgument();

                    if (operation == null) {
                        interpreter.println("Missing subcommand after framework index");
                        interpreter.println(getHelp());
                        return;
                    }

                    SubCommandExecutor exec = subCmdExecutors.get(operation);

                    if (exec == null) {
                        interpreter.println("Incorrect input: subcommand [" + arg + "] not recognized");
                        interpreter.println(getHelp());
                        return;
                    }

                    String out = exec.execute(frkIndex, interpreter);
                    interpreter.println(out);

                }
            } else {
                String out = cmdExecutors.get("frk").execute();
                interpreter.println(out);
            }
        } catch (Exception e) {
            interpreter.println("An error occured for unknown reason:");
            interpreter.printStackTrace(e);
        }
    }

    /**
     * The command's help
     */
    public String getHelp() {
        StringBuffer buffer = new StringBuffer();
        buffer.append("---Framework Info commands---").append(NEW_LINE);
        buffer.append("\tfrk [-d] - Lists all current frameworks in a tree view with ot without details").append(NEW_LINE);
        buffer.append("\tfrk [<framework index>] [<subcommand>] - executes the subcommand on the specified framework").append(NEW_LINE).append(NEW_LINE);
        buffer.append("\tsubcommands:").append(NEW_LINE);
        buffer.append("\tss - lists all bundles with their state and id").append(NEW_LINE);
        buffer.append("\tgetprop - lists all configuration properties for the specified framework").append(NEW_LINE);
        buffer.append("\tvisibility - lists the resources of the current system bundle loader,its parent loader and the bootdelegation and parentClassloader modes").append(NEW_LINE);
        buffer.append("\tinstall <path to bundle jar> - installs that bundle in the specified framework").append(NEW_LINE);
        buffer.append("\tuninstall <bundle ID> - uninstalls that bundle in the specified framework").append(NEW_LINE);
        buffer.append("\tstart <bundle ID> - starts that bundle in the specified framework").append(NEW_LINE);
        buffer.append("\tstop <bundle ID> - stops that bundle in the specified framework").append(NEW_LINE);
        buffer.append("\tupdate <bundle ID> - updates that bundle in the specified framework").append(NEW_LINE);
        buffer.append("\t" + NEW_LINE + "Additionally after each subcommand '-debug' can be included. This will enable detailed tracing where available when the executed command fails.").append(NEW_LINE);
        return buffer.toString();
    }

}