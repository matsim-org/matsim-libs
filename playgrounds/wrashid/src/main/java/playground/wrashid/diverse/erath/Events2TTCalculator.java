/* *********************************************************************** *
 * project: org.matsim.*
 * Events2TTMatrix.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package playground.wrashid.diverse.erath;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsReaderTXTv1;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;


public class Events2TTCalculator {
	
	public static TravelTimeCalculator getTravelTimeCalculator(Scenario scenario, String eventsFile) {
		// reading the network
		//ScenarioImpl scenario = new ScenarioImpl();
		//NetworkLayer network = scenario.getNetwork();
		//new MatsimNetworkReader(scenario).readFile(networkFile);

		TravelTimeCalculator ttc = new TravelTimeCalculator(scenario.getNetwork(),3600,30*3600, scenario.getConfig().travelTimeCalculator());
		//SpanningTree st = new SpanningTree(ttc,new TravelTimeDistanceCostCalculator(ttc, scenario.getConfig().charyparNagelScoring()));
		//TTimeMatrixCalculator ttmc = new TTimeMatrixCalculator(parseL2ZMapping(mapfile),hours,st,network);

		// creating events object and assign handlers
		EventsManager events = (EventsManager) EventsUtils.createEventsManager();
		//events.addHandler(ttmc);
		events.addHandler(ttc);
		
		// reading events.  Will do all the processing as side effect.
		System.out.println("processing events file...");
		EventsReaderTXTv1 reader = new EventsReaderTXTv1(events);
		reader.readFile(eventsFile);
		System.out.println("done.");
		
		return ttc;
	}
}
