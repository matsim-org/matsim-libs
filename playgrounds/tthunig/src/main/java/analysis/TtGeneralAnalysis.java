/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * DefaultControlerModules.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2014 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */
package analysis;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.vehicles.Vehicle;

import com.google.inject.Inject;

/**
 * @author tthunig
 *
 */
public class TtGeneralAnalysis implements PersonDepartureEventHandler, PersonArrivalEventHandler, LinkEnterEventHandler, LinkLeaveEventHandler, VehicleEntersTrafficEventHandler, VehicleLeavesTrafficEventHandler {

	private int totalNumberOfTrips;
	private double totalDistance;
	private double totalTt;
	private double sumOfSpeedsMessured;
	
	private Map<Id<Vehicle>, List<Double>> ttPerVehiclePerTrip;
	private Map<Id<Vehicle>, LinkedList<Double>> delayPerVehiclePerTrip;
	private Map<Id<Vehicle>, LinkedList<Double>> distancePerVehiclePerTrip;
	private Map<Id<Vehicle>, LinkedList<Double>> avgSpeedPerVehiclePerTrip;
	
	private Map<Id<Vehicle>, Double> veh2lastVehEntersTrafficTime;
	private Map<Id<Vehicle>, Double> veh2earliestLinkExitTime;
	
	private Map<Id<Link>, Double> totalDelayPerLink;
//	private Map<Id<Link>, Map<Double, Double>> delayPerLinkPerHour;
	private Map<Id<Link>, Integer> numberOfVehPerLink;
//	private Map<Id<Link>, Map<Double, Integer>> numberOfVehPerLinkPerHour;
//	private Map<Id<Link>, Double> avgSpeedPerLink;
//	private Map<Id<Link>, Map<Double, Double>> avgSpeedPerLinkPerHour;
	
	/* in all these maps the key gives the start of the interval */
	// in 100 m steps
	private Map<Double, Integer> numberOfTripsPerTripDistanceInterval;
	// in 100 s steps
	private Map<Double, Integer> numberOfTripsPerTripDurationInterval;
	// in 1 m/s steps
	private Map<Double, Integer> numberOfTripsPerTripSpeedInterval;
	// in 100 s steps
	private Map<Double, Integer> numberOfDeparturesPerTimeInterval;
	// in 100 s steps
	private Map<Double, Integer> numberOfArrivalsPerTimeInterval;
	
	@Inject
	private Scenario scenario;
	
	@Override
	public void reset(int iteration) {
		this.totalNumberOfTrips = 0;
		this.totalDistance = 0.0;
		this.totalTt = 0.0;
		this.sumOfSpeedsMessured = 0.0;
		this.ttPerVehiclePerTrip = new HashMap<>();
		this.delayPerVehiclePerTrip = new HashMap<>();
		this.distancePerVehiclePerTrip = new HashMap<>();
		this.avgSpeedPerVehiclePerTrip = new HashMap<>();
		this.veh2lastVehEntersTrafficTime = new HashMap<>();
		this.veh2earliestLinkExitTime = new HashMap<>();
		this.totalDelayPerLink = new HashMap<>();
		this.numberOfVehPerLink = new HashMap<>();
		this.numberOfTripsPerTripDistanceInterval = new TreeMap<>();
		this.numberOfTripsPerTripDurationInterval = new TreeMap<>();
		this.numberOfTripsPerTripSpeedInterval = new TreeMap<>();
		this.numberOfDeparturesPerTimeInterval = new TreeMap<>();
		this.numberOfArrivalsPerTimeInterval = new TreeMap<>();
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		totalNumberOfTrips++;
		
		double timeInterval = Math.floor(event.getTime() / 100) * 100;
		if (!numberOfDeparturesPerTimeInterval.containsKey(timeInterval)){
			numberOfDeparturesPerTimeInterval.put(timeInterval, 0);
		}
		int entry = numberOfDeparturesPerTimeInterval.get(timeInterval);
		numberOfDeparturesPerTimeInterval.put(timeInterval, ++entry);
	}

