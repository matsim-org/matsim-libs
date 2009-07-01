/* *********************************************************************** *
 * project: org.matsim.*
 * CreateTimetableForStop.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package playground.marcel.pt.transitSchedule.modules;

import java.util.Arrays;
import java.util.Collection;

import org.matsim.transitSchedule.TransitStopFacility;

import playground.marcel.pt.transitSchedule.DepartureImpl;
import playground.marcel.pt.transitSchedule.TransitLineImpl;
import playground.marcel.pt.transitSchedule.TransitRouteImpl;
import playground.marcel.pt.transitSchedule.TransitRouteStopImpl;

public class CreateTimetableForStop {

	private final TransitLineImpl line;

	public CreateTimetableForStop(final TransitLineImpl line) {
		this.line = line;
	}

	public double[] getDeparturesAtStop(final TransitStopFacility stop) {
		int numOfDepartures = 0;
		Collection<TransitRouteImpl> routes = this.line.getRoutes().values();
		for (TransitRouteImpl route : routes) {
			numOfDepartures += route.getDepartures().size();
		}
		double[] departures = new double[numOfDepartures];
		int index = 0;
		for (TransitRouteImpl route : routes) {
			TransitRouteStopImpl trStop = route.getStop(stop);
			double delay = trStop.getDepartureDelay();
			for (DepartureImpl dep : route.getDepartures().values()) {
				departures[index] = dep.getDepartureTime() + delay;
				index++;
			}
		}
		Arrays.sort(departures);
		return departures;
	}

}
