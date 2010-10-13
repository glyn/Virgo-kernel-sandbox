package org.eclipse.virgo.kernel.osgicommand.frameworkdetection;

import org.eclipse.virgo.kernel.osgicommand.frameworkdetection.helper.FrameworkInfoHelper;
import org.eclipse.virgo.kernel.frameworkdetection.lib.FrameworkCollector;
import org.eclipse.virgo.kernel.frameworkdetection.lib.FrameworkData;
import org.eclipse.osgi.framework.console.CommandInterpreter;
import org.eclipse.osgi.framework.console.CommandProvider;
import org.osgi.framework.BundleContext;

import java.util.concurrent.ConcurrentHashMap;

/**
 * The entry point for the Inner Framework Detection commands
 * 
 * @author Borislav Kapukaranov (borislav.kapukaranov@sap.com)
 * @version 1.0
 */

public class FrameworkInfoCommand implements CommandProvider {

  public static String NEW_LINE = FrameworkInfoHelper.NEW_LINE;
	private BundleContext context = null;
	
	/**
	 * Used only for test purposes
	 */
	public FrameworkInfoCommand() {
	}

	/**
	 * The constructor that is used by the commands bundle activator
	 * @param context
	 */
	public FrameworkInfoCommand(BundleContext context) {
		this.context = context;
	}

	/**
	 * The command method. the entry point for inner framework detection commands execution
	 * @param interpreter
	 * @return
	 */
	public Object _frk(CommandInterpreter interpreter) {
		ConcurrentHashMap<String, FrameworkData> frameworks = FrameworkCollector.getFrameworks();
		String arg = interpreter.nextArgument();
		// marks if a command is executed
		boolean cmd_executed = false;
		if (arg != null) {
			// add here the direct frk arguments
			if (arg.equals("-details") || arg.equals("-d")) {
				interpreter.println(FrameworkInfoHelper.list(frameworks, arg, context));
				cmd_executed = true;
			}
			try {
				// add here frk subcommands for the inner frameworks
				long index = Long.valueOf(arg);
				arg = interpreter.nextArgument();
				if (arg != null) {
					if (arg.equals("listbundles") || arg.equals("ss")) {
						interpreter.println(FrameworkInfoHelper.listBundles(index));
						cmd_executed = true;
					}
					if (arg.equals("startbundle") || arg.equals("start")) {
						try {
							String temp = interpreter.nextArgument();
							long bndIndex = Long.valueOf(temp);
							interpreter.println(FrameworkInfoHelper.startBundle(index, bndIndex));
							cmd_executed = true;
						} catch (NumberFormatException e) {
							interpreter.println("Wrong bundle index, try again with correct input.");
						}
					}
					if (arg.equals("updatebundle") || arg.equals("update")) {
						try {
							String temp = interpreter.nextArgument();
							long bndIndex = Long.valueOf(temp);
							interpreter.println(FrameworkInfoHelper.updateBundle(index, bndIndex));
							cmd_executed = true;
						} catch (NumberFormatException e) {
							interpreter.println("Wrong bundle index, try again with correct input.");
						}
					}
					if (arg.equals("stopbundle") || arg.equals("stop")) {
						try {
							String temp = interpreter.nextArgument();
							long bndIndex = Long.valueOf(temp);
							interpreter.println(FrameworkInfoHelper.stopBundle(index, bndIndex));
							cmd_executed = true;
						} catch (NumberFormatException e) {
							interpreter.println("Wrong bundle index, try again with correct input.");
						}
					}
					if (arg.equals("installbundle") || arg.equals("install")) {
						String temp = interpreter.nextArgument();
						interpreter.println(FrameworkInfoHelper.installBundle(index, temp));
						cmd_executed = true;
					}
					if (arg.equals("uninstallbundle") || arg.equals("uninstall")) {
						try {
							String temp = interpreter.nextArgument();
							long bndIndex = Long.valueOf(temp);
							interpreter.println(FrameworkInfoHelper.uninstallBundle(index, bndIndex));
							cmd_executed = true;
						} catch (NumberFormatException e) {
							interpreter.println("Wrong bundle index, try again with correct input.");
						}
					}
					if (arg.equals("getproperties") || arg.equals("getprop")) {
						interpreter.println(FrameworkInfoHelper.listProperties(index));
						cmd_executed = true;
					}
					if (arg.equals("checkvisibility") || arg.equals("visibility")) {
						interpreter.println(FrameworkInfoHelper.checkVisibility(index));
						cmd_executed = true;
					}
				} else {
					interpreter.println("Missing subcommand after framework index");
					interpreter.println(getHelp());
					cmd_executed = true;
				}
				if (!cmd_executed) {
					interpreter.println("Incorrect input: subcommand [" + arg + "] not recognized");
					interpreter.println(getHelp());
				}
			} catch (NumberFormatException e) {
				if (!cmd_executed) {
					interpreter.println("Wrong index or argument [" + arg + "], try again with correct input.");
					interpreter.println(getHelp());
				}
			}
		} else {
			interpreter.println(FrameworkInfoHelper.list(frameworks, arg, context));
		}
		return null;
	}

	/**
	 * The command's help 
	 */
	public String getHelp() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("---Framework Info commands---").append(FrameworkInfoHelper.NEW_LINE);
		buffer.append("\tfrk [-d or -details] - Lists all current frameworks in a tree view with ot without details").append(FrameworkInfoHelper.NEW_LINE);
		buffer.append("\tfrk [<framework index>] [<subcommand>] - executes the subcommand on the specified framework").append(FrameworkInfoHelper.NEW_LINE).append(FrameworkInfoHelper.NEW_LINE);
		buffer.append("\tsubcommands:").append(FrameworkInfoHelper.NEW_LINE);
		buffer.append("\tss - lists all bundles with their state and id").append(FrameworkInfoHelper.NEW_LINE);
		buffer.append("\tgetprop - lists all configuration properties for the specified framework").append(FrameworkInfoHelper.NEW_LINE);
		buffer.append("\tvisibility - lists the resources of the current system bundle loader,its parent loader and the bootdelegation and parentClassloader modes").append(FrameworkInfoHelper.NEW_LINE);
		buffer.append("\tinstall <path to bundle jar> - installs that bundle in the specified framework").append(FrameworkInfoHelper.NEW_LINE);
		buffer.append("\tuninstall <bundle ID> - uninstalls that bundle in the specified framework").append(FrameworkInfoHelper.NEW_LINE);
		buffer.append("\tstart <bundle ID> - starts that bundle in the specified framework").append(FrameworkInfoHelper.NEW_LINE);
		buffer.append("\tstop <bundle ID> - stops that bundle in the specified framework").append(FrameworkInfoHelper.NEW_LINE);
		buffer.append("\tupdate <bundle ID> - updates that bundle in the specified framework").append(FrameworkInfoHelper.NEW_LINE);
		return buffer.toString();
	}

}