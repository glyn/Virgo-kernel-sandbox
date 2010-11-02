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

/**
 * All needed for analysis data for a framework is gathered in FrameworkData objects - this is the normal way to store and access it.
 * @author Borislav Kapukaranov (borislav.kapukaranov@sap.com)
 */

public class FrameworkData {
  private String frameworkState;
  private Object bundle;
  private String date;
  private StackTraceElement[] origin;
  private long id;
  
  /**
   * Constructor for the storing framework data type.
   * @param frameworkState - the state in which the framework is, represented by the actual framework event
   * @param frkBundle - the framework's system bundle
   * @param date - the date of the last modification to the framework's system bundle
   * @param origin - the stack trace of the call that performed the last changes to the framework's system bundle
   * @param id - the id with which the framework is saved internally
   */
  public FrameworkData(String frameworkState, Object frkBundle, String date, StackTraceElement[] origin, long id) {
    this.frameworkState = frameworkState;
    this.bundle = frkBundle;
    this.date = date;
    this.origin = origin;
    this.id = id;
  }
  
  public String getFrameworkState() {
    return frameworkState;
  }

  public Object getBundle() {
    return bundle;
  }

  public String getDate() {
    return date;
  }

  public StackTraceElement[] getOrigin() {
    return origin;
  }
  
  public void setID(long id) {
	this.id = id;
  }
  
  public long getID() {
    return id;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!(obj instanceof FrameworkData)) return false;
    
    FrameworkData temp = (FrameworkData) obj;
    
    return this.bundle.equals(temp.bundle) &&
           this.frameworkState == temp.frameworkState &&
           this.date.equals(temp.date) &&
           this.origin.equals(temp.origin);
  }

  @Override
  public String toString() {
    String result = "OSGi framework with state [" + frameworkState + "] and ID[" + id + "]\n" +
    		"- system bundle classname: [" + bundle.getClass().getName() + "]\n" +
    		"- last changed on [" + date + "]\n" +
    		"- ownLoader: [" + bundle.getClass().getClassLoader() + "]\n";
    for (StackTraceElement el : origin) {
      result += el.toString() + "\n";
    }
    return result;
  }
  
  public String toString(boolean arg) { 
    String result = "\nOSGi framework with state [" + frameworkState + "] and ID[" + id + "]\n" +
        "- system bundle classname: [" + bundle.getClass().getName() + "]\n" +
        "- last changed on [" + date + "]\n" +
        "- ownLoader: [" + bundle.getClass().getClassLoader() + "]\n";
    if (arg) {
      result += "Call stack:\n";
      for (StackTraceElement el : origin) {
        result += el.toString() + "\n";
      }
    }
    return result;
  }

  
  
}
