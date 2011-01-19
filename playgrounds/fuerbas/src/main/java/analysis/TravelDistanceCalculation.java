/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package analysis;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.MatsimEventsReader;

public class TravelDistanceCalculation {

	/**
	 * @param args
	 */
	
	
	public static void main(String[] args) {
		//path to events file
//		String inputFile = "/home01/sfuerbas/workspace/893.2200.events.txt.gz";
		
		String inputFile = args[0];
		
		//create an event object
		EventsManager events = new EventsManagerImpl();
		
		//create the handler
		TravelDistanceHandler handler = new TravelDistanceHandler();	
		handler.createHandlerScenario();
		
		//add the handler
		events.addHandler(handler);
		
		//create the reader and read the file
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(inputFile);
		
		handler.returnDistances();


	}

}
