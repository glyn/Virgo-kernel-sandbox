/*******************************************************************************
 * Copyright (c) 2008, 2010 VMware Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   VMware Inc. - initial contribution
 *******************************************************************************/

package org.eclipse.virgo.kernel.osgi.region;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.packageadmin.ExportedPackage;
import org.osgi.service.packageadmin.PackageAdmin;


import org.eclipse.virgo.kernel.osgi.framework.OsgiFrameworkLogEvents;
import org.eclipse.virgo.kernel.serviceability.Assert;
import org.eclipse.virgo.medic.eventlog.EventLogger;
import org.eclipse.virgo.util.osgi.manifest.BundleManifest;
import org.eclipse.virgo.util.osgi.manifest.BundleManifestFactory;
import org.eclipse.virgo.util.osgi.manifest.DynamicImportPackage;
import org.eclipse.virgo.util.osgi.manifest.DynamicallyImportedPackage;

/**
 * {@link PackageImportWildcardExpander} expands the wildcards in a string containing the body of an import package
 * header.
 * <p />
 * 
 * <strong>Concurrent Semantics</strong><br />
 * 
 * Thread safe.
 * 
 */
final class PackageImportWildcardExpander {

    private static final String wildcard = "*";

    static String expandPackageImportsWildcards(String userRegionImportsProperty, BundleContext bundleContext) {
        ServiceReference eventLoggerServiceReference = bundleContext.getServiceReference(EventLogger.class.getName());
        EventLogger eventLogger = (EventLogger) bundleContext.getService(eventLoggerServiceReference);

        String[] exportedPackageNames = getExportedPackageNames(bundleContext);

        String expandedUserRegionImportsProperty = expandWildcards(userRegionImportsProperty, exportedPackageNames, eventLogger);

        bundleContext.ungetService(eventLoggerServiceReference);

        return expandedUserRegionImportsProperty;
    }

    private static String[] getExportedPackageNames(BundleContext bundleContext) {
        Set<String> exportedPackageNames = new HashSet<String>();
        for (ExportedPackage exportedPackage : getExportedPackages(bundleContext)) {
            exportedPackageNames.add(exportedPackage.getName());
        }
        return exportedPackageNames.toArray(new String[] {});
    }

    private static ExportedPackage[] getExportedPackages(BundleContext bundleContext) {
        ServiceReference paServiceReference = bundleContext.getServiceReference(PackageAdmin.class.getName());
        PackageAdmin pa = (PackageAdmin) bundleContext.getService(paServiceReference);

        ExportedPackage[] exportedPackages = pa.getExportedPackages((Bundle) null);
        Assert.notNull(exportedPackages, "Expected at least one exported package");
        
        bundleContext.ungetService(paServiceReference);
        
        return exportedPackages;
    }

    private static String expandWildcards(String userRegionImportsProperty, String[] exportedPackageNamess, EventLogger eventLogger) {
        DynamicImportPackage dynamicImportPackage = representImportsAsDynamicImports(userRegionImportsProperty, eventLogger);
        return expandWildcards(dynamicImportPackage, exportedPackageNamess, eventLogger);
    }

    private static DynamicImportPackage representImportsAsDynamicImports(String userRegionImportsProperty, EventLogger eventLogger) {
        Dictionary<String, String> headers = new Hashtable<String, String>();
        headers.put("DynamicImport-Package", userRegionImportsProperty);

        BundleManifest manifest = BundleManifestFactory.createBundleManifest(headers, new RegionManagerParserLogger(eventLogger));
        return manifest.getDynamicImportPackage();
    }

    private static String expandWildcards(DynamicImportPackage dynamicImportPackage, String[] exportedPackageNames, EventLogger eventLogger) {
        StringBuffer expandedPackages = new StringBuffer();
        boolean first = true;
        List<DynamicallyImportedPackage> dynamicallyImportedPackages = dynamicImportPackage.getDynamicallyImportedPackages();
        for (DynamicallyImportedPackage dynamicallyImportedPackage : dynamicallyImportedPackages) {
            String possiblyWildcardedPackageName = dynamicallyImportedPackage.getPackageName();
            if (possiblyWildcardedPackageName.endsWith(wildcard)) {
                List<String> expansions = expandWildcard(possiblyWildcardedPackageName, exportedPackageNames, eventLogger);
                for (String expansion : expansions) {
                    dynamicallyImportedPackage.setPackageName(expansion);
                    if (first) {
                        first = false;
                    } else {
                        expandedPackages.append(",");
                    }
                    expandedPackages.append(dynamicallyImportedPackage.toParseString());
                }
            } else {
                if (first) {
                    first = false;
                } else {
                    expandedPackages.append(",");
                }
                expandedPackages.append(dynamicallyImportedPackage.toParseString());
            }

        }
        return expandedPackages.toString();
    }

    private static List<String> expandWildcard(String wildcardedPackageName, String[] exportedPackageNames, EventLogger eventLogger) {
        List<String> expansions = new ArrayList<String>();

        String prefix = wildcardedPackageName.substring(0, wildcardedPackageName.length() - 1);
        for (String exportedPackage : exportedPackageNames) {
            if (exportedPackage.startsWith(prefix)) {
                expansions.add(exportedPackage);
            }
        }
        if (expansions.isEmpty()) {
            eventLogger.log(OsgiFrameworkLogEvents.REGION_IMPORT_NO_MATCH, wildcardedPackageName);
        }
        return expansions;
    }

}
