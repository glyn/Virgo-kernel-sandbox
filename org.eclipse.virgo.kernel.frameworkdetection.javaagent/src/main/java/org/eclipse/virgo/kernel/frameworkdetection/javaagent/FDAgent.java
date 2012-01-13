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

package org.eclipse.virgo.kernel.frameworkdetection.javaagent;

import javassist.*;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

/**
 * This Agent instruments the FrameworkEvent osgi class, thus enabling all osgi framework to be collected in a storage.
 * It bring minimal overhead since it instruments only one class and all others are immediately rejected.
 * @author Borislav Kapukaranov (borislav.kapukaranov@sap.com)
 */

public class FDAgent implements ClassFileTransformer {

  private String key = "String.valueOf(bundle.hashCode())";
  
  private static final String EVENT_CLASSNAME = "org.osgi.framework.FrameworkEvent";
  String[] accept = new String[] { "org/osgi/framework/FrameworkEvent" };

  public static void premain(String agentArgument, Instrumentation instrumentation) {
    // register agent
    instrumentation.addTransformer(new FDAgent());
  }

  public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {

    for (int i = 0; i < accept.length; i++) {
      if (!className.startsWith(accept[i])) {
        return null;
      }
    }
    return processClass(className, classfileBuffer);
  }

  private byte[] processClass(String className, byte[] classfileBuffer) {
    ClassPool pool = ClassPool.getDefault();
    String path = System.getProperty("osgi.framework");
    if (path != null) {
      try {
        pool.insertClassPath(path.substring(5));
      } catch (NotFoundException e) {
        e.printStackTrace();
      }
    }    
    CtClass cl = null;
    try {
      cl = pool.makeClass(new java.io.ByteArrayInputStream(classfileBuffer));
      classfileBuffer = null;
      if (cl.isInterface() == false && cl.getName().equals(EVENT_CLASSNAME)) {
        CtConstructor[] constructors = cl.getConstructors();
        for (CtConstructor constructor : constructors) {
          // obtain framework info

          for (CtField field : cl.getFields()) {
            if (field.getName().equals("STARTED"))
              constructor.insertAfter("if (type == this.STARTED) org.eclipse.virgo.kernel.frameworkdetection.lib.FrameworkCollector.addFramework(\"Started\", " + key + ", bundle, Thread.currentThread().getStackTrace());");
            if (field.getName().equals("STOPPED"))
              constructor.insertAfter("if (type == this.STOPPED) org.eclipse.virgo.kernel.frameworkdetection.lib.FrameworkCollector.removeFramework(" + key + ");");
            if (field.getName().equals("STOPPED_UPDATE"))
              constructor.insertAfter("if (type == this.STOPPED_UPDATE) org.eclipse.virgo.kernel.frameworkdetection.lib.FrameworkCollector.addFramework(\"Stopped for Update\", " + key + ", bundle, Thread.currentThread().getStackTrace());");
            if (field.getName().equals("STOPPED_BOOTCLASSPATH_MODIFIED"))
              constructor.insertAfter("if (type == this.STOPPED_BOOTCLASSPATH_MODIFIED) org.eclipse.virgo.kernel.frameworkdetection.lib.FrameworkCollector.addFramework(\"Stopped for BootClassPath refresh\", " + key + ", bundle, Thread.currentThread().getStackTrace());");
          }
        }
        classfileBuffer = cl.toBytecode();
      }
    } catch (Exception e) {
      System.err.println("Could not instrument  " + className + ",  exception : " + e.getMessage());
      e.printStackTrace();
    } finally {
      if (cl != null) {
        cl.detach();
      }
    }
    return classfileBuffer;
  }
}
