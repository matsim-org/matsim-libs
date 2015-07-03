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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;

import playground.agarwalamit.munich.utils.ExtendedPersonFilter;
import playground.vsp.congestion.events.CongestionEvent;
import playground.vsp.congestion.handlers.CongestionEventHandler;

/**
 * @author amit
 */

public class CausedDelayHandler implements CongestionEventHandler {

	public CausedDelayHandler(Scenario scenario, int noOfTimeBin) {
		double simulatioEndTime = scenario.getConfig().qsim().getEndTime();
		this.timeBinSize = simulatioEndTime /noOfTimeBin;
		this.network = scenario.getNetwork();
		pf = new ExtendedPersonFilter();
		initialize(noOfTimeBin, scenario);
	}
	
	public CausedDelayHandler(Scenario scenario, int noOfTimeBin, boolean sortingForInsideMunich) {
		double simulatioEndTime = scenario.getConfig().qsim().getEndTime();
		this.timeBinSize = simulatioEndTime /noOfTimeBin;
		this.network = scenario.getNetwork();
		this.isSortingForInsideMunich = sortingForInsideMunich;
		pf  = new ExtendedPersonFilter(isSortingForInsideMunich);
		
		initialize(noOfTimeBin, scenario);
	}
	
	private void initialize(int noOfTimeBin, Scenario scenario) {
		for (int i=0;i<noOfTimeBin;i++){
			this.timeBin2Link2DelayCaused.put(this.timeBinSize*(i+1), new HashMap<Id<Link>,Double>());
			this.timeBin2Person2DelayCaused.put(this.timeBinSize*(i+1), new HashMap<Id<Person>,Double>());
			this.timeBin2Link2Persons.put(this.timeBinSize*(i+1), new HashMap<Id<Link>,Set<Id<Person>>>());
			this.timeBin2ListOfTollPayers.put(this.timeBinSize*(i+1), new HashSet<Id<Person>>());
			
			Map<Id<Link>,Double> link2del = this.timeBin2Link2DelayCaused.get(this.timeBinSize*(i+1));
			Map<Id<Link>, Set<Id<Person>>> link2CausingPersonCount = this.timeBin2Link2Persons.get(this.timeBinSize*(i+1));
			for (Id<Link> linkId : this.network.getLinks().keySet()){
				link2del.put(linkId, 0.);
				link2CausingPersonCount.put(linkId, new HashSet<Id<Person>>());
			}
			
			Map<Id<Person>,Double> person2del = this.timeBin2Person2DelayCaused.get(this.timeBinSize*(i+1));				
			for (Id<Person> personId : scenario.getPopulation().getPersons().keySet()){
				person2del.put(personId, 0.);
			}
		}
	}

	private boolean isSortingForInsideMunich = false;
	private final double timeBinSize;
	private SortedMap<Double,Map<Id<Link>,Double>> timeBin2Link2DelayCaused = new TreeMap<Double, Map<Id<Link>,Double>>();
	private SortedMap<Double,Map<Id<Link>,Set<Id<Person>>>> timeBin2Link2Persons = new TreeMap<Double, Map<Id<Link>,Set<Id<Person>>>>();
	private SortedMap<Double,Map<Id<Person>,Double>> timeBin2Person2DelayCaused = new TreeMap<Double, Map<Id<Person>,Double>>();
	
	/**
	 * Required to get timeBin2 userGroup2 tolledTrips
	 */
	private SortedMap<Double,Set<Id<Person>>> timeBin2ListOfTollPayers = new TreeMap<>();
	
	private Network network;
	private final ExtendedPersonFilter pf;
	
	@Override
	public void reset(int iteration) {
		this.timeBin2Link2DelayCaused.clear();
		this.timeBin2Person2DelayCaused.clear();
		this.timeBin2Link2Persons.clear();
		this.timeBin2ListOfTollPayers.clear();
	}

	@Override
	public void handleEvent(CongestionEvent event) {
		
		Double time = event.getTime(); 
		if(time ==0.0) time = this.timeBinSize;
		double endOfTimeInterval = 0.0;
		endOfTimeInterval = Math.ceil(time/this.timeBinSize)*this.timeBinSize;
		if(endOfTimeInterval<=0.0)endOfTimeInterval=this.timeBinSize;

		Id<Link> linkId = event.getLinkId();
		
		Coord linkCoord = this.network.getLinks().get(linkId).getCoord();
		if(isSortingForInsideMunich && (!pf.isCellInsideMunichCityArea(linkCoord))) return;
		
		Id<Person> causingAgentId = event.getCausingAgentId();

		//tolled trip count --> causing agent 
		Set<Id<Person>> tollPayers = this.timeBin2ListOfTollPayers.get(endOfTimeInterval);
		tollPayers.add(causingAgentId);
		
		//person count (toll payers) == there is no meaning of counting congestion events
		Set<Id<Person>> personSet = this.timeBin2Link2Persons.get(endOfTimeInterval).get(linkId);
		personSet.add(causingAgentId);
		
		//causing person delay
		Map<Id<Person>,Double> causingPerson2delay = this.timeBin2Person2DelayCaused.get(endOfTimeInterval);
		causingPerson2delay.put(causingAgentId, causingPerson2delay.get(causingAgentId) + event.getDelay());
		
		//link delays
		Map<Id<Link>,Double> link2delay = this.timeBin2Link2DelayCaused.get(endOfTimeInterval);
		link2delay.put(linkId, link2delay.get(linkId) + event.getDelay());
	}

	public SortedMap<Double, Map<Id<Link>, Double>> getTimeBin2Link2Delay() {
		return timeBin2Link2DelayCaused;
	}

	/**
	 * @return  set of UNIQUE causing persons (toll payers) on each link in each time bin
	 */
	public SortedMap<Double, Map<Id<Link>, Set<Id<Person>>>> getTimeBin2Link2CausingPersons() {
		return timeBin2Link2Persons;
	}

	public SortedMap<Double, Map<Id<Person>, Double>> getTimeBin2CausingPerson2Delay() {
		return timeBin2Person2DelayCaused;
	}

	/**
	 * @return  set of UNIQUE causing persons (toll payers)  in each time bin
	 */
	public SortedMap<Double,Set<Id<Person>>> getTimeBin2CausingPersons() {
		return timeBin2ListOfTollPayers;
	}
}
