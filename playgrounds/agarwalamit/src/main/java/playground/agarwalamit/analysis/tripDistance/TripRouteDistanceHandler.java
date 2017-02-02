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
package playground.agarwalamit.analysis.tripDistance;

import java.util.*;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.*;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.events.algorithms.Vehicle2DriverEventHandler;

/**
 * Trip distances are calculated by summing up the link lengths for all link leave events which 
 * includes the link on which agent is departed. Inclusion of departure link can be argued against arrival link. 
 *
 * TODO Also look on {@link TripDistanceHandler} which can do the work by this class, if so, remove it.
 *
 * @author amit
 */

public class TripRouteDistanceHandler implements PersonDepartureEventHandler, PersonArrivalEventHandler, LinkLeaveEventHandler,
VehicleEntersTrafficEventHandler, VehicleLeavesTrafficEventHandler {

	private static final Logger LOG = Logger.getLogger(TripRouteDistanceHandler.class);
	private final SortedMap<Id<Person>,Double> personId2TripDepartTimeBin = new TreeMap<>();
	private final SortedMap<Double, Map<Id<Person>,Integer>> timeBin2Person2TripsCount = new TreeMap<>();
	private final SortedMap<Double, Map<Id<Person>,List<Double>>> timeBin2Person2TripsDistance = new TreeMap<>();

	private final Map<Id<Person>, String> person2mode = new HashMap<>();
	private final SortedMap<String, Double> mode2TripDistace = new TreeMap<>();


	private final Vehicle2DriverEventHandler delegate = new Vehicle2DriverEventHandler();

	private final Network network;
	private final double timeBinSize;
	private final int nonCarWarning= 0;
	
	public TripRouteDistanceHandler(final Network network, final double simulationEndTime, final int noOfTimeBins) {
		LOG.info("A trip starts with departure event and ends with arrival events.");
		this.network = network;
		this.timeBinSize = simulationEndTime / noOfTimeBins;
	}

	@Override
	public void reset(int iteration) {
		this.timeBin2Person2TripsCount.clear();
		this.timeBin2Person2TripsDistance.clear();
		this.personId2TripDepartTimeBin.clear();
		this.mode2TripDistace.clear();
		this.delegate.reset(iteration);
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		Id<Link> linkId = event.getLinkId();
		double linkLength = network.getLinks().get(linkId).getLength();
		Id<Person> driverId = this.delegate.getDriverOfVehicle(event.getVehicleId());

		double timebin = this.personId2TripDepartTimeBin.get(driverId);
		Map<Id<Person>,List<Double>> person2TripsDists = timeBin2Person2TripsDistance.get(timebin);
		List<Double> dists = person2TripsDists.get(driverId);
		int tripNr = timeBin2Person2TripsCount.get(timebin).get(driverId);
		if (dists.size() == tripNr) { //existing trip
			double prevCollectedDist = dists.remove( tripNr -1 );
			dists.add(tripNr - 1, prevCollectedDist + linkLength);
		} else throw new RuntimeException("Trip count and trip dist maps are initiated at departure events, thus, tripNr should be equal to "
				+ "number of distances stored in trip dist map. Aborting ...");

		// mode 2 dist
		String mode = this.person2mode.get(driverId);
		if (mode2TripDistace.containsKey(mode)) {
			mode2TripDistace.put(mode, mode2TripDistace.get(mode)+linkLength);
		} else {
			mode2TripDistace.put(mode, linkLength);
		}
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		Id<Person> personId = event.getPersonId();
		double time = this.personId2TripDepartTimeBin.remove(personId);

		int totalTrips = timeBin2Person2TripsCount.get(time).get(personId);
		int distancesStored = timeBin2Person2TripsDistance.get(time).get(personId).size();

		if(totalTrips != distancesStored) {
			throw new RuntimeException(
					"Trip count and trip dist maps are initiated at departure events, thus, tripNr should be equal to "
							+ "number of distances stored in trip dist map. Aborting ...");
		}
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		// if time bin is less than 1 hour, than only integer does not make much sense.
		double time = Math.max(1, Math.ceil( event.getTime()/this.timeBinSize) ) * this.timeBinSize; 
		Id<Person> personId = event.getPersonId();

		this.personId2TripDepartTimeBin.put(personId, time);
		this.person2mode.put(event.getPersonId(), event.getLegMode());

		if(timeBin2Person2TripsCount.containsKey(time)) {
			Map<Id<Person>,Integer> personId2TripCount = timeBin2Person2TripsCount.get(time);
			Map<Id<Person>,List<Double>> personId2TripDist = timeBin2Person2TripsDistance.get(time);
			if(personId2TripCount.containsKey(personId)) { //multiple trips
				personId2TripCount.put(personId, personId2TripCount.get(personId) +1 );
				List<Double> dists = personId2TripDist.get(personId);
				dists.add(0.);
			} else {//person is not present 
				personId2TripCount.put(personId, 1);
				personId2TripDist.put(personId, new ArrayList<>(Arrays.asList(new Double [] {0.0})) );
			}
		} else {// timebin is not present
			Map<Id<Person>,Integer> personId2TripCount = new HashMap<>();
			personId2TripCount.put(personId, 1);
			timeBin2Person2TripsCount.put(time, personId2TripCount);
			
			Map<Id<Person>,List<Double>> personId2TripDist = new HashMap<>();
			personId2TripDist.put(personId, new ArrayList<>(Arrays.asList(new Double [] {0.0})) );
			timeBin2Person2TripsDistance.put(time, personId2TripDist);
		}
	}

	@Override
	public void handleEvent(VehicleLeavesTrafficEvent event) {
		delegate.handleEvent(event);
	}

	@Override
	public void handleEvent(VehicleEntersTrafficEvent event) {
		delegate.handleEvent(event);
	}
	public SortedMap<Double, Map<Id<Person>, Integer>> getTimeBin2Person2TripsCount() {
		return timeBin2Person2TripsCount;
	}

	public SortedMap<Double, Map<Id<Person>, List<Double>>> getTimeBin2Person2TripsDistance() {
		return timeBin2Person2TripsDistance;
	}

	public SortedMap<String, Double> getMode2TripDistace(){
		return this.mode2TripDistace;
	}
}