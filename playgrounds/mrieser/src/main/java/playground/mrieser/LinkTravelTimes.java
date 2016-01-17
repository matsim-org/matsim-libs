/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.mrieser;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.core.utils.misc.Time;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;

public class LinkTravelTimes {

	public static void main(String[] args) {
		String events1Filename = "/path/to/events1.xml.gz";
		String events2Filename = "/path/to/events2.xml.gz";
		String networkFilename = "/path/to/network.xml.gz";
		String attributesFilename = "/path/to/travelTimeDifferences_LinkAttributes.xml.gz";
		
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);

		System.out.println("Loading network " + networkFilename);
		
		new MatsimNetworkReader(scenario.getNetwork()).readFile(networkFilename);
		
		int binSize = 30*60; // 30 minutes
		int maxTime = 24 * 3600; // 24 hours
		
		System.out.println("Loading events " + events1Filename);

		EventsManager events1 = EventsUtils.createEventsManager();
		TravelTimeCalculator ttc1 = new TravelTimeCalculator(scenario.getNetwork(), binSize, maxTime, config.travelTimeCalculator());
		events1.addHandler(ttc1);
		new MatsimEventsReader(events1).readFile(events1Filename);
		
		System.out.println("Loading events " + events2Filename);

		EventsManager events2 = EventsUtils.createEventsManager();
		TravelTimeCalculator ttc2 = new TravelTimeCalculator(scenario.getNetwork(), binSize, maxTime, config.travelTimeCalculator());
		events2.addHandler(ttc2);
		new MatsimEventsReader(events2).readFile(events2Filename);
		
		System.out.println("Filling in link attributes");

		ObjectAttributes oa = new ObjectAttributes();
		for (Link link : scenario.getNetwork().getLinks().values()) {
			for (double time = 0.0; time < maxTime; time += binSize) {
				double traveltime1 = ttc1.getLinkTravelTime(link.getId(), time);
				double traveltime2 = ttc2.getLinkTravelTime(link.getId(), time);
				String attributeName = Time.writeTime(time, Time.TIMEFORMAT_HHMM);
				double value = traveltime1 - traveltime2;
				oa.putAttribute(link.getId().toString(), attributeName, value);
			}
		}
		
		System.out.println("Writing out link attributes to " + attributesFilename);
		new ObjectAttributesXmlWriter(oa).writeFile(attributesFilename);

		System.out.println("Finished.");
	}
}
