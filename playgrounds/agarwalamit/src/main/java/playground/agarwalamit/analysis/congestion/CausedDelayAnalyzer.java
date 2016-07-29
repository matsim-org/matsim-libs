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

import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;

import playground.vsp.congestion.events.CongestionEventsReader;

/**
 * Calculates the delay caused for each person by reading the marginal congestion events.
 * Additionally, returns the link delay and counts which can be used to get the (avg) toll, since a corresponding 
 * person is charged for every marginal congestion event.
 * @author amit
 */

public class CausedDelayAnalyzer {
	private final CausedDelayHandler handler;
	private final String eventsFile;
	
	public CausedDelayAnalyzer(final String eventsFile, final Scenario scenario, final int noOfTimeBin) {
		this.eventsFile = eventsFile;
		this.handler = new CausedDelayHandler(scenario, noOfTimeBin);
	}

	public CausedDelayAnalyzer(final String eventsFile, final Scenario scenario,final  int noOfTimeBin, final boolean sortingForInsideMunich) {
		this.eventsFile = eventsFile;
		this.handler = new CausedDelayHandler(scenario, noOfTimeBin, sortingForInsideMunich);
	}

	public void run(){
		EventsManager eventsManager = EventsUtils.createEventsManager();
		CongestionEventsReader reader = new CongestionEventsReader(eventsManager);

		eventsManager.addHandler(handler);
		reader.readFile(this.eventsFile);
	}

	public SortedMap<Double, Map<Id<Person>, Double>> getTimeBin2CausingPersonId2Delay() {
		return handler.getTimeBin2CausingPerson2Delay();
	}

	public SortedMap<Double, Map<Id<Link>, Double>> getTimeBin2LinkId2Delay() {
		return handler.getTimeBin2Link2Delay();
	}

	/**
	 * @return  set of causing persons (toll payers) on each link in each time bin
	 */
	public SortedMap<Double, Map<Id<Link>, Set<Id<Person>>>> getTimeBin2Link2CausingPersons(){
		return handler.getTimeBin2Link2CausingPersons();
	}

	/**
	 * @return  set of UNIQUE causing persons (toll payers) in each time bin
	 */
	public SortedMap<Double,Set<Id<Person>>> getTimeBin2ListOfTollPayers() {
		SortedMap<Double, Set<Id<Person>>> timeBin2ListOfTollPayers = handler.getTimeBin2CausingPersons();
		// to avid any errors, check the number of persons in each time bin. This needs to be removed subsequently.

		for(double d : timeBin2ListOfTollPayers.keySet()){
			int sum = 0;
			for(Id<Link> linkId : getTimeBin2Link2CausingPersons().get(d).keySet()){
				sum += getTimeBin2Link2CausingPersons().get(d).get(linkId).size();
			}
			if(sum != timeBin2ListOfTollPayers.get(d).size()) throw new RuntimeException("Number of toll payers in each time bin are not equal from two maps. Aborting ....");
		}
		return timeBin2ListOfTollPayers;
	}
}