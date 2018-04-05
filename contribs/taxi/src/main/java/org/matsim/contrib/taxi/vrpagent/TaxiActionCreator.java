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

import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.optimizer.VrpOptimizer;
import org.matsim.contrib.dvrp.optimizer.VrpOptimizerWithOnlineTracking;
import org.matsim.contrib.dvrp.passenger.PassengerEngine;
import org.matsim.contrib.dvrp.passenger.SinglePassengerDropoffActivity;
import org.matsim.contrib.dvrp.passenger.SinglePassengerPickupActivity;
import org.matsim.contrib.dvrp.vrpagent.VrpActivity;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentLogic;
import org.matsim.contrib.dvrp.vrpagent.VrpLegs;
import org.matsim.contrib.dynagent.DynAction;
import org.matsim.contrib.dynagent.DynAgent;
import org.matsim.contrib.taxi.run.TaxiConfigGroup;
import org.matsim.contrib.taxi.schedule.TaxiDropoffTask;
import org.matsim.contrib.taxi.schedule.TaxiPickupTask;
import org.matsim.contrib.taxi.schedule.TaxiStayTask;
import org.matsim.contrib.taxi.schedule.TaxiTask;
import org.matsim.core.mobsim.framework.MobsimTimer;

import com.google.inject.Inject;

/**
 * @author michalm
 */
public class TaxiActionCreator implements VrpAgentLogic.DynActionCreator {
	public static final String PICKUP_ACTIVITY_TYPE = "TaxiPickup";
	public static final String DROPOFF_ACTIVITY_TYPE = "TaxiDropoff";
	public static final String STAY_ACTIVITY_TYPE = "TaxiStay";

	private final PassengerEngine passengerEngine;
	private final VrpLegs.LegCreator legCreator;
	private final double pickupDuration;

	@Inject
	public TaxiActionCreator(PassengerEngine passengerEngine, TaxiConfigGroup taxiCfg, VrpOptimizer optimizer,
			MobsimTimer timer) {
		this(passengerEngine, taxiCfg.isOnlineVehicleTracker() ? //
				VrpLegs.createLegWithOnlineTrackerCreator((VrpOptimizerWithOnlineTracking)optimizer, timer)
				: VrpLegs.createLegWithOfflineTrackerCreator(timer), taxiCfg.getPickupDuration());
	}

	public TaxiActionCreator(PassengerEngine passengerEngine, VrpLegs.LegCreator legCreator, double pickupDuration) {
		this.passengerEngine = passengerEngine;
		this.legCreator = legCreator;
		this.pickupDuration = pickupDuration;
	}

	@Override
	public DynAction createAction(DynAgent dynAgent, Vehicle vehicle, double now) {
		TaxiTask task = (TaxiTask)vehicle.getSchedule().getCurrentTask();
		switch (task.getTaxiTaskType()) {
			case EMPTY_DRIVE:
			case OCCUPIED_DRIVE:
				return legCreator.createLeg(vehicle);

			case PICKUP:
				final TaxiPickupTask pst = (TaxiPickupTask)task;
				return new SinglePassengerPickupActivity(passengerEngine, dynAgent, pst, pst.getRequest(),
						pickupDuration, PICKUP_ACTIVITY_TYPE);

			case DROPOFF:
				final TaxiDropoffTask dst = (TaxiDropoffTask)task;
				return new SinglePassengerDropoffActivity(passengerEngine, dynAgent, dst, dst.getRequest(),
						DROPOFF_ACTIVITY_TYPE);

			case STAY:
				return new VrpActivity(STAY_ACTIVITY_TYPE, (TaxiStayTask)task);

			default:
				throw new IllegalStateException();
		}
	}
}
