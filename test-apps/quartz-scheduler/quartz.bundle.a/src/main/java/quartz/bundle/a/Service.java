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

package quartz.bundle.a;

import org.quartz.Scheduler;

/**
 * Simple test service to serve as a scheduled job.
 * 
 */
public interface Service {

    int getCount();

    Scheduler getScheduler();

    void process();

}
