/* *********************************************************************** *
 * project: org.matsim.*
 * ReadEvents.java
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

package playground.christoph.analysis.wardrop;

import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.EventsReaderTXTv1;


public class ReadEvents {

	public static void main(String[] args) {
		// Instance which takes over line by line of the events file
		// and throws events of added types
		EventsManagerImpl events = new EventsManagerImpl();
		
		// An example of an events handler which takes
		// "LinkLeaveEvents" to calculate total volumes per link of the network
		ActTimesCollector atc = new ActTimesCollector();
		
		// register the handler to the events object
		events.addHandler(atc);
		
		// Reader to read events line by line and parses it over to the events object
		EventsReaderTXTv1 reader = new EventsReaderTXTv1(events);
		reader.readFile("C:\\events.txt.gz");
		
		// an example output of the DailyLinkVolumeCalc
		System.out.println("Size: " + atc.data.size());
	}

}
