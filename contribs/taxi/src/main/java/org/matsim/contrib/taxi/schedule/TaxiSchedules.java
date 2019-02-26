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

package org.matsim.contrib.taxi.schedule;

import java.util.stream.Stream;

import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.taxi.passenger.TaxiRequest;
import org.matsim.contrib.taxi.schedule.TaxiTask.TaxiTaskType;

public class TaxiSchedules {
	public static Stream<TaxiRequest> getTaxiRequests(Schedule schedule) {
		return schedule.tasks().filter(t -> ((TaxiTask)t).getTaxiTaskType() == TaxiTaskType.PICKUP)
				.map(t -> ((TaxiPickupTask)t).getRequest());
	}
}
