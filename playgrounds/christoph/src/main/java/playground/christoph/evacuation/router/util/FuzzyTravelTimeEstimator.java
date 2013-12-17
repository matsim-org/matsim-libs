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
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;
import org.matsim.withinday.mobsim.MobsimDataProvider;

import playground.christoph.evacuation.mobsim.AgentPosition;
import playground.christoph.evacuation.mobsim.AgentsTracker;
import playground.christoph.evacuation.mobsim.Tracker.Position;
import playground.christoph.evacuation.utils.DeterministicRNG;

/**
 * Get travel times from a given travel time calculator and adds a random fuzzy
 * value. This values depends on the agent, the link and the distance between
 * the link and the agent's current position. The calculation of the fuzzy
 * values is deterministic, multiple calls with identical parameters will result
 * in identical return values.
 * 
 * @author cdobler
 */
public class FuzzyTravelTimeEstimator implements TravelTime {

	private static final Logger log = Logger.getLogger(FuzzyTravelTimeEstimator.class);

	private final Scenario scenario;
	private final TravelTime travelTime;
	private final AgentsTracker agentsTracker;
	private final MobsimDataProvider mobsimDataProvider;
	private final DistanceFuzzyFactorProvider distanceFuzzyFactorProvider;
	private final DeterministicRNG rng;

	/*
	 * Cache variables for each thread accessing the object separately.
	 */
	private ThreadLocal<Id> pIdCache;
	private ThreadLocal<Integer> pIdHashCodeCache;
	private ThreadLocal<Double> personFuzzyFactorCache;
	private ThreadLocal<AgentPosition> agentPositionCache;
	private ThreadLocal<Position> positionTypeCache;
	private ThreadLocal<Id> fromLinkIdCache;
	private ThreadLocal<Link> fromLinkCache;
	private ThreadLocal<Boolean> fromLinkIsObservedCache;

	/* package */FuzzyTravelTimeEstimator(Scenario scenario, TravelTime travelTime, AgentsTracker agentsTracker,
			MobsimDataProvider mobsimDataProvider, DistanceFuzzyFactorProvider distanceFuzzyFactorProvider,
			long rngInitialValue) {
		this.scenario = scenario;
		this.travelTime = travelTime;
		this.agentsTracker = agentsTracker;
		this.mobsimDataProvider = mobsimDataProvider;
		this.distanceFuzzyFactorProvider = distanceFuzzyFactorProvider;
		this.rng = new DeterministicRNG(rngInitialValue);

		this.pIdCache = new ThreadLocal<Id>();
		this.pIdHashCodeCache = new ThreadLocal<Integer>();
		this.personFuzzyFactorCache = new ThreadLocal<Double>();
		this.agentPositionCache = new ThreadLocal<AgentPosition>();
		this.positionTypeCache = new ThreadLocal<Position>();
		this.fromLinkIdCache = new ThreadLocal<Id>();
		this.fromLinkCache = new ThreadLocal<Link>();
		this.fromLinkIsObservedCache = new ThreadLocal<Boolean>();
	}

	@Override
	public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {
		setPerson(person);
		double tt = this.travelTime.getLinkTravelTime(link, time, person, vehicle);

		double distanceFuzzyFactor = calcDistanceFuzzyFactor(link);
		double linkFuzzyFactor = calcLinkFuzzyFactor(link);

		/*
		 * personFuzzyFactor and linkFuzzyFactor are uniform distributed,
		 * combined they result in a normal distribution.
		 * 
		 * "(personFuzzyFactor + linkFuzzyFactor - 1)/4" results in a normal
		 * distribution with values between -0.25..0.25.
		 * 
		 * distanceFuzzyFactor is used to increase the fuzzyness for link that
		 * are far away. Its range is from 0..1.
		 * 
		 * Multiplying the factor with "distanceFuzzyFactor + 1" (1..2) results
		 * in a range of -0.50 .. 0.50 for the factor.
		 */
		double factor = ((personFuzzyFactorCache.get() + linkFuzzyFactor - 1.0) / 4) * (distanceFuzzyFactor + 1);
		double ttError = tt * factor;

		return tt + ttError;
	}

	private void setPerson(Person person) {

		/*
		 * Only recalculate the person's HashCode and FuzzyFactor if the person
		 * really has changed.
		 */
		if (person.getId().equals(this.pIdCache.get())) return;

		// person has changed
		Id personId = person.getId();
		int hashCode = personId.hashCode();
		AgentPosition agentPosition = this.agentsTracker.getAgentPosition(personId);

		this.pIdCache.set(personId);
		this.pIdHashCodeCache.set(hashCode);
		this.personFuzzyFactorCache.set(rng.hashCodeToRandomDouble(hashCode));
		this.agentPositionCache.set(agentPosition);
		this.positionTypeCache.set(agentPosition.getPositionType());
	}

	/*
	 * So far use hard-coded values between 0.017 (distance 0.0) and 1.0
	 * (distance ~ 10000.0).
	 */
	private double calcDistanceFuzzyFactor(Link toLink) {
		/*
		 * AgentPosition and PositionType are now updated in the setPerson(...)
		 * method. cdobler, jul'12.
		 */
		// AgentPosition agentPosition =
		// this.agentsTracker.getAgentPosition(pId);
		// Position positionType = agentPosition.getPositionType();

		Id fromLinkId = null;
		Position positionType = this.positionTypeCache.get();
		AgentPosition agentPosition = this.agentPositionCache.get();
		if (positionType == Position.LINK) {
			fromLinkId = agentPosition.getPositionId();
		} else if (positionType == Position.FACILITY) {
			fromLinkId = scenario.getActivityFacilities().getFacilities().get(agentPosition.getPositionId()).getLinkId();
		} else if (positionType == Position.VEHICLE) {
			fromLinkId = this.mobsimDataProvider.getVehicle(agentPosition.getPositionId()).getCurrentLink().getId();
		} else {
			log.warn("Agent's position is undefined! Id: " + this.pIdCache.get());
			return 1.0;
		}

		if (!fromLinkId.equals(this.fromLinkIdCache.get())) {
			this.fromLinkIdCache.set(fromLinkId);
			this.fromLinkCache.set(this.scenario.getNetwork().getLinks().get(fromLinkId));
			this.fromLinkIsObservedCache.set(distanceFuzzyFactorProvider.isLinkObserved(fromLinkId));
		}

		/*
		 * If the agent's current link is not observed, the distance fuzzy
		 * factor is 0.0, therefore we do not have to check the
		 * DistanceFuzzyFactorProviders return value. cdobler, jul'12
		 */
		if (!this.fromLinkIsObservedCache.get()) return 0.0;
		return distanceFuzzyFactorProvider.getFuzzyFactor(fromLinkCache.get(), toLink);
	}

	/*
	 * Returns a fuzzy value between 0.0 and 1.0 which depends on the current
	 * person and the given link.
	 */
	private double calcLinkFuzzyFactor(Link link) {
		int lIdHashCode = link.getId().hashCode();
		return rng.hashCodeToRandomDouble(lIdHashCode + pIdHashCodeCache.get());
	}

}