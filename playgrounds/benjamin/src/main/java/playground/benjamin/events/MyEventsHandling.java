/* *********************************************************************** *
 * project: org.matsim.*
 * EventsReader
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.benjamin.events;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.MatsimEventsReader;




/**
 * This class contains a main method to call the 
 * example event handlers MyEventHandler1-3.
 * 
 * @author dgrether
 */
public class MyEventsHandling {

	
	
	public static void main(String[] args) {
		
		//path to events file
		String inputFile = "output/multipleIterations/ITERS/it.10/10.events.xml.gz";
		//create an event object
		EventsManager events = new EventsManagerImpl();
		
		
		//create the handler and add it
//		MyEventHandler1 handler = new MyEventHandler1();
		
//		MyEventHandler2 handler = new MyEventHandler2(500);
		
		MyEventHandler3 handler = new MyEventHandler3();
		
		events.addHandler(handler);
		
		
		//create the reader and read the file
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(inputFile);
		
		//MyEventHandler2 has method getAverageTravelTime
//		System.out.println("average travel time: " + handler.getAverageTravelTime());
		
//		MyEventHandler2 has method writeChart
		handler.writeChart("output/multipleIterations/departuresPerHour.png");
		handler.writeTxt("output/multipleIterations/departuresPerHour.txt");
		
		
		System.out.println("Events file read!");
	}

}
