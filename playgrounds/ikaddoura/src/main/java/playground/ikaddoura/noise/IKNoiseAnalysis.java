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

package playground.ikaddoura.noise;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.noise.events.NoiseEventsReader;
import org.matsim.contrib.noise.utils.NoiseEventAnalysisHandler;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;

public class IKNoiseAnalysis {

	static String eventsFile = "/Users/ihab/Desktop/ils4i/kaddoura/cn_cordon/output/cordonToll_0/noiseAnalysis-withEvents/analysis_it.100/100.events_NoiseImmission_Offline.xml.gz";
				
	public static void main(String[] args) {
		IKNoiseAnalysis anaMain = new IKNoiseAnalysis();
		anaMain.run();
	}

	private void run() {
	
		EventsManager events = EventsUtils.createEventsManager();
		
		NoiseEventAnalysisHandler handler1 = new NoiseEventAnalysisHandler();
		events.addHandler(handler1);
		// add more handlers here
		
		NoiseEventsReader reader = new NoiseEventsReader(events);
		reader.readFile(eventsFile);
		
		double totalCost = 0.;
		for (Id<Person> personId : handler1.getPersonId2affectedNoiseCost().keySet()) {
			totalCost+=handler1.getPersonId2affectedNoiseCost().get(personId);
		}
		System.out.println("Total affected noise cost: " + totalCost);
					
	}
			 
}
		

