/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2020 by the members listed in the COPYING,        *
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

package org.matsim.contrib.dvrp.passenger;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.qsim.InternalInterface;

/**
 * @author Michal Maciejewski (michalm)
 */
public class TeleportingPassengerEngine implements PassengerEngine {
	@Override
	public void setInternalInterface(InternalInterface internalInterface) {
	}

	@Override
	public void onPrepareSim() {
	}

	@Override
	public void doSimStep(double time) {
	}

	@Override
	public void afterSim() {
	}

	@Override
	public boolean handleDeparture(double now, MobsimAgent agent, Id<Link> linkId) {
		// departures of the corresponding dvrp mode are not handled --> agents will be teleported
		return false;
	}

	@Override
	public boolean pickUpPassenger(PassengerPickupActivity pickupActivity, MobsimDriverAgent driver,
			PassengerRequest request, double now) {
		throw new UnsupportedOperationException("No picking-up when teleporting");
	}

	@Override
	public void dropOffPassenger(MobsimDriverAgent driver, PassengerRequest request, double now) {
		throw new UnsupportedOperationException("No dropping-off when teleporting");
	}

	@Override
	public void notifyPassengerRequestRejected(PassengerRequestRejectedEvent event) {
		throw new UnsupportedOperationException("No request-vehicle scheduling when teleporting");
	}

	@Override
	public void notifyPassengerRequestScheduled(PassengerRequestScheduledEvent event) {
		throw new UnsupportedOperationException("No request-vehicle scheduling when teleporting");
	}
}
