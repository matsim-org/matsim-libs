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

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioUtils;
import playground.ivt.utils.MoreIOUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author thibautd
 */
public class RunExtractData {
	private static final String MODULE = "jointTripIdentifier";
	private static final String EVENTS = "eventFile";
	private static final String DIST = "acceptableDistance_.*";
	private static final String TIME = "acceptableTime_";
	private static final String DIR = "outputDir";

	public static void main(final String[] args) {
		String configFile = args[0];
		Config config = ConfigUtils.loadConfig(configFile);
		Scenario scenario = ScenarioUtils.createScenario(config);
		ScenarioUtils.loadScenario(scenario);
		Network network = scenario.getNetwork();

		ConfigGroup module = config.getModule(MODULE);
		String eventFile = module.getValue(EVENTS);
		String outputDir = module.getValue(DIR);

		MoreIOUtils.initOut( outputDir );

		List<AcceptabilityCondition> conditions = new ArrayList<AcceptabilityCondition>();

		Map<String, String> params = module.getParams();

		for (Map.Entry<String, String> entry : params.entrySet()) {
			if (entry.getKey().matches(DIST)) {
				int dist = (int) Math.round( Double.parseDouble(entry.getValue()) );
				String num = entry.getKey().split("_")[1];
				int time = (int) Math.round( Double.parseDouble(params.get(TIME + num)) );
				conditions.add(new AcceptabilityCondition(dist, time));
			}
		}

		EventsManager eventsManager = EventsUtils.createEventsManager();
		TripReconstructor trips = new TripReconstructor(network);
		eventsManager.addHandler(trips);

		(new MatsimEventsReader(eventsManager)).readFile(eventFile);

		JoinableTrips joinableTripData =
			new JoinableTrips(conditions, trips);

		(new JoinableTripsXmlWriter(joinableTripData)).write(outputDir+"trips.xml.gz");
	}
}

