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
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentLogic;
import org.matsim.contrib.dynagent.DynAction;
import org.matsim.contrib.dynagent.DynAgent;
import org.matsim.core.mobsim.framework.MobsimTimer;

/**
 * @author steffenaxer
 */
public class DrtServiceDynActionCreator implements VrpAgentLogic.DynActionCreator {
	private final VrpAgentLogic.DynActionCreator delegate;
	private final MobsimTimer timer;

	public DrtServiceDynActionCreator(VrpAgentLogic.DynActionCreator delegate, MobsimTimer timer) {
		this.delegate = delegate;
		this.timer = timer;
	}

	public DynAction createAction(DynAgent dynAgent, DvrpVehicle vehicle, double now) {

		Task task = vehicle.getSchedule().getCurrentTask();
		if (task instanceof DrtServiceTask eDrtServiceTask) {
			return new ServiceActivity(eDrtServiceTask);
		}

		return delegate.createAction(dynAgent, vehicle, now);
	}

}
