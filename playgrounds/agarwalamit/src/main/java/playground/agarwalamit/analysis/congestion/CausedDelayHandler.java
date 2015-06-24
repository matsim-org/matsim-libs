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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;

import playground.agarwalamit.munich.ExtendedPersonFilter;
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

		initialize(noOfTimeBin, scenario);
	}
	
	public CausedDelayHandler(Scenario scenario, int noOfTimeBin, boolean sortingForInsideMunich) {
		double simulatioEndTime = scenario.getConfig().qsim().getEndTime();
		this.timeBinSize = simulatioEndTime /noOfTimeBin;
		this.network = scenario.getNetwork();
		this.isSortingForInsideMunich = sortingForInsideMunich;
		
		initialize(noOfTimeBin, scenario);
	}
	
	private void initialize(int noOfTimeBin, Scenario scenario) {
		for (int i=0;i<noOfTimeBin;i++){
			this.timeBin2Link2DelayCaused.put(this.timeBinSize*(i+1), new HashMap<Id<Link>,Double>());
			this.timeBin2Person2DelayCaused.put(this.timeBinSize*(i+1), new HashMap<Id<Person>,Double>());
			this.timeBin2Link2PersonCount.put(this.timeBinSize*(i+1), new HashMap<Id<Link>,Integer>());
			this.getTimeBin2ListOfTollPayers().put(this.timeBinSize*(i+1), new ArrayList<Id<Person>>());
			
			Map<Id<Link>,Double> link2del = this.timeBin2Link2DelayCaused.get(this.timeBinSize*(i+1));
			Map<Id<Link>, Integer> link2CausingPersonCount = this.timeBin2Link2PersonCount.get(this.timeBinSize*(i+1));
			for (Id<Link> linkId : this.network.getLinks().keySet()){
				link2del.put(linkId, 0.);
				link2CausingPersonCount.put(linkId, 0);
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
	private SortedMap<Double,Map<Id<Link>,Integer>> timeBin2Link2PersonCount = new TreeMap<Double, Map<Id<Link>,Integer>>();
	private SortedMap<Double,Map<Id<Person>,Double>> timeBin2Person2DelayCaused = new TreeMap<Double, Map<Id<Person>,Double>>();
	
	/**
	 * Required to get timeBin2 userGroup2 tolledTrips
	 */
	private SortedMap<Double,List<Id<Person>>> timeBin2ListOfTollPayers = new TreeMap<>();
	
	private Network network;
	private final ExtendedPersonFilter pf = new ExtendedPersonFilter();
	
	@Override
	public void reset(int iteration) {
		this.timeBin2Link2DelayCaused.clear();
		this.timeBin2Person2DelayCaused.clear();
		this.timeBin2Link2PersonCount.clear();
		this.getTimeBin2ListOfTollPayers().clear();
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
		List<Id<Person>> tollPayers = this.getTimeBin2ListOfTollPayers().get(endOfTimeInterval);
		tollPayers.add(event.getCausingAgentId());
		
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

	public SortedMap<Double,List<Id<Person>>> getTimeBin2ListOfTollPayers() {
		return timeBin2ListOfTollPayers;
	}
}
