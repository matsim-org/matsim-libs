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
package playground.ikaddoura.analysis.arrivalPatterns;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.handler.EventHandler;

/**
 * @author lkroeger, ikaddoura
 *
 */
public class ArrivalPatternsAnalysisMain {
	
	public static void main(String[] args) {
		
		String inputEventsFile = "/Users/Ihab/Desktop/300.events_180sec.xml";
		String outputFolder = "/Users/Ihab/Desktop/arrivalPatternsAnalysis_180/";
		
		EventsManager eventsManager = EventsUtils.createEventsManager();
		
		TripCounter handler = new TripCounter();
		eventsManager.addHandler((EventHandler) handler);
		
		MatsimEventsReader reader = new MatsimEventsReader(eventsManager);
		reader.readFile(inputEventsFile);
		
		handler.printCounts(outputFolder);
		
	}
	
}