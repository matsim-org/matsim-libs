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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonMoneyEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.gbl.Gbl;

/**
 * @author amit
 */

public class TripTollHandler implements PersonMoneyEventHandler, PersonDepartureEventHandler, PersonArrivalEventHandler {
	private static final Logger LOG = Logger.getLogger(TripTollHandler.class);
	private final double timeBinSize;
	private int nonCarWarning= 0;

	private final SortedMap<Double, Map<Id<Person>,Integer>> timeBin2Person2TripsCount = new TreeMap<>();
	private final SortedMap<Double, Map<Id<Person>,List<Double>>> timeBin2Person2TripToll = new TreeMap<>();
	private final SortedMap<Id<Person>,Double> personId2TripDepartTimeBin = new TreeMap<>();

	public TripTollHandler(final double simulationEndTime, final int noOfTimeBins) {
		LOG.info("A trip starts with departure event and ends with arrival events. ");
		this.timeBinSize = simulationEndTime / noOfTimeBins;
	}
	
	@Override
	public void reset(int iteration) {
		this.timeBin2Person2TripsCount.clear();
		this.timeBin2Person2TripToll.clear();
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if ( ! event.getLegMode().equals(TransportMode.car) ) { // excluding non car trips
			if(nonCarWarning<1){
				LOG.warn(TripTollHandler.class.getSimpleName()+" calculates trip info only for car mode.");
				LOG.warn( Gbl.ONLYONCE );
				nonCarWarning++;
			}
			return ;
		}
		double time = Math.max(1, Math.ceil( event.getTime()/this.timeBinSize) ) * this.timeBinSize;
		Id<Person> personId = event.getPersonId();

		// remove the person here (because some time congestion toll is charged after agent arrival
		this.personId2TripDepartTimeBin.remove(personId);
		this.personId2TripDepartTimeBin.put(personId, time);

		if(timeBin2Person2TripsCount.containsKey(time)) {
			Map<Id<Person>,Integer> personId2TripCount = timeBin2Person2TripsCount.get(time);
			Map<Id<Person>,List<Double>> personId2TripToll = timeBin2Person2TripToll.get(time);
			if(personId2TripCount.containsKey(personId)) { //multiple trips
				personId2TripCount.put(personId, personId2TripCount.get(personId) +1 );
				List<Double> tolls = personId2TripToll.get(personId);
				tolls.add(0.);
			} else {//first trip
				personId2TripCount.put(personId, 1);
				personId2TripToll.put(personId, new ArrayList<>(Arrays.asList(new Double [] {0.0})) );
			}
		} else {//first person and first trip
			Map<Id<Person>,Integer> personId2TripCount = new HashMap<>();
			personId2TripCount.put(personId, 1);
			timeBin2Person2TripsCount.put(time, personId2TripCount);

			Map<Id<Person>,List<Double>> personId2TripToll =  new HashMap<>();
			personId2TripToll.put(personId, new ArrayList<>(Arrays.asList(new Double [] {0.0})) );
			timeBin2Person2TripToll.put(time, personId2TripToll);
		}
	}

	@Override
	public void handleEvent(PersonMoneyEvent event) {
		Id<Person> personId = event.getPersonId();

		double timebin = this.personId2TripDepartTimeBin.get(personId);
		Map<Id<Person>,List<Double>> person2TripsTolls = timeBin2Person2TripToll.get(timebin);
		List<Double> tolls = person2TripsTolls.get(personId);
		int tripNr = timeBin2Person2TripsCount.get(timebin).get(personId);
		if (tolls.size() == tripNr) { //existing trip
			double prevCollectedToll = tolls.remove(tripNr-1);
			tolls.add(tripNr -1, prevCollectedToll + (-event.getAmount()) );
		} else throw new RuntimeException("Trip count and trip dist maps are initiated at departure events, thus, tripNr should be equal to "
				+ "number of distances stored in trip dist map. Aborting ...");
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		if ( ! event.getLegMode().equals(TransportMode.car) ) return ; // excluding non car trips

		Id<Person> personId = event.getPersonId();
		double time = this.personId2TripDepartTimeBin.get(personId);

		// following is required because, sometimes congestion event is thrown after arrival of the agent, 
		//thus removing the agent in the next departure event.
		int totalTrips = timeBin2Person2TripsCount.get(time).get(personId);

		//departure and arrival without any money event.
		int tollsStored = timeBin2Person2TripToll.get(time).get(personId).size();

		if(totalTrips == tollsStored) return;
		else throw new RuntimeException("Trip count and trip dist maps are initiated at departure events, thus, tripNr should be equal to "
				+ "number of distances stored in trip dist map. Aborting ...");
	}

	public SortedMap<Double, Map<Id<Person>, Integer>> getTimeBin2Person2TripsCount() {
		return timeBin2Person2TripsCount;
	}

	public SortedMap<Double, Map<Id<Person>, List<Double>>> getTimeBin2Person2TripToll() {
		return timeBin2Person2TripToll;
	}
}