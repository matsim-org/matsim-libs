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
package tutorial.example6EventsHandling;

import org.matsim.core.events.*;




/**
 * This class contains a main method to call the 
 * example event handlers MyEventHandler1-3.
 * 
 * @author dgrether
 */
public class MyEventsHandling {

	
	
	public static void main(String[] args) {
		System.err.println("Although this example structurally comes before example7, as of now (may09) you need to run example7 first "
				+ "so that you have the needed files in ./output." ) ;
		
		//path to events file
		String inputFile = "output/ITERS/it.10/10.events.txt.gz";
		//create an event object
		EventsManagerImpl events = new EventsManagerImpl();
		//create the handler and add it
		MyEventHandler1 handler = new MyEventHandler1();
//		MyEventHandler2 handler = new MyEventHandler2(500);
//		MyEventHandler3 handler = new MyEventHandler3();
		events.addHandler(handler);
		//create the reader and read the file
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(inputFile);
		
//		System.out.println("average travel time: " + handler.getAverageTravelTime());
//		handler.writeChart("output/departuresPerHour.png");
		
		System.out.println("Events file read!");
	}

}
