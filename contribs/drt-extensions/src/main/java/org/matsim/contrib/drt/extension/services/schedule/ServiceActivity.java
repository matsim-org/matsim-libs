/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2024 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** *
 */
package org.matsim.contrib.drt.extension.services.schedule;

import org.matsim.contrib.drt.extension.services.tasks.DrtServiceTask;
import org.matsim.contrib.dynagent.DynActivity;

public class ServiceActivity implements DynActivity {
	public static final String ACTIVITY_TYPE = "Service";
	private final DrtServiceTask drtServiceTask;

	public ServiceActivity(DrtServiceTask drtServiceTask) {
		this.drtServiceTask = drtServiceTask;
	}

	@Override
	public String getActivityType() {
		return ACTIVITY_TYPE;
	}

	@Override
	public double getEndTime() {
		return this.drtServiceTask.getEndTime();
	}

	@Override
	public void doSimStep(double now) {

	}


}
