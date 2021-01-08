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

import static org.matsim.contrib.drt.schedule.DrtTaskBaseType.getBaseTypeOrElseThrow;

import org.matsim.contrib.drt.schedule.DrtStopTask;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.passenger.BusStopActivity;
import org.matsim.contrib.dvrp.passenger.PassengerHandler;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.contrib.dvrp.tracker.OnlineTrackerListener;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentLogic;
import org.matsim.contrib.dvrp.vrpagent.VrpLegFactory;
import org.matsim.contrib.dynagent.DynAction;
import org.matsim.contrib.dynagent.DynAgent;
import org.matsim.contrib.dynagent.IdleDynActivity;
import org.matsim.core.mobsim.framework.MobsimTimer;

/**
 * @author michalm
 */
public class DrtActionCreator implements VrpAgentLogic.DynActionCreator {
	public static final String DRT_STAY_NAME = "DrtStay";
	public final static String DRT_STOP_NAME = "DrtBusStop";
	private final PassengerHandler passengerHandler;
	private final VrpLegFactory legFactory;

	public DrtActionCreator(PassengerHandler passengerHandler, MobsimTimer timer, DvrpConfigGroup dvrpCfg) {
		this(passengerHandler, v -> VrpLegFactory.createWithOnlineTracker(dvrpCfg.getMobsimMode(), v,
				OnlineTrackerListener.NO_LISTENER, timer));
	}

	public DrtActionCreator(PassengerHandler passengerHandler, VrpLegFactory legFactory) {
		this.passengerHandler = passengerHandler;
		this.legFactory = legFactory;
	}

	@Override
	public DynAction createAction(DynAgent dynAgent, DvrpVehicle vehicle, double now) {
		Task task = vehicle.getSchedule().getCurrentTask();
		switch (getBaseTypeOrElseThrow(task)) {
			case DRIVE:
				return legFactory.create(vehicle);

			case STOP:
				DrtStopTask t = (DrtStopTask)task;
				return new BusStopActivity(passengerHandler, dynAgent, t, t.getDropoffRequests(), t.getPickupRequests(),
						DRT_STOP_NAME);

			case STAY:
				return new IdleDynActivity(DRT_STAY_NAME, task::getEndTime);

			default:
				throw new IllegalStateException();
		}
	}
}
