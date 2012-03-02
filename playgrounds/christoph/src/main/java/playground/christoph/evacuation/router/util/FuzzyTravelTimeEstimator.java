/* *********************************************************************** *
 * project: org.matsim.*
 * FuzzyTravelTimeEstimator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.christoph.evacuation.router.util;

import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.router.util.PersonalizableTravelTime;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.utils.geometry.CoordUtils;

import playground.christoph.evacuation.mobsim.AgentPosition;
import playground.christoph.evacuation.mobsim.AgentsTracker;
import playground.christoph.evacuation.mobsim.Tracker.Position;
import playground.christoph.evacuation.mobsim.VehiclesTracker;

/**
 * Get travel times from a given travel time calculator and adds a random fuzzy value.
 * This values depends on the agent, the link and the distance between the link and 
 * the agent's current position. The calculation of the fuzzy values is deterministic, 
 * multiple calls with identical parameters will result in identical return values.
 * 
 * @author cdobler
 */
public class FuzzyTravelTimeEstimator implements PersonalizableTravelTime {

	private static final Logger log = Logger.getLogger(FuzzyTravelTimeEstimator.class);
	
	private static final long hashCodeModFactor = 123456;

	private final Scenario scenario;
	private final PersonalizableTravelTime travelTime;
	private final AgentsTracker agentsTracker;
	private final VehiclesTracker vehiclesTracker;
	private final Map<Id, Map<Id, Double>> distanceFuzzyFactors;
	
	private Id pId;
	private int pIdHashCode;
	private double personFuzzyFactor;
	
	/*package*/ FuzzyTravelTimeEstimator(Scenario scenario, PersonalizableTravelTime travelTime, AgentsTracker agentsTracker,
			VehiclesTracker vehiclesTracker, Map<Id, Map<Id, Double>> distanceFuzzyFactors) {
		this.scenario = scenario;
		this.travelTime = travelTime;
		this.agentsTracker = agentsTracker;
		this.vehiclesTracker = vehiclesTracker;
		this.distanceFuzzyFactors = distanceFuzzyFactors;
	}
	
	@Override
	public double getLinkTravelTime(Link link, double time) {
			
		double tt = this.travelTime.getLinkTravelTime(link, time);
				
		double distanceFuzzyFactor = calcDistanceFuzzyFactor(link);
		double linkFuzzyFactor = calcLinkFuzzyFactor(link);
		
		/*
		 * Sum of the three factors is between ~0.0 and 3.0
		 * Shift it by -1.5 and scale it by 3 to have an interval 
		 * between -0.50..0.50
		 */
		double factor = (personFuzzyFactor + distanceFuzzyFactor + linkFuzzyFactor - 1.5)/3;
		double ttError = tt * factor;
		
		return tt + ttError;
	}

	@Override
	public void setPerson(Person person) {
		this.travelTime.setPerson(person);

		/* 
		 * Only recalculate the person's HashCode and FuzzyFactor
		 * if the person really has changed.
		 */
		if (person.getId().equals(this.pId)) return;
		
		// person has changed
		this.pId = person.getId();
		this.pIdHashCode = person.getId().toString().hashCode();
		this.personFuzzyFactor = hashCodeToRandomDouble(pIdHashCode);
	}

	/*
	 * So far use hard-coded values between 0.017 (distance 0.0) 
	 * and 1.0 (distance ~ 15000.0).
	 */
	private double calcDistanceFuzzyFactor(Link link) {		
		AgentPosition agentPosition = this.agentsTracker.getAgentPosition(pId);
		Position positionType = agentPosition.getPositionType();
		Coord coord = null;
		Map<Id, Double> fuzzyFactors = null;
		if (positionType == Position.LINK) {
			// check whether there is already a value in the lookup map
			fuzzyFactors = distanceFuzzyFactors.get(agentPosition.getPositionId());
			Double fuzzyFactor = fuzzyFactors.get(link.getId());
			if (fuzzyFactor != null) return fuzzyFactor;
			
			coord = scenario.getNetwork().getLinks().get(agentPosition.getPositionId()).getCoord();
		} else if (positionType == Position.FACILITY) {
			coord = ((ScenarioImpl) scenario).getActivityFacilities().getFacilities().get(agentPosition.getPositionId()).getCoord();
		} else if (positionType == Position.VEHICLE) {
			Id linkId = vehiclesTracker.getVehicleLinkId(agentPosition.getPositionId());
			
			// check whether there is already a value in the lookup map
			fuzzyFactors = distanceFuzzyFactors.get(linkId);
			Double fuzzyFactor = fuzzyFactors.get(link.getId());
			if (fuzzyFactor != null) return fuzzyFactor;

			coord = scenario.getNetwork().getLinks().get(linkId).getCoord();
		} else {
			log.warn("Agent's position is undefined! Id: " + this.pId);
			return 1.0;
		}
		
		double distance = CoordUtils.calcDistance(coord, link.getCoord());
		
		double factor = 1 / (1 + Math.exp((-distance/1500.0) + 4.0));
		
		// if the person is located at a link, add the link factor to the lookup map
		if (fuzzyFactors != null) fuzzyFactors.put(link.getId(), factor);
		
		return factor;
	}
	
	/*
	 * Returns a fuzzy value between 0.0 and 1.0 which
	 * depends on the current person and the given link. 
	 */
	private double calcLinkFuzzyFactor(Link link) {	
		int lIdHashCode = link.getId().toString().hashCode();
		return hashCodeToRandomDouble(lIdHashCode + pIdHashCode);
	}
	
	public static void main(String[] args) {
		FuzzyTravelTimeEstimator ftte = new FuzzyTravelTimeEstimator(null, null, null, null, null);

		Gbl.startMeasurement();
		double sum = 0.0;
		int iters = 10000000;
		for (int i = 0; i < iters; i++) {
			sum += ftte.hashCodeToRandomDouble(i);
		}
		Gbl.printElapsedTime();
	}
	
	/*
	 * Creates a random double value between 0.0 and 1.0 based
	 * on an integer hash value.
	 */
	private double hashCodeToRandomDouble(int hashCode) {
		
		/*
		 *  Small numbers represented as a String return small hash values.
		 *  By doing this operation, we create higher values that result
		 *  in larger differences between two input String (e.g. "1" and "2").
		 */
		hashCode ^= (hashCode << 13);
		hashCode ^= (hashCode >>> 17);
		hashCode ^= (hashCode << 5);

		Long modValue = hashCode % (hashCodeModFactor);
		double value = modValue.doubleValue() / (hashCodeModFactor);
		return Math.abs(value);
	}

}