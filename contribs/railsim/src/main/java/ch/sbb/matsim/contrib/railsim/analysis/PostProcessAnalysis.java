/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2023 by the members listed in the COPYING,        *
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

package ch.sbb.matsim.contrib.railsim.analysis;

import ch.sbb.matsim.contrib.railsim.analysis.linkstates.RailLinkStateAnalysis;
import ch.sbb.matsim.contrib.railsim.analysis.trainstates.TrainStateAnalysis;
import ch.sbb.matsim.contrib.railsim.eventmappers.RailsimLinkStateChangeEventMapper;
import ch.sbb.matsim.contrib.railsim.eventmappers.RailsimTrainStateEventMapper;
import ch.sbb.matsim.contrib.railsim.events.RailsimLinkStateChangeEvent;
import ch.sbb.matsim.contrib.railsim.events.RailsimTrainStateEvent;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * Class to generate rail sim csv files from event file.
 */
public final class PostProcessAnalysis {

	private PostProcessAnalysis() {
	}

	public static void main(String[] args) {
		String eventsFilename;
		String networkFilename = null;
		if (args.length > 0) {
			eventsFilename = args[0];
		} else {
			System.err.println("Please provide events filename.");
			System.exit(2);
			return;
		}
		if (args.length > 1) {
			networkFilename = args[1];
		}

		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		if (networkFilename != null) {
			new MatsimNetworkReader(scenario.getNetwork()).readFile(networkFilename);
		}

		RailLinkStateAnalysis linkStateAnalysis = new RailLinkStateAnalysis();
		TrainStateAnalysis trainStateAnalysis = new TrainStateAnalysis();

		EventsManager events = EventsUtils.createEventsManager();
		events.addHandler(linkStateAnalysis);
		events.addHandler(trainStateAnalysis);
		events.initProcessing();

		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.addCustomEventMapper(RailsimLinkStateChangeEvent.EVENT_TYPE, new RailsimLinkStateChangeEventMapper());
		reader.addCustomEventMapper(RailsimTrainStateEvent.EVENT_TYPE, new RailsimTrainStateEventMapper());
		reader.readFile(eventsFilename);

		events.finishProcessing();

		RailsimCsvWriter.writeLinkStatesCsv(linkStateAnalysis.getEvents(), "railsimLinkStates.csv");
		RailsimCsvWriter.writeTrainStatesCsv(trainStateAnalysis.getEvents(), scenario.getNetwork(), "railsimTrainStates.csv");
	}
}
