/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2021 by the members listed in the COPYING,        *
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

package org.matsim.contrib.evrp;

import org.matsim.contrib.dynagent.DynActivity;
import org.matsim.contrib.ev.charging.ChargingStrategy;
import org.matsim.contrib.ev.charging.ChargingWithAssignmentLogic;
import org.matsim.contrib.ev.fleet.ElectricVehicle;

public class ChargingActivity implements DynActivity {
	/**
	 * String constant should have different value from {@link org.matsim.contrib.ev.charging.VehicleChargingHandler#CHARGING_INTERACTION}.
	 */
	public static final String ACTIVITY_TYPE = "Charging";

	private final ChargingTask chargingTask;
	private double endTime = END_ACTIVITY_LATER;

	private enum State {
		init, added, queued, plugged, unplugged
	}

	private State state = State.init;

	public ChargingActivity(ChargingTask chargingTask) {
		this.chargingTask = chargingTask;
	}

	@Override
	public String getActivityType() {
		return ACTIVITY_TYPE;
	}

	@Override
	public void doSimStep(double now) {
		switch (state) {
			case init -> initialize(now);
			case added, queued, plugged -> {
				if (endTime <= now) {
					endTime = now + 1;
				}
			}
		}
	}

	private void initialize(double now) {
		ChargingWithAssignmentLogic logic = chargingTask.getChargingLogic();
		ElectricVehicle ev = chargingTask.getElectricVehicle();
		logic.unassignVehicle(ev);
		logic.addVehicle(ev, chargingTask.getChargingStrategy(), new DvrpChargingListener(this), now);
		state = State.added;
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
