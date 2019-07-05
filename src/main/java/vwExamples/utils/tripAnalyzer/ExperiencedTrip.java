/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

package vwExamples.utils.tripAnalyzer;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author gleich
 */
public class ExperiencedTrip {
	private final Id<Person> agent;
	private final String activityBefore;
	private final String activityAfter;
	// private final Coord from;
	// private final Coord to;
	private final Id<Link> fromLinkId;
	private final Id<Link> toLinkId;
	private final double startTime;
	private final double endTime;
	private final int tripNumber;
	private final Id<ExperiencedTrip> id;
	private final List<ExperiencedLeg> legs;

	private LinkedList<Id<TransitStopFacility>> transitStopsVisited = new LinkedList<>();

	private Map<String, Double> mode2inVehicleTime = new HashMap<>();
	private Map<String, Double> mode2distance = new HashMap<>();
	private Map<String, Double> mode2waitTime = new HashMap<>();
	private Map<String, Double> mode2maxPerLegWaitTime = new HashMap<>();
	private Map<String, Integer> mode2numberOfLegs = new HashMap<>();
	private String tripClass;
	private int subtourNr;
	private Double startOfParking = null;
	private Double endOfParking = null;

	// Coords unavailable in events

	/**
	 * @param agent
	 * @param activityBefore
	 * @param activityAfter
	 * @param from
	 * @param to
	 * @param fromLink
	 * @param toLink
	 * @param startTime
	 * @param endTime
	 * @param includesDrt
	 */
	ExperiencedTrip(Id<Person> agent, String activityBefore, String activityAfter,
			// Coord from, Coord to,
			Id<Link> fromLink, Id<Link> toLink, double startTime, double endTime, int tripNumber,
			List<ExperiencedLeg> legs, Set<String> monitoredModes) {
		this.agent = agent;
		this.activityBefore = activityBefore;
		this.activityAfter = activityAfter;
		// this.from = from;
		// this.to = to;
		this.fromLinkId = fromLink;
		this.toLinkId = toLink;
		this.startTime = startTime;
		this.endTime = endTime;
		this.tripNumber = tripNumber;
		this.legs = legs;
		this.id = Id.create(agent + "_trip" + tripNumber + "_from_" + fromLinkId + "_to_" + toLinkId,
				ExperiencedTrip.class);
		calcSumsOverAllLegs(monitoredModes);
		findTransitStopsVisited();
		this.tripClass = null;

	}

	private void findTransitStopsVisited() {
		for (ExperiencedLeg leg : legs) {
			if (leg.getMode().equals(TransportMode.pt)) {
				/*
				 * Transfer stops are visited by the pt leg ending there and the following pt
				 * leg but save the TransitStop only once.
				 */
				if (transitStopsVisited.isEmpty()) {
					transitStopsVisited.add(leg.getPtFromStop());
				} else if (transitStopsVisited.getLast().equals(leg.getFromLinkId())) {
					// do not add twice
				} else {
					transitStopsVisited.add(leg.getPtFromStop());
				}
				transitStopsVisited.add(leg.getPtToStop());
			}
		}
	}

