/* *********************************************************************** *
 * project: org.matsim.*
 * RunExtractData.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.thibautd.analysis.joinabletripsidentifier;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.Module;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;

/**
 * @author thibautd
 */
public class RunExtractData {
	private static final String MODULE = "jointTripIdentifier";
	private static final String EVENTS = "eventFile";
	private static final String DIST = "acceptableDistance";
	private static final String TIME = "acceptableTime";
	private static final String DIR = "outputDir";

	public static void main(final String[] args) {
		// TODO: logging
		String configFile = args[0];
		Config config = ConfigUtils.loadConfig(configFile);
		Scenario scenario = ScenarioUtils.createScenario(config);
		ScenarioUtils.loadScenario(scenario);
		Network network = scenario.getNetwork();

		Module module = config.getModule(MODULE);
		String eventFile = module.getValue(EVENTS);
		double acceptableDist = Double.parseDouble(module.getValue(DIST));
		double acceptableTime = Double.parseDouble(module.getValue(TIME));
		String outputDir = module.getValue(DIR);

		EventsManager eventsManager = EventsUtils.createEventsManager();
		TripReconstructor trips = new TripReconstructor(network);
		eventsManager.addHandler(trips);

		(new MatsimEventsReader(eventsManager)).readFile(eventFile);

		JoinableTrips joinableTripData =
			new JoinableTrips(acceptableDist, acceptableTime, trips);

		(new JoinableTripsXmlWriter(joinableTripData)).write(outputDir+"trips.xml");
	}
}

