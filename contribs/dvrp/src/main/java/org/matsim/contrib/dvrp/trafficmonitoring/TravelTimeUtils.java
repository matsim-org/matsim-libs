/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package org.matsim.contrib.dvrp.trafficmonitoring;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.vehicles.Vehicle;

/**
 * @author michalm
 */
public class TravelTimeUtils {
	public static TravelTime maxOfTravelTimes(TravelTime travelTime1, TravelTime travelTime2) {
		return (Link link, double time, Person person, Vehicle vehicle) -> Math.max(
				travelTime1.getLinkTravelTime(link, time, person, vehicle),
				travelTime2.getLinkTravelTime(link, time, person, vehicle));
	}

	public static TravelTime createTravelTimesFromEvents(Network network, Config config, String eventsFile) {
		TravelTimeCalculator ttCalculator = TravelTimeCalculator.create(network, config.travelTimeCalculator());
		initTravelTimeCalculatorFromEvents(ttCalculator, eventsFile);
		return ttCalculator.getLinkTravelTimes();
	}

	public static void initTravelTimeCalculatorFromEvents(TravelTimeCalculator ttCalculator, String eventsFile) {
		EventsManager events = EventsUtils.createEventsManager();
		events.addHandler(ttCalculator);
		new MatsimEventsReader(events).readFile(eventsFile);
	}
}
