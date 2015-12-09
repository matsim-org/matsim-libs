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
package playground.agarwalamit.analysis.trip;

import java.util.ArrayList;
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

	public TripTollHandler(double simulationEndTime, int noOfTimeBins) {
		log.info("A trip starts with departure event and ends with arrival events. ");
		this.timeBinSize = simulationEndTime / noOfTimeBins;
	}

	private double timeBinSize;
	private int nonCarWarning= 0;
	private static final Logger log = Logger.getLogger(TripTollHandler.class);

	private SortedMap<Double, Map<Id<Person>,Integer>> timeBin2Person2TripsCount = new TreeMap<>();
	private SortedMap<Double, Map<Id<Person>,List<Double>>> timeBin2Person2TripToll = new TreeMap<>();

	private SortedMap<Id<Person>,Double> personId2TripDepartTimeBin = new TreeMap<>();

	@Override
	public void reset(int iteration) {
		this.timeBin2Person2TripsCount.clear();
		this.timeBin2Person2TripToll.clear();
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if ( ! event.getLegMode().equals(TransportMode.car) ) { // excluding non car trips
			if(nonCarWarning<1){
				log.warn(TripTollHandler.class.getSimpleName()+" calculates trip info only for car mode.");
				log.warn( Gbl.ONLYONCE );
				nonCarWarning++;
			}
			return ;
		}
		double time = Math.max(1, Math.ceil( event.getTime()/this.timeBinSize) );
		Id<Person> personId = event.getPersonId();

		// remove the person here (because some time congestion toll is charged after agent arrival
		this.personId2TripDepartTimeBin.remove(personId);
		this.personId2TripDepartTimeBin.put(personId, time);

		if(timeBin2Person2TripsCount.containsKey(time)) {
			Map<Id<Person>,Integer> personId2TripCount = timeBin2Person2TripsCount.get(time);
			if(personId2TripCount.containsKey(personId)) { //multiple trips
				personId2TripCount.put(personId, personId2TripCount.get(personId) +1 );
			} else {//first trip
				personId2TripCount.put(personId, 1);
			}
		} else {//first person and first trip
			Map<Id<Person>,Integer> personId2TripCount = new HashMap<>();
			personId2TripCount.put(personId, 1);
			timeBin2Person2TripsCount.put(time, personId2TripCount);
		}
	}

	@Override
	public void handleEvent(PersonMoneyEvent event) {
		Id<Person> personId = event.getPersonId();

		double timebin = this.personId2TripDepartTimeBin.get(personId);
		if(timeBin2Person2TripToll.containsKey(timebin)){
			Map<Id<Person>,List<Double>> person2TripsTolls = timeBin2Person2TripToll.get(timebin);
			if ( person2TripsTolls.containsKey(personId) ) {
				List<Double> tolls = person2TripsTolls.get(personId);
				int tripNr = timeBin2Person2TripsCount.get(timebin).get(personId);
				if (tolls.size() == tripNr) { //existing trip
					double prevCollectedToll = tolls.remove(tripNr-1);
					tolls.add(tripNr -1, prevCollectedToll + (-event.getAmount()) );
				} else if(tripNr - tolls.size() == 1) { //new trip
					tolls.add(-event.getAmount());
				} else throw new RuntimeException("This is trip nr "+tripNr+" and tolls stored in the list are "+tolls.size()+".  This should not happen. Aborting ...");
			} else {
				List<Double> tolls = new ArrayList<>();
				tolls.add(-event.getAmount());	
				person2TripsTolls.put(personId, tolls);
			}
		} else {
			List<Double> tolls = new ArrayList<>();
			tolls.add(-event.getAmount());	
			Map<Id<Person>,List<Double>> person2TripsTolls = new HashMap<>();
			person2TripsTolls.put(personId, tolls);
			timeBin2Person2TripToll.put(timebin, person2TripsTolls);
		}
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		if ( ! event.getLegMode().equals(TransportMode.car) ) return ; // excluding non car trips

		Id<Person> personId = event.getPersonId();
		double time = this.personId2TripDepartTimeBin.get(personId);

		// following is required because, sometimes congestion event is thrown after arrival of the agent, thus removing the agent in the next departure event.
		int totalTrips = timeBin2Person2TripsCount.get(time).get(personId);
		
		//departure and arrival without any money event.
		if(timeBin2Person2TripToll.containsKey(time)){
			int tollsStored ;
			if (timeBin2Person2TripToll.get(time).containsKey(personId)) {
				tollsStored = timeBin2Person2TripToll.get(time).get(personId).size();
			} else tollsStored = 0;
			
			if(totalTrips == tollsStored) return;
			else if(totalTrips - tollsStored == 1) {
				Map<Id<Person>,List<Double>> person2Tolls = this.timeBin2Person2TripToll.get(time);
				List<Double> tolls ;
				if ( person2Tolls.containsKey(personId)) {
					tolls = person2Tolls.get(personId);
				}
				else tolls = new ArrayList<>();
				tolls.add(0.);
				person2Tolls.put(personId, tolls);
			} else throw new RuntimeException("This should not happen. Aborting...");
		} else {
			List<Double> tolls = new ArrayList<>();
			tolls.add(0.);
			Map<Id<Person>,List<Double>> person2Tolls = new HashMap<>();
			person2Tolls.put(personId, tolls);
			this.timeBin2Person2TripToll.put(time, person2Tolls);
		}
	}

	public SortedMap<Double, Map<Id<Person>, Integer>> getTimeBin2Person2TripsCount() {
		return timeBin2Person2TripsCount;
	}

	public SortedMap<Double, Map<Id<Person>, List<Double>>> getTimeBin2Person2TripToll() {
		return timeBin2Person2TripToll;
	}
	
}