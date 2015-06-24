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

import java.util.List;
import java.util.Map;
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

	public CausedDelayAnalyzer(String eventsFile, Scenario scenario, int noOfTimeBin) {
		this.eventsFile = eventsFile;
		handler = new CausedDelayHandler(scenario, noOfTimeBin);
	}
	
	public CausedDelayAnalyzer(String eventsFile, Scenario scenario, int noOfTimeBin,boolean sortingForInsideMunich) {
		this.eventsFile = eventsFile;
		handler = new CausedDelayHandler(scenario, noOfTimeBin, sortingForInsideMunich);
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
	
	public SortedMap<Double,List<Id<Person>>> getTimeBin2ListOfTollPayers() {
		return handler.getTimeBin2ListOfTollPayers();
	}
}