	@Override
	public void handleEvent(VehicleEntersTrafficEvent event) {		
		// for the first link every agent needs one second without delay
		veh2earliestLinkExitTime.put(event.getVehicleId(), event.getTime() + 1);
		
		veh2lastVehEntersTrafficTime.put(event.getVehicleId(), event.getTime());
		
		// initialize vehicle based data structure for the first trip
		if (!delayPerVehiclePerTrip.containsKey(event.getVehicleId())){
			delayPerVehiclePerTrip.put(event.getVehicleId(), new LinkedList<Double>());
			ttPerVehiclePerTrip.put(event.getVehicleId(), new LinkedList<Double>());
			distancePerVehiclePerTrip.put(event.getVehicleId(), new LinkedList<Double>());
			avgSpeedPerVehiclePerTrip.put(event.getVehicleId(), new LinkedList<Double>());
		}
		delayPerVehiclePerTrip.get(event.getVehicleId()).add(0.0);
		distancePerVehiclePerTrip.get(event.getVehicleId()).add(0.0);
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		Link currentLink = scenario.getNetwork().getLinks().get(event.getLinkId());
		
		// remove the distance of the last trip temporary from the list
		double previousDistance = distancePerVehiclePerTrip.get(event.getVehicleId()).pollLast();
		// if the last distance is 0.0, it is the departure link
		if (previousDistance == 0.0){
			// count this link as 1 m
			distancePerVehiclePerTrip.get(event.getVehicleId()).add(1.0);
		} else {
			// add the summed distance
			distancePerVehiclePerTrip.get(event.getVehicleId()).add(previousDistance	+ currentLink.getLength());
		}
		
		// initialize link based analysis data structure
		if (!totalDelayPerLink.containsKey(event.getLinkId())){
			totalDelayPerLink.put(event.getLinkId(), 0.0);
			numberOfVehPerLink.put(event.getLinkId(), 0);
		}
		// calculate delay
		double currentDelay = event.getTime() - veh2earliestLinkExitTime.get(event.getVehicleId());
		totalDelayPerLink.put(event.getLinkId(), totalDelayPerLink.get(event.getLinkId()) + currentDelay);
		// remove the delay of the last trip temporary from the list
		double previousDelay = delayPerVehiclePerTrip.get(event.getVehicleId()).pollLast();
		// add it again as updated delay
		delayPerVehiclePerTrip.get(event.getVehicleId()).add(previousDelay + currentDelay);
		
		int vehCount = numberOfVehPerLink.get(event.getLinkId());
		numberOfVehPerLink.put(event.getLinkId(), ++vehCount);
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		// calculate earliest link exit time
		Link currentLink = scenario.getNetwork().getLinks().get(event.getLinkId());
		double freespeedTt = currentLink.getLength() / currentLink.getFreespeed();
		// this is the earliest time where matsim sets the agent to the next link
		double matsimFreespeedTT = Math.floor(freespeedTt + 1);	
		veh2earliestLinkExitTime.put(event.getVehicleId(), event.getTime() + matsimFreespeedTT);
	}

	@Override
	public void handleEvent(VehicleLeavesTrafficEvent event) {
		// trip is finished ... handle it's distance
		double tripDistance = distancePerVehiclePerTrip.get(event.getVehicleId()).peekLast();
		totalDistance += tripDistance;
		double distInterval = Math.floor(tripDistance / 100) * 100;
		int distEntry = 0;
		if (numberOfTripsPerTripDistanceInterval.containsKey(distInterval)){
			distEntry = numberOfTripsPerTripDistanceInterval.get(distInterval);
		}
		numberOfTripsPerTripDistanceInterval.put(distInterval, ++distEntry);
		
		// ... handle it's duration
		double tripDuration = event.getTime() - veh2lastVehEntersTrafficTime.get(event.getVehicleId());
		totalTt += tripDuration;
		ttPerVehiclePerTrip.get(event.getVehicleId()).add(tripDuration);
		double durationInterval = Math.floor(tripDuration / 100) * 100;
		int durEntry = 0;
		if (numberOfTripsPerTripDurationInterval.containsKey(durationInterval)){
			durEntry = numberOfTripsPerTripDurationInterval.get(durationInterval);
		}
		numberOfTripsPerTripDurationInterval.put(durationInterval, ++durEntry);
		
		// ... calculate it's average speed
		double avgTripSpeed = tripDistance / tripDuration;
		sumOfSpeedsMessured += avgTripSpeed;
		avgSpeedPerVehiclePerTrip.get(event.getVehicleId()).add(avgTripSpeed);
		double speedInterval = Math.floor(avgTripSpeed);
		int speedEntry = 0;
		if (numberOfTripsPerTripSpeedInterval.containsKey(speedInterval)){
			speedEntry = numberOfTripsPerTripSpeedInterval.get(speedInterval);
		}
		numberOfTripsPerTripSpeedInterval.put(speedInterval, ++speedEntry);
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		double timeInterval = Math.floor(event.getTime() / 100) * 100;
		if (!numberOfArrivalsPerTimeInterval.containsKey(timeInterval)){
			numberOfArrivalsPerTimeInterval.put(timeInterval, 0);
		}
		int entry = numberOfArrivalsPerTimeInterval.get(timeInterval);
		numberOfArrivalsPerTimeInterval.put(timeInterval, ++entry);
	}

