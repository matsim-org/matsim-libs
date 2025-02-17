/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.dvrp.load.DvrpLoad;
import org.matsim.contrib.dvrp.optimizer.Request;

import java.util.List;

public interface PassengerRequest extends Request {
	/**
	 * @return beginning of the time window (inclusive) - earliest time when the passenger can be picked up
	 */
	double getEarliestStartTime();

	/**
	 * @return end of the time window (exclusive) - time by which the passenger should be picked up
	 */
	default double getLatestStartTime() {
		return Double.MAX_VALUE;
	}

	Link getFromLink();

	Link getToLink();

	List<Id<Person>> getPassengerIds();

	String getMode();

	DvrpLoad getLoad();
}
