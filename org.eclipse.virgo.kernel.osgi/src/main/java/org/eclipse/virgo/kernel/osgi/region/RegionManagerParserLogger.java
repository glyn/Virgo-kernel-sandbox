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

import org.eclipse.virgo.kernel.osgi.framework.OsgiFrameworkLogEvents;
import org.eclipse.virgo.medic.eventlog.EventLogger;
import org.eclipse.virgo.util.osgi.manifest.parse.ParserLogger;

/**
 * {@link RegionManagerParserLogger} maps OSGi bundle manifest parsing errors to a log message.
 * <p />
 * 
 * <strong>Concurrent Semantics</strong><br />
 * 
 * This class is thread safe.
 * 
 */
final class RegionManagerParserLogger implements ParserLogger {

    private final EventLogger eventLogger;

    public RegionManagerParserLogger(EventLogger eventLogger) {
        this.eventLogger = eventLogger;
    }

    public String[] errorReports() {
        return new String[0];
    }

    public void outputErrorMsg(Exception re, String item) {
        this.eventLogger.log(OsgiFrameworkLogEvents.REGION_IMPORTS_PARSE_FAILED, re, item);
    }
}
