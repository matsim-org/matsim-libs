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
package playground.jbischoff.teach.events;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;




/**
 * This class contains a main method to call 
 *  event handlers using post-processing
 * 
 * @author jbischoff
 */
public class RunEventsHandlingExample {

	
	
	public static void main(String[] args) {

		//path to events file
		String inputFile = "output/nullfall/ITERS/it.50/50.events.xml.gz";

		//create an event object
		EventsManager events = EventsUtils.createEventsManager();

		//create the handler and add it
		CityCenterEventEnterHandler cityCenterEventEnterHandler = new CityCenterEventEnterHandler();

		//add the links here that you want to monitor
		cityCenterEventEnterHandler.addLinkId(Id.createLinkId(28112));
		
		
		events.addHandler(cityCenterEventEnterHandler);


        //create the reader and read the file
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(inputFile);
		
		System.out.println(cityCenterEventEnterHandler.getVehiclesInCityCenter());
		
		System.out.println("Events file read!");
	}

}