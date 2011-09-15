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

import java.io.File;
import java.io.IOException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.Module;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.CollectLogMessagesAppender;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.ConfigUtils;

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

		Module module = config.getModule(MODULE);
		String eventFile = module.getValue(EVENTS);
		String outputDir = module.getValue(DIR);

		try {
			// create directory if does not exist
			if (!outputDir.substring(outputDir.length() - 1, outputDir.length()).equals("/")) {
				outputDir += "/";
			}
			File outputDirFile = new File(outputDir);
			if (!outputDirFile.exists()) {
				outputDirFile.mkdirs();
			}

			// init logFile
			CollectLogMessagesAppender appender = new CollectLogMessagesAppender();
			Logger.getRootLogger().addAppender(appender);

			IOUtils.initOutputDirLogging(
				outputDir,
				appender.getLogEvents());
		} catch (IOException e) {
			// do NOT continue without proper logging!
			throw new RuntimeException("error while creating log file",e);
		}

		List<AcceptabilityCondition> conditions = new ArrayList<AcceptabilityCondition>();

		Map<String, String> params = module.getParams();

		for (Map.Entry<String, String> entry : params.entrySet()) {
			if (entry.getKey().matches(DIST)) {
				double dist = Double.parseDouble(entry.getValue());
				String num = entry.getKey().split("_")[1];
				double time = Double.parseDouble(params.get(TIME + num));
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

