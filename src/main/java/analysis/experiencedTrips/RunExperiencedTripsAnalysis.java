/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
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

package analysis.experiencedTrips;

import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;

public class RunExperiencedTripsAnalysis {
	public static void main(String[] args) {
		
		String runDirectory = "C:/Users/Joschka/Documents/shared-svn/projects/vw_rufbus/projekt2/drt_test_Scenarios/BS_DRT/output/0.1_drt_100veh";
		String runId = "0.1_drt_100veh.";
		String runPrefix = runDirectory+"/"+runId;
		
		Set<String> monitoredModes = new HashSet<>();
		monitoredModes.add("pt");
		monitoredModes.add("drt");
		monitoredModes.add("drt_walk");
		monitoredModes.add("transit_walk");
		
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario.getNetwork()).readFile(runPrefix+"output_network.xml.gz");
		new TransitScheduleReader(scenario).readFile(runPrefix+"output_transitSchedule.xml.gz");
		
				
		// Analysis
		EventsManager events = EventsUtils.createEventsManager();
		
		
		Set<Id<Link>> monitoredStartAndEndLinks = new HashSet<>();
		
		DrtPtTripEventHandler eventHandler = new DrtPtTripEventHandler(scenario.getNetwork(), scenario.getTransitSchedule(), 
				monitoredModes, monitoredStartAndEndLinks);
		events.addHandler(eventHandler);
		new DrtEventsReader(events).readFile(runPrefix + "output_events.xml.gz");
		System.out.println("Start writing trips of " + eventHandler.getPerson2ExperiencedTrips().size() + " agents.");
		ExperiencedTripsWriter tripsWriter = new ExperiencedTripsWriter(runPrefix+
				"experiencedTrips.csv", 
				eventHandler.getPerson2ExperiencedTrips(), monitoredModes,scenario.getNetwork());
		tripsWriter.writeExperiencedTrips();
		ExperiencedTripsWriter legsWriter = new ExperiencedTripsWriter(runPrefix + 
				"experiencedLegs.csv", 
				eventHandler.getPerson2ExperiencedTrips(), monitoredModes,scenario.getNetwork());
		legsWriter.writeExperiencedLegs();
	}

}
