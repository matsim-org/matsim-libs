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

package org.matsim.contrib.taxi.vrpagent;

import static org.matsim.contrib.taxi.schedule.TaxiTaskBaseType.getBaseTypeOrElseThrow;

import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.passenger.PassengerHandler;
import org.matsim.contrib.dvrp.passenger.SinglePassengerDropoffActivity;
import org.matsim.contrib.dvrp.passenger.SinglePassengerPickupActivity;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.contrib.dvrp.tracker.OnlineTrackerListener;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentLogic;
import org.matsim.contrib.dvrp.vrpagent.VrpLegFactory;
import org.matsim.contrib.dynagent.DynAction;
import org.matsim.contrib.dynagent.DynAgent;
import org.matsim.contrib.dynagent.IdleDynActivity;
import org.matsim.contrib.taxi.run.TaxiConfigGroup;
import org.matsim.contrib.taxi.schedule.TaxiDropoffTask;
import org.matsim.contrib.taxi.schedule.TaxiPickupTask;
import org.matsim.core.mobsim.framework.MobsimTimer;

/**
 * @author michalm
 */
public class TaxiActionCreator implements VrpAgentLogic.DynActionCreator {
	public static final String PICKUP_ACTIVITY_TYPE = "TaxiPickup";
	public static final String DROPOFF_ACTIVITY_TYPE = "TaxiDropoff";
	public static final String STAY_ACTIVITY_TYPE = "TaxiStay";

	private final PassengerHandler passengerHandler;
	private final VrpLegFactory legFactory;

	public TaxiActionCreator(PassengerHandler passengerHandler, TaxiConfigGroup taxiCfg, MobsimTimer timer,
			DvrpConfigGroup dvrpCfg) {
		this(passengerHandler, taxiCfg.isOnlineVehicleTracker() ?
				v -> VrpLegFactory.createWithOnlineTracker(dvrpCfg.getMobsimMode(), v,
						OnlineTrackerListener.NO_LISTENER, timer) :
				v -> VrpLegFactory.createWithOfflineTracker(dvrpCfg.getMobsimMode(), v, timer));
	}

	public TaxiActionCreator(PassengerHandler passengerHandler, VrpLegFactory legFactory) {
		this.passengerHandler = passengerHandler;
		this.legFactory = legFactory;
	}

	@Override
	public DynAction createAction(DynAgent dynAgent, DvrpVehicle vehicle, double now) {
		Task task = vehicle.getSchedule().getCurrentTask();
		switch (getBaseTypeOrElseThrow(task)) {
			case EMPTY_DRIVE:
			case OCCUPIED_DRIVE:
				return legFactory.create(vehicle);

			case PICKUP:
				final TaxiPickupTask pst = (TaxiPickupTask)task;
				return new SinglePassengerPickupActivity(passengerHandler, dynAgent, pst, pst.getRequest(),
						PICKUP_ACTIVITY_TYPE);

			case DROPOFF:
				final TaxiDropoffTask dst = (TaxiDropoffTask)task;
				return new SinglePassengerDropoffActivity(passengerHandler, dynAgent, dst, dst.getRequest(),
						DROPOFF_ACTIVITY_TYPE);

			case STAY:
				return new IdleDynActivity(STAY_ACTIVITY_TYPE, task::getEndTime);

			default:
				throw new IllegalStateException();
		}
	}
}
