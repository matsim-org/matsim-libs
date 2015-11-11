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
package playground.agarwalamit.munich.analysis.userGroup;

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

import playground.agarwalamit.munich.utils.ExtendedPersonFilter;
import playground.benjamin.scenarios.munich.analysis.filter.UserGroup;

/**
 * @author amit
 */

public class PeakHourTollHandler implements PersonMoneyEventHandler, PersonDepartureEventHandler, PersonArrivalEventHandler {

	public PeakHourTollHandler() {
		log.info("A trip starts with departure event and ends with arrival events. "
				+ "Therefore, toll payments corresponding to any trip starting in the peak hour is considered as peak hour toll.");
		log.warn("Peak hours are assumed as 07:00-09:00 and 16:00-18:00 by looking on the travel demand for BAU scenario.");
		for(UserGroup ug : UserGroup.values()) {
			this.userGrpTo_PkHrToll.put(ug, new HashMap<Id<Person>,List<Double>>());
			this.userGrpTo_OffPkHrToll.put(ug, new HashMap<Id<Person>,List<Double>>());
			this.userGrpTo_PkHrTripCounts.put(ug, new HashMap<Id<Person>,Integer>() );
			this.userGrpTo_OffPkHrTripCounts.put(ug, new HashMap<Id<Person>,Integer>() );
		}
	}

	private final List<Double> pkHrs = new ArrayList<>(Arrays.asList(new Double []{7.,8.,16.,17.}));
	private static final Logger log = Logger.getLogger(PeakHourTollHandler.class);

	private SortedMap<UserGroup, Map<Id<Person>,List<Double>>> userGrpTo_PkHrToll = new TreeMap<>();
	private SortedMap<UserGroup, Map<Id<Person>,Integer>> userGrpTo_PkHrTripCounts = new TreeMap<>();
	private SortedMap<UserGroup, Map<Id<Person>,List<Double>>> userGrpTo_OffPkHrToll = new TreeMap<>();
	private SortedMap<UserGroup, Map<Id<Person>,Integer>> userGrpTo_OffPkHrTripCounts = new TreeMap<>();

	private List<Id<Person>> pkHrsPersons = new ArrayList<>();
	private final ExtendedPersonFilter pf = new ExtendedPersonFilter();

	@Override
	public void reset(int iteration) {
		this.userGrpTo_OffPkHrToll.clear();
		this.userGrpTo_PkHrToll.clear();
		this.userGrpTo_PkHrTripCounts.clear();
		this.userGrpTo_OffPkHrTripCounts.clear();
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if ( ! event.getLegMode().equals(TransportMode.car) ) return ; // excluding non car trips
		double time = Math.floor( event.getTime()/3600. );
		Id<Person> personId = event.getPersonId();
		UserGroup ug = pf.getUserGroupFromPersonId(personId);

		// remove the person if exists
		pkHrsPersons.remove(personId);

		if (pkHrs.contains(time)) {//pkHr
			pkHrsPersons.add(event.getPersonId());
			Map<Id<Person>,Integer> id2count = this.userGrpTo_PkHrTripCounts.get(ug);
			if(id2count.containsKey(personId)) {//multiple trips in peak hours
				id2count.put(personId, id2count.get(personId)+1);
			} else {//first trip
				id2count.put(personId, 1);
			}
		} else { //offPkHr
			Map<Id<Person>,Integer> id2count = this.userGrpTo_OffPkHrTripCounts.get(ug);
			if(id2count.containsKey(personId)) {//multiple trips in off peak hours
				id2count.put(personId, id2count.get(personId)+1);
			} else {//first trip
				id2count.put(personId, 1);
			}
		}
	}

