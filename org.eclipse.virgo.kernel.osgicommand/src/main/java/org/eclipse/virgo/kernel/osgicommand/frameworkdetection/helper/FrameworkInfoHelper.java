package org.eclipse.virgo.kernel.osgicommand.frameworkdetection.helper;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.core.runtime.internal.adaptor.URLConverterImpl;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import org.eclipse.virgo.kernel.frameworkdetection.lib.FrameworkCollector;
import org.eclipse.virgo.kernel.frameworkdetection.lib.FrameworkData;

/**
 * This is a helper class needed for the operation of the Inner Frameworks Detection commands
 * 
 * @author Borislav Kapukaranov (borislav.kapukaranov@sap.com)
 * @version 1.0
 */
public class FrameworkInfoHelper {
  
  public static int fixedIndex = -1;
  public static final String NEW_LINE = "\r\n";
  public static final String START_LIST_HEADER = "Current state of the OSGi Frameworks:" + NEW_LINE;
  // some default osgi properties
  public static final String[] osgiProps = {
      "org.osgi.framework.system.packages",
      "org.osgi.framework.bootdelegation",
      "osgi.parentClassloader",
      "osgi.install.area",
      "osgi.configuration.area",
      "osgi.sharedConfiguration.area",
      "osgi.instance.area",
      "osgi.instance.area.default",
      "osgi.user.area",
      "osgi.user.area.default",
      "osgi.manifest.cache",
      "user.home",
      "user.dir",
      "osgi.noShutdown",
      "osgi.compatibility.bootdelegation",
      "org.osgi.framework.vendor",
      "osgi.bundlefile.limit",
      "osgi.logfile",
      "osgi.framework.extensions",
      "osgi.frameworkClassPath"
  };
 
