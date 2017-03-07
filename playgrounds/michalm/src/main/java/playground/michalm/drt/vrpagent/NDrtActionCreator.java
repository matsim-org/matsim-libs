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

package playground.michalm.drt.vrpagent;

import org.matsim.contrib.drt.tasks.DrtStayTask;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.passenger.*;
import org.matsim.contrib.dvrp.vrpagent.*;
import org.matsim.contrib.dynagent.*;

import playground.michalm.drt.schedule.*;

/**
 * @author michalm
 */
public class NDrtActionCreator implements VrpAgentLogic.DynActionCreator {
	public static final String DRT_STAY_NAME = "DrtStay";
	public final static String DRT_STOP_NAME = "DrtBusStop";
	private final PassengerEngine passengerEngine;
	private final VrpLegs.LegCreator legCreator;

	public NDrtActionCreator(PassengerEngine passengerEngine, VrpLegs.LegCreator legCreator) {
		this.passengerEngine = passengerEngine;
		this.legCreator = legCreator;
	}

	@Override
	public DynAction createAction(DynAgent dynAgent, Vehicle vehicle, double now) {
		NDrtTask task = (NDrtTask)vehicle.getSchedule().getCurrentTask();
		switch (task.getDrtTaskType()) {
			case DRIVE:
				return legCreator.createLeg(vehicle);

			case STOP:
				NDrtStopTask t = (NDrtStopTask)task;
				return new BusStopActivity(passengerEngine, dynAgent, t, t.getDropoffRequests(), t.getPickupRequests(),
						DRT_STOP_NAME);

			case STAY:
				return new VrpActivity(DRT_STAY_NAME, (DrtStayTask)task);

			default:
				throw new IllegalStateException();
		}
	}
}
