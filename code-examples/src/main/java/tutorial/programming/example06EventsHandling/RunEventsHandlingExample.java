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
package tutorial.programming.example06EventsHandling;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;




/**
 * This class contains a main method to call the 
 * example event handlers MyEventHandler1-3.
 * 
 * @author dgrether
 */
public class RunEventsHandlingExample {

	
	
	public static void main(String[] args) {

		//path to events file
		String inputFile = "output/example5/ITERS/it.10/10.events.xml.gz";

		//create an event object
		EventsManager events = EventsUtils.createEventsManager();

		//create the handler and add it
		MyEventHandler1 handler1 = new MyEventHandler1();
		MyEventHandler2 handler2 = new MyEventHandler2();
		MyEventHandler3 handler3 = new MyEventHandler3();
		events.addHandler(handler1);
		events.addHandler(handler2);
		events.addHandler(handler3);
		
        //create the reader and read the file
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(inputFile);
		
		System.out.println("average travel time: " + handler2.getTotalTravelTime());
		handler3.writeChart("output/departuresPerHour.png");
		
		System.out.println("Events file read!");
	}

}
