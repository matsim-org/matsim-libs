/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package org.matsim.contrib.ev.dvrp;

import org.matsim.contrib.dynagent.DynActivity;
import org.matsim.contrib.ev.charging.ChargingWithQueueingAndAssignmentLogic;
import org.matsim.contrib.ev.fleet.ElectricVehicle;

public class ChargingActivity implements DynActivity {
	private static final String CHARGING_ACTIVITY_TYPE = "ChargingActivity";

	private final ChargingTask chargingTask;
	private double endTime = END_ACTIVITY_LATER;

	private enum State {
		init, queued, plugged, unplugged
	};

	private State state = State.init;

	public ChargingActivity(ChargingTask chargingTask) {
		this.chargingTask = chargingTask;
	}

	@Override
	public String getActivityType() {
		return CHARGING_ACTIVITY_TYPE;
	}

	@Override
	public void doSimStep(double now) {
		switch (state) {
			case init:
				initialize(now);
				return;

			case queued:
			case plugged:
				if (endTime <= now) {
					endTime = now + 1;
				}
				return;

			default:
				return;
		}
	}

	private void initialize(double now) {
		ChargingWithQueueingAndAssignmentLogic logic = chargingTask.getChargingLogic();
		ElectricVehicle ev = chargingTask.getElectricVehicle();
		logic.unassignVehicle(ev);
		logic.addVehicle(ev, new DvrpChargingListener(this), now);
	}

	@Override
	public double getEndTime() {
		return endTime;
	}

	public void vehicleQueued(double now) {
		state = State.queued;
	}

	public void chargingStarted(double now) {
		state = State.plugged;
		chargingTask.setChargingStartedTime(now);
	}

	public void chargingEnded(double now) {
		endTime = now;
		state = State.unplugged;
	}
}