  /**
   * The method lists a tree output of the current framework structure. After every change the user has to invoke that method 
   * so that the structure is updated with the newest frameworks and their indexes. The framework indexes are NOT CONSTANT 
   * once they are registered, so every on every change of the structure the tree must be refreshed with a list
   * @param frameworks - the current framework repository
   * @param arg - the command argument specified by the user
   * @param context - the bundle context of the console environment from where the use operates
   * @return formatted string with the current tree structure
   */
  public static String list(ConcurrentHashMap<String, FrameworkData> frameworks, String arg, BundleContext context) {
    StringBuilder output = new StringBuilder();
    output.append(START_LIST_HEADER);
    output.append("Avaliable frameworks ").append(getFrameworksCount()).append(NEW_LINE);
    output.append("----------------------------------------------------------");
    output.append(NEW_LINE);
    FrameworkData farOuterFramework = null;
    try {
      ArrayList<Long> passed = new ArrayList<Long>();
      Hashtable<Long, Object> frk_origin_map = new Hashtable<Long, Object>();
      Hashtable<Long, ArrayList<Long>> relations = new Hashtable<Long, ArrayList<Long>>();
      ArrayList<Long> currentLevel = new ArrayList<Long>();
      boolean isOuterFrkDiscovered = false;
      for (FrameworkData info : frameworks.values()) {
        for (Object b : context.getBundles()) {
          // find the outer framework
          // if all stack trace elements can  be loaded by some bundle then this is the outer framework and that bundle is its origin
          // commonly this is the system bundle (org.eclipse.osgi)
          StackTraceElement[] se = info.getOrigin();
          boolean loadall = true;
          for (StackTraceElement aSe : se) {
            try {
              loadClass(b, aSe.getClassName());
            } catch (InvocationTargetException e) {
              // class found, save & exit..
              loadall = false;
              break;
            }
          }
          if (loadall && (Long) getBundleId(b) == 0) {
            // all stack trace elements are loaded, store the outer framework and mark it as passed
            farOuterFramework = info;
            passed.add(info.getID());
            currentLevel.add(farOuterFramework.getID());
            frk_origin_map.put(farOuterFramework.getID(), b);
            isOuterFrkDiscovered = true;
            break;
          }
        }
        if (isOuterFrkDiscovered)
          break;
      }
      if (farOuterFramework == null) {
        output = new StringBuilder();
        output.append("Error while building the tree structure. Outer framework could not be determined.");
        return output.toString();
      }
      
      // process all frameworks to determine their relations
      for (FrameworkData current : frameworks.values()) {
        // here we will store the inner frameworks of the current level
        currentLevel = new ArrayList<Long>();
        for (FrameworkData inner : frameworks.values()) {
          // check if we already looked at this framework and it is not the current one we are processing
          if (!passed.contains(inner.getID()) && inner.getID() != current.getID()) {
            Object bc = getBundleContext(current.getBundle());
            Object outerSystemBundle = getBundle(bc, 0);
            StackTraceElement[] se = inner.getOrigin();
            String className = se[0].getClassName();
            // find where the classloader changes
            for (StackTraceElement aSe : se) {
              try {
                loadClass(outerSystemBundle, aSe.getClassName());
                className = aSe.getClassName();
              } catch (InvocationTargetException e) {
                // class name found, save it & exit..
                className = aSe.getClassName();
                break;
              }
            }

            // now we got the className and we'll try to see which bundle can load it to determine the possible origin bundles
            // sometimes there may be more than one possible origin bundle
            for (Object b : getBundles(bc)) {
              if (getResource(b, className.replace(".", "/") + ".class") != null) {
                frk_origin_map.put(inner.getID(), b);
                currentLevel.add(inner.getID());
                passed.add(inner.getID());
              }
            }
          }
        }
        // Add the ID no matter if it has children or not. This way we can iterate without checks for null and the structure is more consistent:
        // 1 -> [2]
        // 2 -> [empty array]
        relations.put(current.getID(), currentLevel);
      }

      // output tree
      fixedIndex = -1;
      // create a preliminary framework tree
      String tempTree = processTree(farOuterFramework.getID(), "\\___", relations, frk_origin_map);
      fixedIndex = -1;
      // normalize the framework indexes so that all are positive numbers and are in ascending order
      normalizeFrameworkIds();
      //remove the " \\___" for the root level
      output.append(tempTree.substring(4));
      output.append(NEW_LINE);

      // check if further data has to be displayed
      if (arg != null && (arg.equals("-d") || arg.equals("details"))) {
        for (FrameworkData info : frameworks.values()) {
          if (context != null) {
            // detailed info in list
            output.append(NEW_LINE).append("[ ").append(info.getID()).append(" ] - [").append(info.getFrameworkState()).append("]").append(NEW_LINE);
            output.append("- system bundle classname: [").append(info.getBundle().getClass().getName()).append("]").append(NEW_LINE);
            Bundle systemBundle = context.getBundle(0);
            StackTraceElement[] se = info.getOrigin();
            String className = se[0].getClassName();
            boolean loadedAll = true;
            // find where the classloader changes
            for (StackTraceElement aSe : se) {
              try {
                systemBundle.loadClass(aSe.getClassName());
                className = aSe.getClassName();
              } catch (ClassNotFoundException e) {
                // class found, save & exit..
                className = aSe.getClassName();
                loadedAll = false;
                break;
              }
            }
            ArrayList<Bundle> originBundles = new ArrayList<Bundle>();
            if (loadedAll) {
              originBundles.add(context.getBundle(0));
            } else {
              for (Bundle b : context.getBundles()) {
                if (getResource(b, className.replace(".", "/") + ".class") != null) {
                  originBundles.add(b);
                }
              }
            }
            if (originBundles.size() > 0) {
              for (Bundle originBundle : originBundles) {
                output.append("- possibly started from bundle: [").append(originBundle.getSymbolicName()).append("_").append(originBundle.getVersion()).append("]").append(NEW_LINE);
              }
            } else {
              output.append("- possibly started from bundle: [ERROR: could not determine the origin bundle]").append(NEW_LINE);
            }

            output.append("- on [").append(info.getDate()).append("]").append(NEW_LINE);
            if (info.getBundle().getClass().getClassLoader() != null) {
              output.append("- inner frk sys.bundle loader: [").append(info.getBundle().getClass().getClassLoader()).append("]").append(NEW_LINE);
            } else {
              output.append("- inner frk sys.bundle loader: [ data not available ]").append(NEW_LINE);
            }
            if (info.getBundle().getClass().getClassLoader().getParent() != null) {
              output.append("- inner frk sys.bundle parent loader: [").append(info.getBundle().getClass().getClassLoader().getParent()).append("]").append(NEW_LINE);
            } else {
              output.append("- inner frk sys.bundle parent loader: [ data not available ]").append(NEW_LINE);
            }

            output.append("Startup Call stack:").append(NEW_LINE);
            for (StackTraceElement el : info.getOrigin()) {
              output.append(el.toString()).append("").append(NEW_LINE);
            }

          } else {
            // use the default toString with no origin bundle found
            output.append(info.toString(arg));
          }
        }
      }
      return output.toString();
    } catch (SecurityException e) {
      e.printStackTrace();
    } catch (IllegalArgumentException e) {
      e.printStackTrace();
    } catch (NoSuchMethodException e) {
      e.printStackTrace();
    } catch (IllegalAccessException e) {
      e.printStackTrace();
    } catch (InvocationTargetException e) {
      e.printStackTrace();
    }
    return null;
  }
  