	public Map<Double, Double> getRelativeCumulativeFrequencyOfTripsPerDuration(){
		return determineRelCumFreqMapOfCountsPerInterval(numberOfTripsPerTripDurationInterval);
	}
	
	public double getTotalTt(){
		return totalTt;
	}
	
	public Map<Id<Vehicle>, List<Double>> getTtPerVehiclePerTrip(){
		return ttPerVehiclePerTrip;
	}
	
	public double getTotalDistance(){
		return totalDistance;
	}
	
	public Map<Double, Double> getRelativeCumulativeFrequencyOfTripsPerDistance(){
		return determineRelCumFreqMapOfCountsPerInterval(numberOfTripsPerTripDistanceInterval);
	}
	
	public double getAverageTripSpeed(){
		return sumOfSpeedsMessured * 1. / totalNumberOfTrips;	
	}
	
	public Map<Double, Double> getRelativeCumulativeFrequencyOfTripsPerSpeed(){
		return determineRelCumFreqMapOfCountsPerInterval(numberOfTripsPerTripSpeedInterval);
	}
	
	public Map<Double, Double> getRelativeCumulativeFrequencyOfDeparturesPerTimeInterval(){
		return determineRelCumFreqMapOfCountsPerInterval(numberOfDeparturesPerTimeInterval);
	}
	
	public Map<Double, Double> getRelativeCumulativeFrequencyOfArrivalsPerTimeInterval(){
		return determineRelCumFreqMapOfCountsPerInterval(numberOfArrivalsPerTimeInterval);
	}
	
	private Map<Double, Double> determineRelCumFreqMapOfCountsPerInterval(Map<Double, Integer> numberOfTripsMap) {
		Map<Double, Double> relativeComulativeFrequency = new TreeMap<>();
		double relCumFreqOfCurrentInterval = 0.0;
		for (Entry<Double, Integer> entry : numberOfTripsMap.entrySet()){
			relCumFreqOfCurrentInterval += (entry.getValue() * 1. / totalNumberOfTrips);
			relativeComulativeFrequency.put(entry.getKey(), relCumFreqOfCurrentInterval);
		}
		return relativeComulativeFrequency;
	}

	public double getTotalDelay(){
		double totalDelay = 0.0;
		for (Double linkDelay : totalDelayPerLink.values()){
			totalDelay += linkDelay;
		}
		return totalDelay;
	}
	
	public Map<Id<Link>, Double> getTotalDelayPerLink(){
		return totalDelayPerLink;
	}

	public Map<Id<Link>, Double> getAvgDelayPerLink(){
		Map<Id<Link>, Double> avgDelayMap = new HashMap<>();
		for (Id<Link> linkId : totalDelayPerLink.keySet()){
			avgDelayMap.put(linkId, totalDelayPerLink.get(linkId) / numberOfVehPerLink.get(linkId));
		}
		return avgDelayMap;
	}
	
	public Map<Id<Vehicle>, LinkedList<Double>> getDelayPerVehiclePerTrip(){
		return delayPerVehiclePerTrip;
	}
	
	public Map<Id<Vehicle>, LinkedList<Double>> getDistancePerVehiclePerTrip(){
		return distancePerVehiclePerTrip;
	}

}
