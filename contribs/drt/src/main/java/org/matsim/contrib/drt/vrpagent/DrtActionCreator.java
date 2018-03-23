/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package org.matsim.contrib.drt.vrpagent;

import org.matsim.contrib.drt.schedule.*;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.optimizer.*;
import org.matsim.contrib.dvrp.passenger.*;
import org.matsim.contrib.dvrp.vrpagent.*;
import org.matsim.contrib.dynagent.*;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.qsim.QSim;

import com.google.inject.Inject;

/**
 * @author michalm
 */
public class DrtActionCreator implements VrpAgentLogic.DynActionCreator {
	public static final String DRT_STAY_NAME = "DrtStay";
	public final static String DRT_STOP_NAME = "DrtBusStop";
	private final PassengerEngine passengerEngine;
	private final VrpLegs.LegCreator legCreator;

	@Inject
	public DrtActionCreator(PassengerEngine passengerEngine, VrpOptimizer optimizer, MobsimTimer mobsimTimer) {
		this.passengerEngine = passengerEngine;
		legCreator = VrpLegs.createLegWithOnlineTrackerCreator((VrpOptimizerWithOnlineTracking)optimizer,
				mobsimTimer);
	}

	@Override
	public DynAction createAction(DynAgent dynAgent, Vehicle vehicle, double now) {
		DrtTask task = (DrtTask)vehicle.getSchedule().getCurrentTask();
		switch (task.getDrtTaskType()) {
			case DRIVE:
				return legCreator.createLeg(vehicle);

			case STOP:
				DrtStopTask t = (DrtStopTask)task;
				return new BusStopActivity(passengerEngine, dynAgent, t, t.getDropoffRequests(), t.getPickupRequests(),
						DRT_STOP_NAME);

			case STAY:
				return new VrpActivity(DRT_STAY_NAME, (DrtStayTask)task);

			default:
				throw new IllegalStateException();
		}
	}
}