  /**
   * Installs a bundle in a specified inner/nested framework 
   * @param frkIndex - the framework where the bundle will be installed
   * @param url - the url to the target bundle, it can be passed without the "file:" prefix
   * @return formatted string with the output of the operation
   */
  public static String installBundle(long frkIndex, String url) {
    StringBuilder output = new StringBuilder();
    FrameworkData info = FrameworkCollector.getFrameworkByID(frkIndex);
    output.append("Installing bundle [").append(url).append("] in framework [").append(frkIndex).append("]..").append(NEW_LINE);
    if (info == null) {
      output.append("ERROR: Inner framework [").append(frkIndex).append("] is not available, check if it is stopped").append(NEW_LINE);
      return output.toString();
    }
    try {
      Object innerCtx = getBundleContext(info.getBundle());
      String bndUrl = url;
      if (!url.startsWith("file:")) {
        bndUrl = "file:" + url;
      }
      Object installedBundle = install(innerCtx, bndUrl);
      output.append("Bundle [").append(getSymbolicName(installedBundle)).append("_").append(getVersion(installedBundle)).append("] installed successfuly with id [").append(getBundleId(installedBundle)).append("]").append(NEW_LINE);
    } catch (IllegalStateException ise) {
      output.append("ERROR: Bundle installation failed:").append(NEW_LINE);
      ise.printStackTrace();
    } catch (SecurityException se) {
      output.append("ERROR: Bundle installation failed:").append(NEW_LINE);
      se.printStackTrace();
    } catch (IllegalArgumentException e) {
      output.append("ERROR: Bundle installation failed:").append(NEW_LINE);
      e.printStackTrace();
    } catch (IllegalAccessException e) {
      output.append("ERROR: Bundle installation failed:").append(NEW_LINE);
      e.printStackTrace();
    } catch (InvocationTargetException e) {
      output.append("ERROR: Bundle installation failed:").append(NEW_LINE);
      e.printStackTrace();
    } catch (NoSuchMethodException e) {
      output.append("ERROR: Bundle installation failed:").append(NEW_LINE);
      e.printStackTrace();
    }
    return output.toString();
  }
  
  /**
   * The method starts an install bundle in the target framework
   * @param frkIndex - the index of the target framework
   * @param bndIndex - the bundle index that will be started
   * @return formatted string with the result of the operation
   */
  public static String startBundle(long frkIndex, long bndIndex) {
    StringBuilder output = new StringBuilder();
    FrameworkData info = FrameworkCollector.getFrameworkByID(frkIndex);
    output.append("Starting bundle [").append(bndIndex).append("] in framework [").append(frkIndex).append("]..").append(NEW_LINE);
    if (info == null) {
      output.append("ERROR: Inner framework [").append(frkIndex).append("] is not available, check if it is stopped").append(NEW_LINE);
      return output.toString();
    }
    try {
      Object innerCtx = getBundleContext(info.getBundle());
      Object bnd = getBundle(innerCtx, bndIndex);
      if (bnd != null) {
        if (getState(bnd) != Bundle.ACTIVE) {
          start(bnd);
          output.append("Bundle [").append(getSymbolicName(bnd)).append("_").append(getVersion(bnd)).append("] started successfully in framework [").append(frkIndex).append("]!").append(NEW_LINE);
        } else {
          output.append("Bundle [").append(getSymbolicName(bnd)).append("_").append(getVersion(bnd)).append("] already started in framework [").append(frkIndex).append("]!").append(NEW_LINE);
        }
      } else {
        output.append("ERROR: Cannot find bundle [").append(bndIndex).append("] in framework [").append(frkIndex).append("]");
      }
    } catch (IllegalStateException ise) {
      ise.printStackTrace();
    } catch (SecurityException se) {
      se.printStackTrace();
    } catch (IllegalArgumentException e) {
      e.printStackTrace();
    } catch (NoSuchMethodException e) {
      e.printStackTrace();
    } catch (IllegalAccessException e) {
      e.printStackTrace();
    } catch (InvocationTargetException e) {
      e.printStackTrace();
    }
    return output.toString();
  }
  
