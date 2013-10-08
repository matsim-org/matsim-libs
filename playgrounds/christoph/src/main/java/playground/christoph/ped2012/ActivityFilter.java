/* *********************************************************************** *
 * project: matsim
 * ActivityFilter.java
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

package playground.christoph.ped2012;

import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.Event;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.events.handler.BasicEventHandler;

public class ActivityFilter implements BasicEventHandler {
	
	private EventWriterXML eventWriter;
	
	public static void main(String[] args) {
//		String inputFile = "/home/cdobler/workspace/matsim/mysimulations/ped2012/output/ITERS/it.0/0.events.xml.gz";
//		String outputFile = "/home/cdobler/workspace/matsim/mysimulations/ped2012/output/ITERS/it.0/0.events_activities.xml.gz";

		String inputFile = "D:/Users/Christoph/workspace/matsim/mysimulations/ped2012/output/ITERS/it.0/0.events.xml.gz";
		String outputFile = "D:/Users/Christoph/workspace/matsim/mysimulations/ped2012/output/ITERS/it.0/0.events_activities.xml.gz";
		
		new ActivityFilter().filterFile(inputFile, outputFile);
	}
			
	public void filterFile(String inputEventsFile, String outputEventsFile) {		
		
		eventWriter = new EventWriterXML(outputEventsFile);
		EventsManager eventsManager = EventsUtils.createEventsManager();
		MatsimEventsReader eventReader = new MatsimEventsReader(eventsManager);
		
		eventsManager.addHandler(this);
		eventReader.readFile(inputEventsFile);
		eventWriter.closeFile();
	}
	
	@Override
	public void reset(int iteration) {
		eventWriter.reset(iteration);
	}

	@Override
	public void handleEvent(Event event) {

		// If it is an activity event, write it to the output file, otherwise skip it.
		if (event instanceof ActivityStartEvent || event instanceof ActivityEndEvent) {
			eventWriter.handleEvent(event);
		} else return;
	}
}