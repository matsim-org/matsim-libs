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
package playground.agarwalamit.analysis.Toll;

import java.io.BufferedWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonMoneyEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.IOUtils;

import playground.agarwalamit.munich.ExtendedPersonFilter;
import playground.benjamin.scenarios.munich.analysis.filter.UserGroup;
import playground.vsp.analysis.modules.AbstractAnalyisModule;

/**
 * @author amit
 */

public class AverageTollAnalyzer extends AbstractAnalyisModule {

	public static void main(String[] args) {
		String congestionImpl = "implV6";
		String eventsFile = "../../../repos/runs-svn/detEval/emissionCongestionInternalization/output/1pct/run12/policies/"+congestionImpl+"/ITERS/it.1500/1500.events.xml.gz";
		String outputFolder = "../../../repos/runs-svn/detEval/emissionCongestionInternalization/output/1pct/run12/policies/"+congestionImpl+"/analysis/";
		AverageTollAnalyzer ata = new AverageTollAnalyzer(eventsFile);
		ata.preProcessData();
		ata.postProcessData();
		ata.writeResults(outputFolder);
	}

	public AverageTollAnalyzer (String eventsFile) {
		super(AverageTollAnalyzer.class.getSimpleName());
		this.eventsFile = eventsFile;
	}

	private String eventsFile;
	private ExtendedPersonFilter pf = new ExtendedPersonFilter();
	private HandlerForAvgToll handler = new HandlerForAvgToll();
	private Map<String, Tuple<Double,Double>> userGrp2TollInfo = new HashMap<>();
	
	@Override
	public List<EventHandler> getEventHandler() {
		return null;
	}

	@Override
	public void preProcessData() {

		EventsManager events = EventsUtils.createEventsManager();
		MatsimEventsReader reader = new MatsimEventsReader(events);
		events.addHandler(handler);
		reader.readFile(eventsFile);
	}

	@Override
	public void postProcessData() {
		Map<UserGroup,List<Id<Person>>> userGroup2AllCarTrips = handler.userGroup2AllCarTrips;
		Map<UserGroup,Set<Id<Person>>> userGroup2TollPayerCount  = handler.userGroup2TollPayer;
		Map<UserGroup,Double> userGroup2Toll = handler.userGroup2Toll;
		
		double totalToll = 0;
		int counterAllPerson = 0;
		int counterTollPayer = 0;
		
		for (UserGroup ug : UserGroup.values()){
			double toll = userGroup2Toll.get(ug);
			double tollPerPeson = toll/userGroup2AllCarTrips.get(ug).size();
			double tollPerPayer = toll/userGroup2TollPayerCount.get(ug).size();
			userGrp2TollInfo.put(ug.toString(), new Tuple<Double, Double>(tollPerPeson, tollPerPayer));
			
			totalToll += toll;
			counterAllPerson += userGroup2AllCarTrips.get(ug).size();
			counterTollPayer += userGroup2TollPayerCount.get(ug).size();
		}
		userGrp2TollInfo.put("total", new Tuple<Double, Double>(totalToll/counterAllPerson,totalToll/counterTollPayer));
	}

	@Override
	public void writeResults(String outputFolder) {
		BufferedWriter writer = IOUtils.getBufferedWriter(outputFolder+"/averageTollInfo.txt");
		try {
			writer.write("UserGroup \t averageTollPerCarTrip \t averageTollPerTollPayer \n");
			
			for(String str : userGrp2TollInfo.keySet()){
				writer.write(str+"\t"+userGrp2TollInfo.get(str).getFirst()+"\t"+userGrp2TollInfo.get(str).getSecond()+"\n");
			}
			writer.close();
		} catch (Exception e) {
			throw new RuntimeException("Data is not written in file. Reason: "
					+ e);
		}
	}

	private class HandlerForAvgToll implements PersonDepartureEventHandler, PersonMoneyEventHandler {

		private Map<UserGroup,List<Id<Person>>> userGroup2AllCarTrips = new HashMap<UserGroup, List<Id<Person>>>();
		private Map<UserGroup,Set<Id<Person>>> userGroup2TollPayer = new HashMap<UserGroup, Set<Id<Person>>>();
		private Map<UserGroup,Double> userGroup2Toll = new HashMap<UserGroup, Double>();
		
		private HandlerForAvgToll () {
			for(UserGroup ug : UserGroup.values()){
				userGroup2AllCarTrips.put(ug, new ArrayList<Id<Person>>());
				userGroup2TollPayer.put(ug, new HashSet<Id<Person>>());
				userGroup2Toll.put(ug, 0.);
			}
		}
		
		@Override
		public void reset(int iteration) {
			userGroup2AllCarTrips.clear();
			userGroup2TollPayer.clear();
			userGroup2Toll.clear();
		}

		@Override
		public void handleEvent(PersonMoneyEvent event) {
			UserGroup ug = pf.getUserGroupFromPersonId(event.getPersonId());
			userGroup2Toll.put(ug, userGroup2Toll.get(ug)+event.getAmount());
			Set<Id<Person>> personIds = userGroup2TollPayer.get(ug);
			personIds.add(event.getPersonId());
		}

		@Override
		public void handleEvent(PersonDepartureEvent event) {
			if(event.getLegMode().equals(TransportMode.car)) {// cant use per car user, must use per car trip
				UserGroup ug = pf.getUserGroupFromPersonId(event.getPersonId());
				List<Id<Person>> personIds = userGroup2AllCarTrips.get(ug);
				personIds.add(event.getPersonId());
			}
		}
	}
}