  /**
   * Stops bundle in a target framework
   * @param frkIndex - the target framework's index
   * @param bndIndex - the target bundle's index
   * @return the result from the operation
   */
  public static String stopBundle(long frkIndex, long bndIndex) {
    StringBuilder output = new StringBuilder();
    FrameworkData info = FrameworkCollector.getFrameworkByID(frkIndex);
    output.append("Stopping bundle [").append(bndIndex).append("] in framework [").append(frkIndex).append("]..").append(NEW_LINE);
    if (info == null) {
      output.append("ERROR: Inner framework [").append(frkIndex).append("] is not available, check if it is stopped").append(NEW_LINE);
      return output.toString();
    }
    try {
      Object innerCtx = getBundleContext(info.getBundle());
      Object bnd = getBundle(innerCtx, bndIndex);
      if (bnd != null) {
        if (getState(bnd) != Bundle.RESOLVED && getState(bnd) != Bundle.INSTALLED) {
          stop(bnd);
          output.append("Bundle [").append(getSymbolicName(bnd)).append("_").append(getVersion(bnd)).append("] stopped successfully in framework [").append(frkIndex).append("]!").append(NEW_LINE);
        } else {
          output.append("Bundle [").append(getSymbolicName(bnd)).append("_").append(getVersion(bnd)).append("] already stopped in framework [").append(frkIndex).append("]!").append(NEW_LINE);
        }
      } else {
        output.append("ERROR: Cannot find bundle [").append(bndIndex).append("] in framework [").append(frkIndex).append("]");
      }
    } catch (IllegalStateException ise) {
      ise.printStackTrace();
    } catch (SecurityException se) {
      se.printStackTrace();
    } catch (IllegalArgumentException e) {
      e.printStackTrace();
    } catch (NoSuchMethodException e) {
      e.printStackTrace();
    } catch (IllegalAccessException e) {
      e.printStackTrace();
    } catch (InvocationTargetException e) {
      e.printStackTrace();
    }
    return output.toString();
  }
  /**
   * Uninstalls a bundle in a target framework
   * @param frkIndex - the target framework
   * @param bndIndex - the bundle index that will be uninstalled
   * @return the result from the operation
   */
  public static String uninstallBundle(long frkIndex, long bndIndex) {
    StringBuilder output = new StringBuilder();
    FrameworkData info = FrameworkCollector.getFrameworkByID(frkIndex);
    output.append("Uninstalling bundle [").append(bndIndex).append("] in framework [").append(frkIndex).append("]..").append(NEW_LINE);
    if (info == null) {
      output.append("ERROR: Inner framework [").append(frkIndex).append("] is not available, check if it is stopped").append(NEW_LINE);
      return output.toString();
    }
    try {
      Object innerCtx = getBundleContext(info.getBundle());
      Object bnd = getBundle(innerCtx, bndIndex);
      if (bnd != null) {
        uninstall(bnd);
        output.append("Bundle [").append(getSymbolicName(bnd)).append("_").append(getVersion(bnd)).append("] uninstalled successfully in framework [").append(frkIndex).append("]!").append(NEW_LINE);
      } else {
        output.append("ERROR: Cannot find bundle [").append(bndIndex).append("] in framework [").append(frkIndex).append("]");
      }
    } catch (IllegalStateException ise) {
      ise.printStackTrace();
    } catch (SecurityException se) {
      se.printStackTrace();
    } catch (IllegalArgumentException e) {
      e.printStackTrace();
    } catch (NoSuchMethodException e) {
      e.printStackTrace();
    } catch (IllegalAccessException e) {
      e.printStackTrace();
    } catch (InvocationTargetException e) {
      e.printStackTrace();
    }
    return output.toString();
  }
  
