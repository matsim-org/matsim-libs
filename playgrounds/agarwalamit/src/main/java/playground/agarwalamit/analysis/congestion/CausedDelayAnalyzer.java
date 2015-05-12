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
package playground.agarwalamit.analysis.congestion;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;

import playground.vsp.congestion.events.CongestionEvent;
import playground.vsp.congestion.events.CongestionEventsReader;
import playground.vsp.congestion.handlers.CongestionEventHandler;

/**
 * Calculates the delay caused for each person by reading the marginal congestion events.
 * Additionally, returns the link delay and counts which can be used to get the (avg) toll, since a corresponding 
 * person is charged for every marginal congestion event.
 * @author amit
 */

public class CausedDelayAnalyzer {

	public CausedDelayAnalyzer(String eventsFile, Scenario scenario, int noOfTimeBin) {
		this.eventsFile = eventsFile;
		handler = new CausedDelayHandler(scenario, noOfTimeBin);
	}

	private CongestionEventsReader reader;
	private CausedDelayHandler handler;
	private String eventsFile;

	public void run(){
		
		EventsManager eventsManager = EventsUtils.createEventsManager();
		reader = new CongestionEventsReader(eventsManager);

		eventsManager.addHandler(handler);
		reader.parse(this.eventsFile);
	}

	public SortedMap<Double, Map<Id<Person>, Double>> getTimeBin2CausingPersonId2Delay() {
		return handler.getTimeBin2CausingPerson2Delay();
	}

	public SortedMap<Double, Map<Id<Link>, Double>> getTimeBin2LinkId2Delay() {
		return handler.getTimeBin2Link2Delay();
	}
	
	public SortedMap<Double, Map<Id<Link>, Integer>> getTimeBin2Link2PersonCount(){
		return handler.getTimeBin2Link2PersonCount();
	}


	public class CausedDelayHandler implements CongestionEventHandler {

		public CausedDelayHandler(Scenario scenario, int noOfTimeBin) {
			double simulatioEndTime = scenario.getConfig().qsim().getEndTime();
			this.timeBinSize = simulatioEndTime /noOfTimeBin;

			for (int i=0;i<noOfTimeBin;i++){
				this.timeBin2Link2DelayCaused.put(this.timeBinSize*(i+1), new HashMap<Id<Link>,Double>());
				this.timeBin2Person2DelayCaused.put(this.timeBinSize*(i+1), new HashMap<Id<Person>,Double>());
				this.timeBin2Link2PersonCount.put(this.timeBinSize*(i+1), new HashMap<Id<Link>,Integer>());

				Map<Id<Link>,Double> link2del = this.timeBin2Link2DelayCaused.get(this.timeBinSize*(i+1));
				Map<Id<Link>, Integer> link2CausingPersonCount = this.timeBin2Link2PersonCount.get(this.timeBinSize*(i+1));
				for (Id<Link> linkId : scenario.getNetwork().getLinks().keySet()){
					link2del.put(linkId, 0.);
					link2CausingPersonCount.put(linkId, 0);
				}
				
				Map<Id<Person>,Double> person2del = this.timeBin2Person2DelayCaused.get(this.timeBinSize*(i+1));				
				for (Id<Person> personId : scenario.getPopulation().getPersons().keySet()){
					person2del.put(personId, 0.);
				}
			}
		}

		private Map<Id<Person>, Double> personId2DelayCaused = new HashMap<>();
		private Map<Id<Link>, Double> linkId2DelayCaused = new HashMap<>();
		private final double timeBinSize;
		private SortedMap<Double,Map<Id<Link>,Double>> timeBin2Link2DelayCaused = new TreeMap<Double, Map<Id<Link>,Double>>();
		private SortedMap<Double,Map<Id<Link>,Integer>> timeBin2Link2PersonCount = new TreeMap<Double, Map<Id<Link>,Integer>>();
		private SortedMap<Double,Map<Id<Person>,Double>> timeBin2Person2DelayCaused = new TreeMap<Double, Map<Id<Person>,Double>>();

		@Override
		public void reset(int iteration) {
			this.personId2DelayCaused.clear();
			this.linkId2DelayCaused.clear();
			this.timeBin2Link2DelayCaused.clear();
			this.timeBin2Person2DelayCaused.clear();
			this.timeBin2Link2PersonCount.clear();
		}

		@Override
		public void handleEvent(CongestionEvent event) {
			
			Double time = event.getTime(); 
			if(time ==0.0) time = this.timeBinSize;
			double endOfTimeInterval = 0.0;
			endOfTimeInterval = Math.ceil(time/this.timeBinSize)*this.timeBinSize;
			if(endOfTimeInterval<=0.0)endOfTimeInterval=this.timeBinSize;

			Id<Link> linkId = event.getLinkId();
			Id<Person> causingAgentId = event.getCausingAgentId();

			//person count
			Map<Id<Link>, Integer> link2PersonCount = this.timeBin2Link2PersonCount.get(endOfTimeInterval);
			link2PersonCount.put(linkId,link2PersonCount.get(linkId)+1);
			
			//causing person delay
			Map<Id<Person>,Double> causingPerson2delay = this.timeBin2Person2DelayCaused.get(endOfTimeInterval);
			causingPerson2delay.put(causingAgentId, causingPerson2delay.get(causingAgentId)+event.getDelay());
			
			//link delays
			Map<Id<Link>,Double> link2delay = this.timeBin2Link2DelayCaused.get(endOfTimeInterval);
			link2delay.put(linkId, link2delay.get(linkId)+event.getDelay());
		}

		public SortedMap<Double, Map<Id<Link>, Double>> getTimeBin2Link2Delay() {
			return timeBin2Link2DelayCaused;
		}

		public SortedMap<Double, Map<Id<Link>, Integer>> getTimeBin2Link2PersonCount() {
			return timeBin2Link2PersonCount;
		}

		public SortedMap<Double, Map<Id<Person>, Double>> getTimeBin2CausingPerson2Delay() {
			return timeBin2Person2DelayCaused;
		}

	}

}