	private void calcSumsOverAllLegs(Set<String> monitoredModes) {
		for (String mode : monitoredModes) {
			mode2inVehicleTime.put(mode, 0.0);
			mode2distance.put(mode, 0.0);
			mode2waitTime.put(mode, 0.0);
			mode2maxPerLegWaitTime.put(mode, 0.0);
			mode2numberOfLegs.put(mode, 0);
		}
		mode2inVehicleTime.put("Other", 0.0);
		mode2distance.put("Other", 0.0);
		mode2waitTime.put("Other", 0.0);
		mode2maxPerLegWaitTime.put("Other", 0.0);
		mode2numberOfLegs.put("Other", 0);
		for (ExperiencedLeg leg : legs) {
			String mode = leg.getMode();
			if (monitoredModes.contains(mode)) {
				mode2inVehicleTime.put(mode, mode2inVehicleTime.get(mode) + leg.getInVehicleTime());
				mode2distance.put(mode, mode2distance.get(mode) + leg.getDistance());
				mode2waitTime.put(mode, mode2waitTime.get(mode) + leg.getWaitTime());
				mode2numberOfLegs.put(mode, mode2numberOfLegs.get(mode) + 1);
				if (mode2maxPerLegWaitTime.get(mode) < leg.getWaitTime()) {
					mode2maxPerLegWaitTime.put(mode, leg.getWaitTime());
				}
			} else {
				mode2inVehicleTime.put("Other", mode2inVehicleTime.get("Other") + leg.getInVehicleTime());
				mode2distance.put("Other", mode2distance.get("Other") + leg.getDistance());
				mode2waitTime.put("Other", mode2waitTime.get("Other") + leg.getWaitTime());
				mode2numberOfLegs.put("Other", mode2numberOfLegs.get("Other") + 1);
				if (mode2maxPerLegWaitTime.get("Other") < leg.getWaitTime()) {
					mode2maxPerLegWaitTime.put("Other", leg.getWaitTime());
				}
			}
		}
	}

	double getTotalTravelTime() {
		return endTime - startTime;
	}

	// Getter
	Map<String, Double> getMode2inVehicleOrMoveTime() {
		return mode2inVehicleTime;
	}

	Map<String, Double> getMode2inVehicleOrMoveDistance() {
		return mode2distance;
	}

	Map<String, Double> getMode2waitTime() {
		return mode2waitTime;
	}

	Map<String, Double> getMode2maxPerLegWaitTime() {
		return mode2maxPerLegWaitTime;
	}

	Map<String, Integer> getMode2numberOfLegs() {
		return mode2numberOfLegs;
	}

	Id<Person> getAgent() {
		return agent;
	}

	String getActivityBefore() {
		return activityBefore;
	}

	String getActivityAfter() {
		return activityAfter;
	}

	// Coord getFrom() {
	// return from;
	// }
	// Coord getTo() {
	// return to;
	// }
	Id<Link> getFromLinkId() {
		return fromLinkId;
	}

	Id<Link> getToLinkId() {
		return toLinkId;
	}

	double getStartTime() {
		return startTime;
	}

	double getEndTime() {
		return endTime;
	}

	String getTripClass() {
		return tripClass;
	}

	int getTripNumber() {
		return tripNumber;
	}

	Id<ExperiencedTrip> getId() {
		return id;
	}

	List<ExperiencedLeg> getLegs() {
		return legs;
	}

	void setTripClass(String tripClass) {
		this.tripClass = tripClass;
	}

	void setSubTourNr(int subTourNr) {
		this.subtourNr = subTourNr;

	}

	int getSubTourNr() {
		return this.subtourNr;

	}

	void setParkingStart(Double time) {
		this.startOfParking = time;
	}

	void setParkingEnd(Double time) {
		this.endOfParking = time;
	}

	public LinkedList<Id<TransitStopFacility>> getTransitStopsVisited() {
		return transitStopsVisited;
	}

	String getMainMode() {
		Set<String> acceptedMainModes = new HashSet<>(Arrays.asList("car", "pt", "drt", "walk", "ride", "bike","transit_walk","stayHome"));

		for (ExperiencedLeg leg : this.legs) {
			String legMode = leg.getMode();

			if (acceptedMainModes.contains(legMode)) {
				return legMode;
			}
//			else if  (legMode.equals("transit_walk")) {
//				return "walk";
//			}

		}
		
		//Default mode not found, check for transit_walk
		for (ExperiencedLeg leg : this.legs) {
			String legMode = leg.getMode();

			if (TransportMode.transit_walk.equals(legMode)) {
				return legMode;
			}
//			else if  (legMode.equals("transit_walk")) {
//				return "walk";
//			}

		}
//
//		if (mode2distance.get("transit_walk") > 0) {
//			return "walk";
//		}

		return "unknown";

	}
}
