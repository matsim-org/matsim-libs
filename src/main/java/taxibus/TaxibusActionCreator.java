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

package taxibus;

import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.passenger.*;
import org.matsim.contrib.dvrp.vrpagent.*;
import org.matsim.contrib.dynagent.*;

import taxibus.tasks.*;

public class TaxibusActionCreator implements VrpAgentLogic.DynActionCreator {
	public static final String TAXIBUS_STAY_NAME = "TaxibusStay";
	public static final String TAXIBUS_DROPOFF_NAME = "TaxibusDropoff";
	public final static String TAXIBUS_PICKUP_NAME = "TaxibusPickup";
	private final PassengerEngine passengerEngine;
	private final VrpLegFactory legFactory;
	private final double pickupDuration;

	public TaxibusActionCreator(PassengerEngine passengerEngine, VrpLegFactory legFactory, double pickupDuration) {
		this.passengerEngine = passengerEngine;
		this.legFactory = legFactory;
		this.pickupDuration = pickupDuration;
	}

	@Override
	public DynAction createAction(DynAgent dynAgent, Vehicle vehicle, double now) {
		TaxibusTask task = (TaxibusTask)vehicle.getSchedule().getCurrentTask();;
		switch (task.getTaxibusTaskType()) {
			case DRIVE_EMPTY:
			case DRIVE_WITH_PASSENGERS:
				return legFactory.create(vehicle);

			case PICKUP:
				final TaxibusPickupTask pst = (TaxibusPickupTask)task;
				return new MultiPassengerPickupActivity(passengerEngine, dynAgent, pst, pst.getRequests(),
						pickupDuration, TAXIBUS_PICKUP_NAME);

			case DROPOFF:
				final TaxibusDropoffTask dst = (TaxibusDropoffTask)task;
				return new MultiPassengerDropoffActivity(passengerEngine, dynAgent, dst, dst.getRequests(),
						TAXIBUS_DROPOFF_NAME);

			case STAY:
				return new VrpActivity(TAXIBUS_STAY_NAME, (TaxibusStayTask)task);

			default:
				throw new IllegalStateException();
		}
	}
}
