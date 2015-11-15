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
package playground.agarwalamit.munich.analysis.userGroup.toll;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.gbl.Gbl;

/**
 * @author amit
 */

public class TripDistanceHandler
implements PersonDepartureEventHandler, PersonArrivalEventHandler, LinkLeaveEventHandler {

	public TripDistanceHandler(Network network, double simulationEndTime, int noOfTimeBins) {
		this.network = network;
		this.timeBinSize = simulationEndTime / noOfTimeBins;
	}

	private SortedMap<Double, Map<Id<Person>,Integer>> timeBin2Person2TripsCount = new TreeMap<>();
	private SortedMap<Double, Map<Id<Person>,List<Double>>> timeBin2Person2TripsDistance = new TreeMap<>();

	private static final Logger log = Logger.getLogger(TripDistanceHandler.class);
	private Network network;
	private double timeBinSize;
	private int nonCarWarning= 0;
	
	@Override
	public void reset(int iteration) {
		this.timeBin2Person2TripsCount.clear();
		this.timeBin2Person2TripsDistance.clear();
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		Id<Link> linkId = event.getLinkId();
		double linkLength = network.getLinks().get(linkId).getLength();
		Id<Person> driverId = event.getDriverId();
		
		double time = Math.max(this.timeBinSize, Math.ceil( event.getTime()/this.timeBinSize) );

		if(timeBin2Person2TripsDistance.containsKey(time)){
			Map<Id<Person>,List<Double>> person2TripsDists = timeBin2Person2TripsDistance.get(time);
			if ( person2TripsDists.containsKey(driverId) ) {
				List<Double> dists = person2TripsDists.get(driverId);
				int tripNr = timeBin2Person2TripsCount.get(time).get(driverId);
				if (dists.size() == tripNr) { //existing trip
					double prevCollectedDist = dists.get(tripNr-1);
					dists.remove(tripNr-1);
					dists.add(tripNr -1, prevCollectedDist + linkLength);
				} else if(tripNr - dists.size() == 1) { //new trip
					dists.add(linkLength);
				} else throw new RuntimeException("This is trip nr "+tripNr+" and distances stored in the list are "+dists.size()+".  This should not happen. Aborting ...");
			} else {
				List<Double> dists = new ArrayList<>();
				dists.add(linkLength);	
				person2TripsDists.put(driverId, dists);
			}
		} else {
			List<Double> dists = new ArrayList<>();
			dists.add(linkLength);	
			Map<Id<Person>,List<Double>> person2TripsDists = new HashMap<>();
			person2TripsDists.put(driverId, dists);
			timeBin2Person2TripsDistance.put(time, person2TripsDists);
		}
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		if ( ! event.getLegMode().equals(TransportMode.car) ) return ; // excluding non car trips

		double time = Math.max(this.timeBinSize, Math.ceil( event.getTime()/this.timeBinSize) );
		// following is required because, sometimes agent depart and arrive on the same link, therefore, for such trips, distance =0.
		
		Id<Person> personId = event.getPersonId();
		int totalTrips = timeBin2Person2TripsCount.get(time).get(personId);;
		int distancesStored ;
		if (timeBin2Person2TripsDistance.get(time).containsKey(personId)) {
			distancesStored = timeBin2Person2TripsDistance.get(time).get(personId).size();
		} else distancesStored = 0;
		
		if(totalTrips == distancesStored) return;
		else if(totalTrips - distancesStored == 1) {
			Map<Id<Person>,List<Double>> person2Dists = this.timeBin2Person2TripsDistance.get(time);
			List<Double> dists ;
			if ( person2Dists.containsKey(personId)) dists = person2Dists.get(personId);
			else dists = new ArrayList<>();
			dists.add(0.);
			person2Dists.put(personId, dists);
		} else throw new RuntimeException("This should not happen. Aborting...");
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if ( ! event.getLegMode().equals(TransportMode.car) ) { // excluding non car trips
			if(nonCarWarning<1){
				log.warn(TripDistanceHandler.class.getSimpleName()+" calculates trip info only for car mode.");
				log.warn( Gbl.ONLYONCE );
				nonCarWarning++;
			}
			return ;
		}

		double time = Math.max(this.timeBinSize, Math.ceil( event.getTime()/this.timeBinSize) );

		Id<Person> personId = event.getPersonId();

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
	public SortedMap<Double, Map<Id<Person>, Integer>> getTimeBin2Person2TripsCount() {
		return timeBin2Person2TripsCount;
	}

	public SortedMap<Double, Map<Id<Person>, List<Double>>> getTimeBin2Person2TripsDistance() {
		return timeBin2Person2TripsDistance;
	}
}