	@Override
	public void handleEvent(PersonMoneyEvent event) {
		Id<Person> personId = event.getPersonId();
		UserGroup ug = pf.getUserGroupFromPersonId(personId);
		if( pkHrsPersons.contains(personId) ) {
			Map<Id<Person>,List<Double>> id2Toll = this.userGrpTo_PkHrToll.get(ug);
			if(id2Toll.containsKey(personId )) {
				int tripNr = this.userGrpTo_PkHrTripCounts.get(ug).get(personId);
				List<Double> tolls = id2Toll.get(personId);
				if(tolls.size()==tripNr) {//existing trip
					double prevCollectedToll = tolls.get(tripNr-1);
					tolls.remove(tripNr-1);
					tolls.add(tripNr-1, prevCollectedToll+ (-event.getAmount()));	
				} else if(tripNr - tolls.size() == 1) { //new trip
					tolls.add(-event.getAmount());
				} else throw new RuntimeException("This money to event should have been already categorized. Aborting ...");
			} else {
				List<Double> tolls = new ArrayList<>();
				tolls.add(-event.getAmount());
				id2Toll.put(personId, tolls);
			}
		} else {
			Map<Id<Person>,List<Double>> id2Toll = this.userGrpTo_OffPkHrToll.get(ug);
			if(id2Toll.containsKey(personId )) {
				if(this.userGrpTo_OffPkHrTripCounts.get(ug).get(personId) == null ) {
					System.out.println("some problem");
				}
				int tripNr = this.userGrpTo_OffPkHrTripCounts.get(ug).get(personId);
				List<Double> tolls = id2Toll.get(personId);
				if(tolls.size()==tripNr) {//existing trip
					double prevCollectedToll = tolls.get(tripNr-1);
					tolls.remove(tripNr-1);
					tolls.add(tripNr-1, prevCollectedToll+ (-event.getAmount()));	
				} else if(tripNr - tolls.size() == 1) { //new trip
					tolls.add(-event.getAmount());
				} else throw new RuntimeException("This money to event should have been already categorized. Aborting ...");
			} else {
				List<Double> tolls = new ArrayList<>();
				tolls.add(-event.getAmount());
				id2Toll.put(personId, tolls);
			}
		}
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		if ( ! event.getLegMode().equals(TransportMode.car) ) return ; // excluding non car trips
		Id<Person> personId = event.getPersonId();
		UserGroup ug = pf.getUserGroupFromPersonId(personId);

		// fill the map if toll in some of the trips is zero.		
		if (pkHrsPersons.contains(event.getPersonId()) ) { 
			/*
			 * not possible to remove person departed in peak hour because, some of the congestion events are thrown after arrival.
			 * Probably, better to remove in the next departure event.
			 */
			int totalTrips = this.userGrpTo_PkHrTripCounts.get(ug).get(personId);
			int tolledTrips ;
			if(this.userGrpTo_PkHrToll.get(ug).containsKey(personId)) { // not in the map yet
				tolledTrips = this.userGrpTo_PkHrToll.get(ug).get(personId).size(); 
			}
			else tolledTrips = 0;
			if(totalTrips == tolledTrips) return;
			else if( totalTrips - tolledTrips == 1) {
				Map<Id<Person>,List<Double>> id2Toll = this.userGrpTo_PkHrToll.get(ug);
				List<Double> tolls ;
				if(id2Toll.containsKey(personId)) tolls  = id2Toll.get(personId);
				else tolls = new ArrayList<>();
				tolls.add(0.);
				id2Toll.put(personId, tolls);
			} else throw new RuntimeException("This should not happen. Aborting...");
		} else {
			int totalTrips = this.userGrpTo_OffPkHrTripCounts.get(ug).get(personId);
			int tolledTrips ;
			if(this.userGrpTo_OffPkHrToll.get(ug).containsKey(personId)) { // not in the map yet
				tolledTrips = this.userGrpTo_OffPkHrToll.get(ug).get(personId).size(); 
			}
			else tolledTrips = 0;
			if(totalTrips == tolledTrips) return;
			else if( totalTrips - tolledTrips == 1) {
				Map<Id<Person>,List<Double>> id2Toll = this.userGrpTo_OffPkHrToll.get(ug);
				List<Double> tolls;
				if(id2Toll.containsKey(personId)) tolls  = id2Toll.get(personId);
				else tolls = new ArrayList<>();
				tolls.add(0.);
				id2Toll.put(personId, tolls);
			} else throw new RuntimeException("This should not happen. Aborting...");

		}
	}
	
	public SortedMap<UserGroup, Map<Id<Person>, List<Double>>> getUserGrpTo_PkHrToll() {
		return userGrpTo_PkHrToll;
	}

	public SortedMap<UserGroup, Map<Id<Person>, Integer>> getUserGrpTo_PkHrTripCounts() {
		return userGrpTo_PkHrTripCounts;
	}

	public SortedMap<UserGroup, Map<Id<Person>, List<Double>>> getUserGrpTo_OffPkHrToll() {
		return userGrpTo_OffPkHrToll;
	}

	public SortedMap<UserGroup, Map<Id<Person>, Integer>> getUserGrpTo_OffPkHrTripCounts() {
		return userGrpTo_OffPkHrTripCounts;
	}
	
	public SortedMap<UserGroup,Integer> getUserGroupToPeakHourTripCounts() {
		return sumOfTripCounts(this.userGrpTo_PkHrTripCounts);
	}
	
	public SortedMap<UserGroup,Integer> getUserGroupToOffPeakHourTripCounts() {
		return sumOfTripCounts(this.userGrpTo_OffPkHrTripCounts);
	}
	
	public SortedMap<UserGroup, Double> getUserGroupToTotalPeakHourToll(){
		return sumOfTollPerTrip(this.userGrpTo_PkHrToll);
	}
	
	public SortedMap<UserGroup, Double> getUserGroupToTotalOffPeakHourToll(){
		return sumOfTollPerTrip(this.userGrpTo_OffPkHrToll);
	}
	
	private SortedMap<UserGroup,Integer> sumOfTripCounts (SortedMap<UserGroup, Map<Id<Person>,Integer>> countMap) {
		SortedMap<UserGroup,Integer> sumMap = new TreeMap<>();
		for (UserGroup ug : countMap.keySet() ) {
			int tripCountSum = 0;
			for(Id<Person> p :countMap.get(ug).keySet()){
				tripCountSum += countMap.get(ug).get(p);
			}
			sumMap.put(ug, tripCountSum);
		}
		return sumMap;
	}
	
	private SortedMap<UserGroup, Double> sumOfTollPerTrip(SortedMap<UserGroup, Map<Id<Person>,List<Double>>> tollMap){
		SortedMap<UserGroup, Double> userGrpToToll = new TreeMap<>();
		for (UserGroup ug : tollMap.keySet() ) {
			double tollSum = 0.;
			for(Id<Person> p :tollMap.get(ug).keySet()){
				for(double d:tollMap.get(ug).get(p)){
					tollSum += d;
				}
			}
			userGrpToToll.put(ug, tollSum);
		}
		return userGrpToToll;
	}
}
