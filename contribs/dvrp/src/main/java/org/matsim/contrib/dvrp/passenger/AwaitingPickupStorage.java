/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package org.matsim.contrib.dvrp.passenger;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.dvrp.optimizer.Request;

public class AwaitingPickupStorage {
	// passenger's request id -> driver's stay task
	private final Map<Id<Request>, PassengerPickupActivity> awaitingPickups = new HashMap<>();

	public void storeAwaitingPickup(Id<Request> requestId, PassengerPickupActivity pickupActivity) {
		awaitingPickups.put(requestId, pickupActivity);
	}

	public PassengerPickupActivity retrieveAwaitingPickup(Id<Request> requestId) {
		return awaitingPickups.remove(requestId);
	}
}
