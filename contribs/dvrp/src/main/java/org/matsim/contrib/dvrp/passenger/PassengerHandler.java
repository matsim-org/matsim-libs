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
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;

/**
 * @author Michal Maciejewski (michalm)
 *
 * This looks quite general.  But as of now is a dvrp thing.  kai, apr'23
 */
public interface PassengerHandler {
	boolean notifyWaitForPassengers(PassengerPickupActivity pickupActivity, MobsimDriverAgent driver, Id<Request> requestId);

	boolean tryPickUpPassengers(PassengerPickupActivity pickupActivity, MobsimDriverAgent driver, Id<Request> requestId,
			double now);

	void dropOffPassengers(MobsimDriverAgent driver, Id<Request> requestId, double now);
}
