/* *********************************************************************** *
 * project: org.matsim.*
 * EventsAnalysis.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.ikaddoura.busCorridor.events;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;

/**
 * @author Ihab
 *
 */
public class TestEventFileReader {

	public static void main(String[] args) {
		
		String eventFile = "../../shared-svn/studies/ihab/busCorridor/output_version1/outputSimulation/extITERS/extIt.0/outputInternalSimulation/ITERS/it.0/0.events.xml.gz";
		EventsManager events = (EventsManager) EventsUtils.createEventsManager();

		TestEventHandler handler = new TestEventHandler();
		events.addHandler(handler);	
		
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(eventFile);
		
		System.out.println("Events file "+eventFile+" read!");
	}

}
