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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.router.util.PersonalizableTravelTime;
import org.matsim.core.scenario.ScenarioImpl;

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
	private final DistanceFuzzyFactorProvider distanceFuzzyFactorProvider;
	
	private Id pId;
	private int pIdHashCode;
	private double personFuzzyFactor;
	
	/*package*/ FuzzyTravelTimeEstimator(Scenario scenario, PersonalizableTravelTime travelTime, AgentsTracker agentsTracker,
			VehiclesTracker vehiclesTracker, DistanceFuzzyFactorProvider distanceFuzzyFactorProvider) {
		this.scenario = scenario;
		this.travelTime = travelTime;
		this.agentsTracker = agentsTracker;
		this.vehiclesTracker = vehiclesTracker;
		this.distanceFuzzyFactorProvider = distanceFuzzyFactorProvider;
	}
	
	@Override
	public double getLinkTravelTime(Link link, double time) {
			
		double tt = this.travelTime.getLinkTravelTime(link, time);
				
		double distanceFuzzyFactor = calcDistanceFuzzyFactor(link);
		double linkFuzzyFactor = calcLinkFuzzyFactor(link);
		
		/*
		 * personFuzzyFactor and linkFuzzyFactor are uniform distributed,
		 * combined they result in a normal distribution.
		 * 
		 * "(personFuzzyFactor + linkFuzzyFactor - 1)/4" results in a 
		 * normal distribution with values between -0.25..0.25.
		 * 
		 * distanceFuzzyFactor is used to increase the fuzzyness for link
		 * that are far away. Its range is from 0..1.
		 * 
		 * Multiplying the factor with "distanceFuzzyFactor + 1" (1..2)
		 * results in a range of -0.50 .. 0.50 for the factor.
		 */
		double factor = ((personFuzzyFactor + linkFuzzyFactor - 1.0) / 4) * (distanceFuzzyFactor + 1);
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
		this.pIdHashCode = person.getId().hashCode();
		this.personFuzzyFactor = hashCodeToRandomDouble(pIdHashCode);
	}

	/*
	 * So far use hard-coded values between 0.017 (distance 0.0) 
	 * and 1.0 (distance ~ 15000.0).
	 */
	private double calcDistanceFuzzyFactor(Link link) {		
		AgentPosition agentPosition = this.agentsTracker.getAgentPosition(pId);
		Position positionType = agentPosition.getPositionType();

		Id linkId = null;
		if (positionType == Position.LINK) {
			linkId = agentPosition.getPositionId();
		} else if (positionType == Position.FACILITY) {
			linkId = ((ScenarioImpl) scenario).getActivityFacilities().getFacilities().get(agentPosition.getPositionId()).getLinkId();
		} else if (positionType == Position.VEHICLE) {
			linkId = vehiclesTracker.getVehicleLinkId(agentPosition.getPositionId());			
		} else {
			log.warn("Agent's position is undefined! Id: " + this.pId);
			return 1.0;
		}
		return distanceFuzzyFactorProvider.getFuzzyFactor(linkId, link.getId());
	}
	
	/*
	 * Returns a fuzzy value between 0.0 and 1.0 which
	 * depends on the current person and the given link. 
	 */
	private double calcLinkFuzzyFactor(Link link) {	
		int lIdHashCode = link.getId().hashCode();
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