/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2021 by the members listed in the COPYING,        *
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

package org.matsim.contrib.drt.schedule;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.drt.passenger.AcceptedDrtRequest;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.contrib.dvrp.schedule.StayTask;

/**
 * A task representing stopping at a bus stop with at least one or more passengers being picked up or dropped off.
 * <p>
 * Note that we can have both dropoff requests and pickup requests for the same stop.  kai, nov'18
 *
 * @author Michal Maciejewski (michalm)
 */
public interface DrtStopTask extends StayTask {
	Map<Id<Request>, AcceptedDrtRequest> getDropoffRequests();

	Map<Id<Request>, AcceptedDrtRequest> getPickupRequests();

	void addDropoffRequest(AcceptedDrtRequest request);

	void addPickupRequest(AcceptedDrtRequest request);
	
	void removePickupRequest(Id<Request> requestId);
	
	void removeDropoffRequest(Id<Request> requestId);
}
