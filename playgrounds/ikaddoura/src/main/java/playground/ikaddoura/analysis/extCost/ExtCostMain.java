/* *********************************************************************** *
* project: org.matsim.*
* firstControler
* *
* *********************************************************************** *
* *
* copyright : (C) 2007 by the members listed in the COPYING, *
* LICENSE and WARRANTY file. *
* email : info at matsim dot org *
* *
* *********************************************************************** *
* *
* This program is free software; you can redistribute it and/or modify *
* it under the terms of the GNU General Public License as published by *
* the Free Software Foundation; either version 2 of the License, or *
* (at your option) any later version. *
* See also COPYING, LICENSE and WARRANTY file *
* *
* *********************************************************************** */ 

package playground.ikaddoura.analysis.extCost;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;

public class ExtCostMain {
	
	private String eventsFile = "/Users/Ihab/Desktop/events_0.725_3min.xml.gz";
	private String outputFolder = "/Users/Ihab/Desktop/constFare_3min_0.725";
	
	public static void main(String[] args) {
		ExtCostMain anaMain = new ExtCostMain();
		anaMain.run();
	}

	private void run() {
	

		EventsManager events = EventsUtils.createEventsManager();
		
		ExtCostEventHandler handler1 = new ExtCostEventHandler();
		events.addHandler(handler1);

		System.out.println("Reading events file...");

		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(eventsFile);
		
		System.out.println("Reading events file... Done.");
		
		System.out.println("Writing output files...");

		PtTripInfoWriter writer = new PtTripInfoWriter(handler1, outputFolder);
		writer.writeResults1();
		writer.writeAvgFares1();
		writer.writeAvgFares2();
		writer.writeAvgFares3();
		writer.writeAvgFares4();
		writer.writeAvgFares5();
		
		System.out.println("Writing output files... Done.");

	}
			 
}
		

