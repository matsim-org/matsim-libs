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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.utils.io.IOUtils;

import playground.agarwalamit.analysis.congestion.CrossMarginalCongestionEventsWriter;
import playground.agarwalamit.utils.LoadMyScenarios;
import playground.vsp.congestion.events.CongestionEvent;
import playground.vsp.congestion.events.CongestionEventsReader;
import playground.vsp.congestion.handlers.CongestionEventHandler;


/**
 * @author amit
 */

public class CompareCongestionEvents  {

	private final String eventsFileV3 = "/Users/amit/Documents/repos/runs-svn/siouxFalls/run203/implV3/ITERS/it.1000/1000.events.xml.gz";
	private final String eventsFileV4 = "/Users/amit/Documents/repos/runs-svn/siouxFalls/run203/implV3/ITERS/it.1000/1000.events_implV4.xml.gz";

	private List<CongestionEvent>  getCongestionEvents (String eventsFile){

		final List<CongestionEvent> congestionevents = new ArrayList<>();

		EventsManager eventsManager = EventsUtils.createEventsManager();
		CongestionEventsReader reader = new CongestionEventsReader(eventsManager);

		eventsManager.addHandler(new CongestionEventHandler () {
			@Override
			public void reset(int iteration) {
				congestionevents.clear();
			}

			@Override
			public void handleEvent(CongestionEvent event) {
				congestionevents.add(event);
			}
		});
		reader.readFile(eventsFile);

		return congestionevents;
	}

	public static void main(String[] args) {
		//		new CompareCongestionEvents().run("/Users/amit/Documents/repos/runs-svn/siouxFalls/run203/analysis/");
		new CompareCongestionEvents().compareTwoImplForSameRun();
	}

	private List<String> wronglyChargedEventsList ;
	
	private void compareTwoImplForSameRun(){

		List<String> eventsImpl3List = eventList2StringList(getCongestionEvents(eventsFileV3));
		
		String runDir = "/Users/amit/Documents/repos/runs-svn/siouxFalls/run203/implV3/";
		Scenario scenario = LoadMyScenarios.loadScenarioFromOutputDir(runDir);
		
		CrossMarginalCongestionEventsWriter w =	new CrossMarginalCongestionEventsWriter(scenario);
		w.readAndWrite("implV4");
		
		List<String> eventsImpl4List = eventList2StringList(w.getCongestionEventsList());

		System.out.println("V3 list size"+eventsImpl3List.size());
		System.out.println("V4 list size"+eventsImpl4List.size());

		Set<String> eventsImpl3 = new LinkedHashSet<>();
		eventsImpl3.addAll(eventsImpl3List);

		Set<String> eventsImpl4 = new LinkedHashSet<>();
		eventsImpl4.addAll(eventsImpl4List);

		System.out.println("V3 set size"+eventsImpl3.size());
		System.out.println("V4 set size"+eventsImpl4.size());

		wronglyChargedEventsList = new ArrayList<>();
		
		for(String e3 : eventsImpl3){
			if(eventsImpl4.contains(e3)){
				eventsImpl4.remove(e3);
			} else {
				wronglyChargedEventsList.add(e3);
			}
		}
		
		System.out.println("Wrong events are "+wronglyChargedEventsList.size());
		System.out.println("Uncharged events are "+eventsImpl4.size());
		checkWrongEventsList();
	}
	
	private void checkWrongEventsList(){
		double wrongDelays = 0;
		Set<String> causingPersons = new HashSet<>();
		Set<String> affectedPersons = new HashSet<>();
		Set<Double> delays = new HashSet<>();
		
		for(String e:wronglyChargedEventsList){
			
			String causingPerson = e.split(" ")[4];
			String affectedPerson = e.split(" ")[5];
			causingPersons.add(causingPerson);
			affectedPersons.add(affectedPerson);
			
			String delay = (e.split(" ")[6]);
			String delayNumber = delay.substring(7,delay.length()-1);
			
			double d = Double.valueOf(delayNumber);
			delays.add(d);
			wrongDelays +=d;
		}
		
		System.out.println("Wrongly charged delays in hr is "+wrongDelays/3600);
		System.out.println("In wrongly events affected persons are "+ affectedPersons.size());
		System.out.println("In wrongly events causing persons are "+ causingPersons.size());
	}

	private List<String> eventList2StringList(List<CongestionEvent> l){
		List<String> outList = new ArrayList<>();

		for(CongestionEvent e :l){
			outList.add(e.toString());	
		}
		return outList;
	}

	private void run(String outputFolder){

		List<CongestionEvent> eventsImpl3 = getCongestionEvents(eventsFileV3);
		List<CongestionEvent> eventsImpl4 = getCongestionEvents(eventsFileV4);

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

	private Set<Id<Person>> getAffectedPersons(List<CongestionEvent> mce){
		Set<Id<Person>> affectedPersons = new HashSet<>();
		for(CongestionEvent e : mce) {
			affectedPersons.add(e.getAffectedAgentId());
		}
		return affectedPersons;
	}

	private Set<Id<Person>> getCausingPersons(List<CongestionEvent> mce){
		Set<Id<Person>> causingPersons = new HashSet<>();
		for(CongestionEvent e : mce) {
			causingPersons.add(e.getCausingAgentId());
		}
		return causingPersons;
	}

}
