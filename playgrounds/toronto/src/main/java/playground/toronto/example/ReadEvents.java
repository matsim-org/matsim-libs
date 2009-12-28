/* *********************************************************************** *
 * project: org.matsim.*
 * LinkLeaveEventHandler.java
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

package playground.toronto.example;

import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.EventsReaderTXTv1;


public class ReadEvents {

	public static void main(String[] args) {
		// Instance which takes over line by line of the events file
		// and throws events of added types
		EventsManagerImpl events = new EventsManagerImpl();
		
		// An example of an events handler which takes
		// "LinkLeaveEvents" to calculate total volumes per link of the network
		DailyLinkVolumeCalc dlvc = new DailyLinkVolumeCalc();
		
		// register the handler to the events object
		events.addHandler(dlvc);
		
		// Reader to read events line by line and paases it over to the events object
		EventsReaderTXTv1 reader = new EventsReaderTXTv1(events);
		reader.readFile("events.txt.gz");
		
		// an example output of the DailyLinkVolumeCalc
		dlvc.writeTable();
	}

}
