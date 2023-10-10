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

import java.util.Collection;

/**
 * @author Michal Maciejewski (michalm)
 * @author Sebastian Hörl (sebhoerl), IRT SystemX
 */
public interface RequestQueue<R extends PassengerRequest> {
	void updateQueuesOnNextTimeSteps(double currentTime);

	void addRequest(R request);

	/**
	 * Assumes external code can modify schedulableRequests (e.g. remove scheduled
	 * requests and add unscheduled ones)
	 *
	 * @return requests to be inserted into vehicle schedules
	 */
	public Collection<R> getSchedulableRequests();
}
