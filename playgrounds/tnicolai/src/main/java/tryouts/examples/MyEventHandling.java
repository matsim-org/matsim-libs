/* *********************************************************************** *
 * project: org.matsim.*
 * MyEventHandler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

/**
 * 
 */
package tryouts.examples;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;

/**
 * @author thomas
 *
 */
public class MyEventHandling {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		//path to events file
		String inputFile = "/Users/thomas/Development/workspace/matsim/output/example5/ITERS/it.100/100.events.xml.gz";
		//create an EventsManager object
		EventsManager events = (EventsManager) EventsUtils.createEventsManager();
		//create the handler and add it
//		MyEventHandler1 handler = new MyEventHandler1();
//		MyEventHandler2 handler = new MyEventHandler2(2000);
		MyEventHandler3 handler = new MyEventHandler3();
		events.addHandler(handler);
		//create the reader and read the file
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(inputFile);
		
		// only available in MyEventHandler2
//		System.out.println("Total travel time: " + handler.getTotalTravelTime());
//		System.out.println("Avarage travel time for 2000 agents: " + handler.getAverageTravelTime());
		
		// only available in MyEventHandler3
		handler.writeChart("/Users/thomas/Development/workspace/matsim/output/chart.png");
		
		System.out.println("Events file read!");
	}

}