  /**
   * Lists all bundles in a target framework
   * @param index - the index of the target framework
   * @return the result of the operation in the form of list of bundles
   */
  public static String listBundles(long index) {
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
      innerCtx = getBundleContext(info.getBundle());
      Object[] innerBundles = getBundles(innerCtx);
      for (Object bnd : innerBundles) {
        output.append(getBundleId(bnd)).append("\t").append(getStateName((Integer) getState(bnd))).append(getSymbolicName(bnd)).append("_").append(getVersion(bnd)).append(NEW_LINE);
      }
    } catch (IllegalArgumentException e) {
      e.printStackTrace();
    } catch (SecurityException e) {
      e.printStackTrace();
    } catch (IllegalAccessException e) {
      e.printStackTrace();
    } catch (InvocationTargetException e) {
      e.printStackTrace();
    } catch (NoSuchMethodException e) {
      e.printStackTrace();
    }
    return output.toString();
  }
  
  /**
   * Lists properties for a target framework. If it is Equinox framework all properties will be listed, 
   * for other OSGI frameworks only a subset of OSGI properties will be listed
   * @param frkIndex the target framework index
   * @return the result of the operation (list of properties)
   */
  public static String listProperties(long frkIndex) {
    StringBuilder output = new StringBuilder();
    output.append("Listing all properties for framework [").append(frkIndex).append("]..").append(NEW_LINE);
    output.append("----------------------------------------------------------");
    output.append(NEW_LINE);
    FrameworkData info = FrameworkCollector.getFrameworkByID(frkIndex);
    Object bc = null;
    try {
      bc = getBundleContext(info.getBundle());
      Properties props = (Properties) getEquinoxProperties(bc.getClass().getClassLoader());
      for (Object key : props.keySet()) {
        output.append(key).append(" = ").append(getProperty(bc, (String)key)).append(NEW_LINE);
      }
    } catch (SecurityException e) {
      e.printStackTrace();
    } catch (IllegalArgumentException e) {
      e.printStackTrace();
    } catch (NoSuchMethodException e) {
      e.printStackTrace();
    } catch (InstantiationException e) {
      e.printStackTrace();
    } catch (IllegalAccessException e) {
      e.printStackTrace();
    } catch (InvocationTargetException e) {
      e.printStackTrace();
    } catch (ClassNotFoundException e) {
      output = new StringBuilder();
      output.append("This is not an Equinox framework. Only standard OSGi properties will be displayed:").append(NEW_LINE);
      output.append(getOSGiProps(frkIndex, bc));
    }
    return output.toString();
  }
  
  private static String getOSGiProps(long frkIndex, Object bundleContext) {
    StringBuilder output = new StringBuilder();
    for (String key : osgiProps) {
      try {
        if (getProperty(bundleContext, key) != null) {
          output.append(key).append(" = ").append(getProperty(bundleContext, key)).append(NEW_LINE);
        }
      } catch (SecurityException e) {
        e.printStackTrace();
      } catch (IllegalArgumentException e) {
        e.printStackTrace();
      } catch (NoSuchMethodException e) {
        e.printStackTrace();
      } catch (IllegalAccessException e) {
        e.printStackTrace();
      } catch (InvocationTargetException e) {
        e.printStackTrace();
      }
    }
    return output.toString();
  }
  
  /**
   * This method outputs some debug and supportability information about a target framework, 
   * such as what are its what are its bootDelegation and parentClassLoader property values,
   * as well as what are the resources of its system bundle loaders
   * @param frkIndex - the target framework's index
   * @return the result of the operation including all the resources and the property values
   */
  public static String checkVisibility(long frkIndex) {
    StringBuilder output = new StringBuilder();
    FrameworkData info = FrameworkCollector.getFrameworkByID(frkIndex);
    try {
      output.append(NEW_LINE);
      output.append("This framework's:").append(NEW_LINE);
      output.append("----------------------------------------------------------").append(NEW_LINE);
      Object frkParentLoader = getProperty(getBundleContext(info.getBundle()), osgiProps[2]);
      output.append(osgiProps[1]).append(" = ").append(getProperty(getBundleContext(info.getBundle()), osgiProps[1])).append(NEW_LINE);
      output.append(osgiProps[2]).append(" = ").append(frkParentLoader == null ? "boot" : frkParentLoader).append(NEW_LINE);
      output.append(NEW_LINE);
      output.append("bundle loader resources:").append(NEW_LINE);
      URLConverterImpl converter = new URLConverterImpl();
      ClassLoader cl = info.getBundle().getClass().getClassLoader();
      if (cl instanceof URLClassLoader) {
        URL[] jars = ((URLClassLoader) cl).getURLs();
        for (URL jar : jars) {
          output.append(jar).append(NEW_LINE);
        }
      } else {
        Enumeration<URL> jars = cl.getResources("/META-INF/MANIFEST.MF");
        Set<String> res = new HashSet<String>();
        while (jars.hasMoreElements()) {
          res.add(converter.toFileURL(jars.nextElement()).toString().substring(6));
        }
        for (String strRes : res) {
          output.append(strRes.substring(0, strRes.length() - "/META-INF/MANIFEST.MF".length())).append(NEW_LINE);
        }
      }
      output.append(NEW_LINE);
      output.append("bundle parent loader resources:").append(NEW_LINE);
      ClassLoader parent = cl.getParent();
      //if the parent is null print no resources
      if (parent != null) {
        if (cl.getParent() instanceof URLClassLoader) {
          URL[] jars = ((URLClassLoader) cl.getParent()).getURLs();
          for (URL jar : jars) {
            output.append(jar).append(NEW_LINE);
          }
        } else {
          Set<String> res = new HashSet<String>();
          Enumeration<URL> jars_enum = cl.getParent().getResources("/META-INF/MANIFEST.MF");
          res.clear();
          while (jars_enum.hasMoreElements()) {
            res.add(converter.toFileURL(jars_enum.nextElement()).toString().substring(6));
          }
          for (String strRes : res) {
            output.append(strRes.substring(0, strRes.length() - "/META-INF/MANIFEST.MF".length())).append(NEW_LINE);
          }
        }
      } else {
        output.append("No parent loader detected").append(NEW_LINE);
      }
    } catch (SecurityException e) {
      e.printStackTrace();
    } catch (IllegalArgumentException e) {
      e.printStackTrace();
    } catch (NoSuchMethodException e) {
      e.printStackTrace();
    } catch (IllegalAccessException e) {
      e.printStackTrace();
    } catch (InvocationTargetException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return output.toString();
  }
  
  /**
   * Updates a bundle in a target framework
   * @param frkIndex - the target framework's index
   * @param bndIndex - the target bundle 
   * @return the output of the operation in string form
   */
  public static String updateBundle(long frkIndex, long bndIndex) {
    StringBuilder output = new StringBuilder();
    FrameworkData info = FrameworkCollector.getFrameworkByID(frkIndex);
    output.append("Updating bundle [").append(bndIndex).append("] in framework [").append(frkIndex).append("]..").append(NEW_LINE);
    if (info == null) {
      output.append("ERROR: Inner framework [").append(frkIndex).append("] is not available, check if it is stopped").append(NEW_LINE);
      return output.toString();
    }
    try {
      Object innerCtx = getBundleContext(info.getBundle());
      Object bnd = getBundle(innerCtx, bndIndex);
      if (bnd != null) {
        update(bnd);
        output.append("Bundle [").append(getSymbolicName(bnd)).append("_").append(getVersion(bnd)).append("] updated successfully in framework [").append(frkIndex).append("]!").append(NEW_LINE);
      } else {
        output.append("ERROR: Cannot find bundle [").append(bndIndex).append("] in framework [").append(frkIndex).append("]");
      }
    } catch (IllegalStateException ise) {
      ise.printStackTrace();
    } catch (SecurityException se) {
      se.printStackTrace();
    } catch (IllegalArgumentException e) {
      e.printStackTrace();
    } catch (NoSuchMethodException e) {
      e.printStackTrace();
    } catch (IllegalAccessException e) {
      e.printStackTrace();
    } catch (InvocationTargetException e) {
      e.printStackTrace();
    }
    return output.toString();
  }

  /**
   * Helper method 
   * @return the current registered framework count
   */
  public static int getFrameworksCount() {
    return FrameworkCollector.getFrameworks().values().size();
  }
  
  /**
   * Helper method
   * @param frameworkIndex - the target framework
   * @return the bundles count for a target framework
   */
  public static int getBundlesCount(int frameworkIndex) {
    return ((Bundle)FrameworkCollector.getFrameworkByID(frameworkIndex).getBundle()).getBundleContext().getBundles().length;
  }

  private static void normalizeFrameworkIds() {
    Map<String, FrameworkData> frameworks = FrameworkCollector.getFrameworks();
    for (FrameworkData info : frameworks.values()) {
      info.setID(info.getID() * (-1));
    }
  }

  private static String processTree(long innerId, String offset, Hashtable<Long, ArrayList<Long>> relations, Hashtable<Long, Object> frkOriginMap) throws SecurityException, IllegalArgumentException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
    StringBuilder result = new StringBuilder();
    FrameworkData frkData = FrameworkCollector.getFrameworkByID(innerId);
    if (frkData != null) {
      String osgi = frkData.getBundle().getClass().getName();
      frkData.setID(fixedIndex);
      if (osgi.contains("eclipse")) {
        osgi = "Equinox";
      } else {
        if (osgi.contains("felix")) {
          osgi = "Felix";
        } else {
          osgi = "OSGi";
        }
      }
      result.append(offset).append("[ ").append(fixedIndex * (-1)).append(" ] ").append(osgi).append(" [").append(getSymbolicName(frkOriginMap.get(innerId))).append("_").append(getVersion(frkOriginMap.get(innerId))).append("]").append(NEW_LINE);
      for (Long lvl : relations.get(innerId)) {
        fixedIndex--;
        String frkResult = processTree(lvl, "     " + offset, relations, frkOriginMap);
        if (frkResult != null) {
          result.append(frkResult);
        }
      }
      return result.toString();
    } else {
      return null;
    }
  }
  
  //Utility methods below
  
  private static String getStateName(int state) {
    switch (state) {
      case Bundle.UNINSTALLED:
        return "UNINSTALLED "; //$NON-NLS-1$

      case Bundle.INSTALLED:
        return "INSTALLED   "; //$NON-NLS-1$

      case Bundle.RESOLVED:
        return "RESOLVED    "; //$NON-NLS-1$

      case Bundle.STARTING:
        return "STARTING    "; //$NON-NLS-1$

      case Bundle.STOPPING:
        return "STOPPING    "; //$NON-NLS-1$

      case Bundle.ACTIVE:
        return "ACTIVE      "; //$NON-NLS-1$

      default:
        return Integer.toHexString(state);
    }
  }
  
  private static Object loadClass(Object bundle, String name) throws SecurityException, NoSuchMethodException, IllegalArgumentException, IllegalAccessException, InvocationTargetException {
    Method m = bundle.getClass().getMethod("loadClass", new Class[]{String.class});
    m.setAccessible(true);
    return m.invoke(bundle, new Object[]{name});
  }

  private static Object getResource(Object bundle, String name) throws SecurityException, NoSuchMethodException, IllegalArgumentException, IllegalAccessException, InvocationTargetException {
    Method m = bundle.getClass().getMethod("getResource", new Class[]{String.class});
    m.setAccessible(true);
    return m.invoke(bundle, new Object[]{name});
  }

  private static Object getProperty(Object bundleContext, String key) throws SecurityException, NoSuchMethodException, IllegalArgumentException, IllegalAccessException, InvocationTargetException {
    Method m = bundleContext.getClass().getMethod("getProperty", new Class[]{String.class});
    m.setAccessible(true);
    return m.invoke(bundleContext, new Object[]{key});
  }

  private static Object getBundleContext(Object systemBundle) throws IllegalArgumentException, SecurityException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
    Method m = systemBundle.getClass().getMethod("getBundleContext", new Class[]{});
    m.setAccessible(true);
    return m.invoke(systemBundle, new Object[]{});
  }

  private static Object[] getBundles(Object bundleContext) throws SecurityException, NoSuchMethodException, IllegalArgumentException, IllegalAccessException, InvocationTargetException {
    Method m = bundleContext.getClass().getMethod("getBundles", new Class[]{});
    m.setAccessible(true);
    return (Object[]) m.invoke(bundleContext, new Object[]{});
  }

  private static Object getBundle(Object bundleContext, long bndIndex) throws SecurityException, NoSuchMethodException, IllegalArgumentException, IllegalAccessException, InvocationTargetException {
    Method m = bundleContext.getClass().getMethod("getBundle", new Class[]{long.class});
    m.setAccessible(true);
    return m.invoke(bundleContext, new Object[]{bndIndex});
  }

  private static Object getBundleId(Object bundle) throws SecurityException, NoSuchMethodException, IllegalArgumentException, IllegalAccessException, InvocationTargetException {
    Method m = bundle.getClass().getMethod("getBundleId", new Class[]{});
    m.setAccessible(true);
    return m.invoke(bundle, new Object[]{});
  }

  private static Object getSymbolicName(Object bundle) throws SecurityException, NoSuchMethodException, IllegalArgumentException, IllegalAccessException, InvocationTargetException {
    Method m = bundle.getClass().getMethod("getSymbolicName", new Class[]{});
    m.setAccessible(true);
    return m.invoke(bundle, new Object[]{});
  }

  private static int getState(Object bundle) throws SecurityException, NoSuchMethodException, IllegalArgumentException, IllegalAccessException, InvocationTargetException {
    Method m = bundle.getClass().getMethod("getState", new Class[]{});
    m.setAccessible(true);
    return (Integer) m.invoke(bundle, new Object[]{});
  }

  private static Object start(Object bundle) throws SecurityException, NoSuchMethodException, IllegalArgumentException, IllegalAccessException, InvocationTargetException {
    Method m = bundle.getClass().getMethod("start", new Class[]{});
    m.setAccessible(true);
    return m.invoke(bundle, new Object[]{});
  }

  private static Object stop(Object bundle) throws SecurityException, NoSuchMethodException, IllegalArgumentException, IllegalAccessException, InvocationTargetException {
    Method m = bundle.getClass().getMethod("stop", new Class[]{});
    m.setAccessible(true);
    return m.invoke(bundle, new Object[]{});
  }

  private static Object update(Object bundle) throws SecurityException, NoSuchMethodException, IllegalArgumentException, IllegalAccessException, InvocationTargetException {
    Method m = bundle.getClass().getMethod("update", new Class[]{});
    m.setAccessible(true);
    return m.invoke(bundle, new Object[]{});
  }

  private static Object uninstall(Object bundle) throws SecurityException, NoSuchMethodException, IllegalArgumentException, IllegalAccessException, InvocationTargetException {
    Method m = bundle.getClass().getMethod("uninstall", new Class[]{});
    m.setAccessible(true);
    return m.invoke(bundle, new Object[]{});
  }

  private static Object install(Object bundleContext, String bndUrl) throws SecurityException, NoSuchMethodException, IllegalArgumentException, IllegalAccessException, InvocationTargetException {
    Method m = bundleContext.getClass().getMethod("installBundle", new Class[]{String.class});
    m.setAccessible(true);
    return m.invoke(bundleContext, new Object[]{bndUrl});
  }

  private static Object getEquinoxProperties(ClassLoader cl) throws SecurityException, NoSuchMethodException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, ClassNotFoundException {
    Class fp = Class.forName("org.eclipse.osgi.framework.internal.core.FrameworkProperties", true, cl);
    Method m = fp.getMethod("getProperties", new Class[]{});
    m.setAccessible(true);
    return m.invoke(fp.newInstance(), new Object[]{});
  }

  private static Object getVersion(Object bundle) {
    Method m;
    Object result = null;
    try {
      m = bundle.getClass().getMethod("getVersion", new Class[]{});
      m.setAccessible(true);
      result = m.invoke(bundle, new Object[]{});
    } catch (SecurityException e) {
      e.printStackTrace();
    } catch (NoSuchMethodException e) {
      result = "N/A";
    } catch (IllegalArgumentException e) {
      e.printStackTrace();
    } catch (IllegalAccessException e) {
      result = "N/A";
    } catch (InvocationTargetException e) {
      e.printStackTrace();
    }
    return result;
  }
}
