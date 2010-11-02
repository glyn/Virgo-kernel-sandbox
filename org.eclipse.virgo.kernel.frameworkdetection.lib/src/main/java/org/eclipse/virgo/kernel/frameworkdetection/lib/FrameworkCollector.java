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

package org.eclipse.virgo.kernel.frameworkdetection.lib;

import java.util.GregorianCalendar;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Here all current frameworks are registered when started or unregistered when stopped.
 * The Detection agent puts this class to the boot-class-path so that it is accessible from everywhere with the proper bootdelegation setting.
 * @author Borislav Kapukaranov (borislav.kapukaranov@gmail.com)
 */
public class FrameworkCollector {
  private static long id = 0;
  private static ConcurrentHashMap<String, FrameworkData> frameworks = new ConcurrentHashMap<String, FrameworkData>();

  /**
   * Way to obtain all frameworks
   * @return all frameworks registered so far with their FrameworkData objects
   */
  public static ConcurrentHashMap<String, FrameworkData> getFrameworks() {
    return frameworks;
  }
  
  /**
   * Used by the FrameworkEvent class to register a started framework
   * @param framewokrState - the framework's state - usually STARTED
   * @param key - the frameworks key - the hashcode of its system bundle
   * @param frkBundle - the framework system bundle
   * @param origin - the StackTraceElement[] that contains the start call stack
   */
  public static void addFramework(String framewokrState, String key, Object frkBundle, StackTraceElement[] origin) {
    if (frameworks.containsKey(key)) {
      FrameworkData lastState = frameworks.remove(key);
      frameworks.put(key, new FrameworkData(framewokrState, frkBundle, GregorianCalendar.getInstance().getTime().toString(), origin, ++id));
    } else {
      frameworks.put(key, new FrameworkData(framewokrState, frkBundle, GregorianCalendar.getInstance().getTime().toString(), origin, ++id));
    }
  }
  
  /**
   * Get a framework by its unique id.
   * @param id - the frameworks id
   * @return the FrameworkData object for the target framework
   */
  public static FrameworkData getFrameworkByID(long id) {
    for (FrameworkData info : frameworks.values()) {
      if (info.getID() == id) {
        return info;
      }
    }
    return null;
  }
  
  /**
   * A way to remove a framework - used by the instrumented FrameworkEvent class to unregister stopped frameworks
   * @param key - the key of the framework to be removed
   */
  public static void removeFramework(String key) {
    frameworks.remove(key);
  }
 
}
