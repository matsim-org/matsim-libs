/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.agarwalamit.congestionPricing.analysis;

import java.io.BufferedWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.utils.io.IOUtils;

import playground.ikaddoura.internalizationCar.MarginalCongestionEvent;
import playground.ikaddoura.internalizationCar.MarginalCongestionEventHandler;
import playground.ikaddoura.internalizationCar.MarginalCongestionEventsReader;


/**
 * @author amit
 */

public class CompareCongestionEvents  {

	private String eventsFile_v3 = "/Users/amit/Documents/repos/runs-svn/siouxFalls/run203/implV3/ITERS/it.1000/1000.events.xml.gz";
	private String eventsFile_v4 = "/Users/amit/Documents/repos/runs-svn/siouxFalls/run203/implV4/ITERS/it.1000/1000.events.xml.gz";

	private List<MarginalCongestionEvent>  getCongestionEvents (String eventsFile){

		final List<MarginalCongestionEvent> congestionevents = new ArrayList<MarginalCongestionEvent>();

		EventsManager eventsManager = EventsUtils.createEventsManager();
		MarginalCongestionEventsReader reader = new MarginalCongestionEventsReader(eventsManager);

		eventsManager.addHandler(new MarginalCongestionEventHandler () {
			@Override
			public void reset(int iteration) {
				congestionevents.clear();
			}

			@Override
			public void handleEvent(MarginalCongestionEvent event) {
				congestionevents.add(event);
			}
		});
		reader.parse(eventsFile);

		return congestionevents;
	}

	public static void main(String[] args) {
		new CompareCongestionEvents().run("/Users/amit/Documents/repos/runs-svn/siouxFalls/run203/analysis/");
	} 

	private void run(String outputFolder){

 		List<MarginalCongestionEvent> eventsImpl3 = getCongestionEvents(eventsFile_v3);
		List<MarginalCongestionEvent> eventsImpl4 = getCongestionEvents(eventsFile_v4);

		BufferedWriter writer = IOUtils.getBufferedWriter(outputFolder+"/congestionEventsInfo.txt");
		try {
			writer.write("Particulars \t implV3 \t implV4 \n");
			
			writer.write("number of congestion events \t "+eventsImpl3.size()+"\t"+eventsImpl4.size()+"\n");
			
			writer.write("number of affected persons \t "+getAffectedPersons(eventsImpl3).size()+"\t"+getAffectedPersons(eventsImpl4).size()+"\n");
			writer.write("number of causing persons \t "+getCausingPersons(eventsImpl3).size()+"\t"+getCausingPersons(eventsImpl4).size()+"\n");
			writer.close();
		} catch (Exception e) {
			throw new RuntimeException("Data is not written in file. Reason: "
					+ e);
		}
	}
	
	private Set<Id<Person>> getAffectedPersons(List<MarginalCongestionEvent> mce){
		Set<Id<Person>> affectedPersons = new HashSet<Id<Person>>();
		for(MarginalCongestionEvent e : mce) {
			affectedPersons.add(e.getAffectedAgentId());
		}
		return affectedPersons;
	}
	
	private Set<Id<Person>> getCausingPersons(List<MarginalCongestionEvent> mce){
		Set<Id<Person>> causingPersons = new HashSet<Id<Person>>();
		for(MarginalCongestionEvent e : mce) {
			causingPersons.add(e.getCausingAgentId());
		}
		return causingPersons;
	}
	
}
