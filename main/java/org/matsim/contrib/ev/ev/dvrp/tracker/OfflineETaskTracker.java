/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
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
 * *********************************************************************** */

package org.matsim.contrib.ev.ev.dvrp.tracker;

import org.matsim.contrib.ev.ev.dvrp.ETask;
import org.matsim.contrib.ev.ev.dvrp.EvDvrpVehicle;
import org.matsim.core.mobsim.framework.MobsimTimer;

/**
 * @author michalm
 */
public class OfflineETaskTracker implements ETaskTracker {
	private final MobsimTimer timer;
	private final ETask task;
	private final double socAtStart;

	public OfflineETaskTracker(EvDvrpVehicle vehicle, MobsimTimer timer) {
		this.timer = timer;
		task = (ETask)vehicle.getSchedule().getCurrentTask();
		socAtStart = vehicle.getElectricVehicle().getBattery().getSoc();
	}

	@Override
	public double predictEndTime() {
		return Math.max(task.getEndTime(), timer.getTimeOfDay());
	}

	@Override
	public double predictSocAtEnd() {
		return socAtStart - task.getTotalEnergy();
	}
}
