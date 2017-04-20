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

package playground.michalm.taxi.vrpagent;

import org.matsim.contrib.dynagent.AbstractDynActivity;

import playground.michalm.taxi.data.EvrpVehicle.Ev;
import playground.michalm.taxi.ev.ETaxiChargingLogic;
import playground.michalm.taxi.schedule.ETaxiChargingTask;

public class ETaxiAtChargerActivity extends AbstractDynActivity {
	public static final String STAY_AT_CHARGER_ACTIVITY_TYPE = "ETaxiStayAtCharger";

	private final ETaxiChargingTask chargingTask;

	private double endTime = END_ACTIVITY_LATER;

	private enum State {
		init, queued, plugged, unplugged
	};

	private State state = State.init;

	public ETaxiAtChargerActivity(ETaxiChargingTask chargingTask) {
		super(STAY_AT_CHARGER_ACTIVITY_TYPE);
		this.chargingTask = chargingTask;
	}

	@Override
	public void doSimStep(double now) {
		switch (state) {
			case queued:
			case plugged:
				if (endTime <= now) {
					endTime = now + 1;
				}
				return;

			case init:
				initialize(now);
				return;

			default:
				return;
		}
	}

	private void initialize(double now) {
		ETaxiChargingLogic logic = chargingTask.getLogic();
		Ev ev = chargingTask.getEv();

		logic.removeAssignedVehicle(ev);
		ev.setAtChargerActivity(this);
		logic.addVehicle(ev, now);
	}

	@Override
	public double getEndTime() {
		return endTime;
	}

	public void vehicleQueued(double now) {
		ETaxiChargingLogic logic = chargingTask.getLogic();
		endTime = now + logic.estimateMaxWaitTimeOnArrival() + logic.estimateChargeTime(chargingTask.getEv());
		state = State.queued;
	}

	public void chargingStarted(double now) {
		// if veh is fully charged we must add at least 1 second to make sure that there is
		// at least 1 time step (1 second) between chargingStarted() and chargingEnded()
		endTime = now + Math.max(1, chargingTask.getLogic().estimateChargeTime(chargingTask.getEv()));
		state = State.plugged;
		chargingTask.setChargingStartedTime(now);
	}

	public void chargingEnded(double now) {
		endTime = now;
		state = State.unplugged;
	}

	@Override
	public void finalizeAction(double now) {
		chargingTask.getEv().setAtChargerActivity(null);
	}
}
