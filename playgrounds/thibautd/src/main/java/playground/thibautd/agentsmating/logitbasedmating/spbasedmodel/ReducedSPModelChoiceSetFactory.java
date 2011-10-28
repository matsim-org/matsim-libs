/* *********************************************************************** *
 * project: org.matsim.*
 * ReducedModelChoiceSetFactory.java
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
package playground.thibautd.agentsmating.logitbasedmating.spbasedmodel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.utils.misc.RouteUtils;
import org.matsim.planomat.costestimators.LegTravelTimeEstimator;

import playground.thibautd.agentsmating.logitbasedmating.basic.AlternativeImpl;
import playground.thibautd.agentsmating.logitbasedmating.basic.TripRequestImpl;
import playground.thibautd.agentsmating.logitbasedmating.framework.Alternative;
import playground.thibautd.agentsmating.logitbasedmating.framework.ChoiceSetFactory;
import playground.thibautd.agentsmating.logitbasedmating.framework.DecisionMaker;
import playground.thibautd.agentsmating.logitbasedmating.utils.SimpleLegTravelTimeEstimatorFactory;

/**
 * @author thibautd
 */
public class ReducedSPModelChoiceSetFactory implements ChoiceSetFactory {
	private static final double CAR_COST_PER_M = 0; // CHF/m
	private static final double SURPLUS_DRIVER = 15 * 60;

	private final SimpleLegTravelTimeEstimatorFactory estimatorFactory;
	private final Network network;

	public ReducedSPModelChoiceSetFactory(
			final Network network,
			final SimpleLegTravelTimeEstimatorFactory estimatorFactory) {
		this.estimatorFactory = estimatorFactory;
		this.network = network;
	}

	@Override
	public List<Alternative> createChoiceSet(
			final DecisionMaker decisionMaker,
			final Plan plan,
			final int indexOfLeg) {
		List<Alternative> nonCpAlternatives = new ArrayList<Alternative>(2);
		List<Alternative> allAlternatives = new ArrayList<Alternative>(4);
		Alternative currentAlternative;

		LegTravelTimeEstimator ttEstimator = 
			estimatorFactory.createLegTravelTimeEstimator( plan );
		Id personId = plan.getPerson().getId();

		Activity origin = (Activity) plan.getPlanElements().get( indexOfLeg - 1 );
		Activity destination = (Activity) plan.getPlanElements().get( indexOfLeg + 1 );
		Leg leg = (Leg) plan.getPlanElements().get( indexOfLeg );
		double departureTime = origin.getEndTime();
		double arrivalTime = departureTime + leg.getTravelTime();

		double carTravelTime, ptTravelTime;
		double carTravelCost, ptTravelCost;
		double parkingCost;
		double distance;
		double walkingTime = 0;
		double waitingTime = 0;
		int nTransfers = 0;

		// car
		leg.setMode( TransportMode.car );
		carTravelTime = ttEstimator.getLegTravelTimeEstimation(
				personId,
				departureTime,
				origin,
				destination,
				leg,
				true); // modify back leg, to obtain route
		distance = RouteUtils.calcDistance(
				(NetworkRoute) leg.getRoute(),
				network);
		carTravelCost = distance * CAR_COST_PER_M;
		parkingCost = 0d;
		walkingTime = 0d;
		currentAlternative = createCarAlternative(
				carTravelTime,
				carTravelCost,
				parkingCost,
				walkingTime);

		allAlternatives.add( currentAlternative );
		nonCpAlternatives.add( currentAlternative );

		// pt
		leg.setMode( TransportMode.pt );
		ptTravelTime = ttEstimator.getLegTravelTimeEstimation(
				personId,
				departureTime,
				origin,
				destination,
				leg,
				true); // modify back leg, to obtain route
		//distance = RouteUtils.calcDistance(
		//		(NetworkRoute) leg.getRoute(),
		//		network);
		ptTravelCost = 0d;
		walkingTime = 0d;
		waitingTime = 0d;
		currentAlternative = createPtAlternative(
				ptTravelTime,
				ptTravelCost,
				walkingTime,
				waitingTime,
				nTransfers);

		allAlternatives.add( currentAlternative );
		nonCpAlternatives.add( currentAlternative );

		nonCpAlternatives = Collections.unmodifiableList( nonCpAlternatives );
		// cpd
		currentAlternative = createCpdAlternative(
				carTravelTime + SURPLUS_DRIVER,
				carTravelCost / 2d,
				parkingCost,
				walkingTime);
		allAlternatives.add( new TripRequestImpl(
			currentAlternative,
			indexOfLeg,
			origin,
			destination,
			arrivalTime,
			decisionMaker,
			nonCpAlternatives) );

		// cpp
		currentAlternative = createCppAlternative(
				carTravelTime,
				carTravelCost / 2d,
				walkingTime);
		allAlternatives.add( new TripRequestImpl(
			currentAlternative,
			indexOfLeg,
			origin,
			destination,
			arrivalTime,
			decisionMaker,
			nonCpAlternatives) );

		return allAlternatives;
	}

	private Alternative createCarAlternative(
			final double travelTime,
			final double travelCost,
			final double parkingCost,
			final double walkingTime) {
		Map<String, Object> attributes = new HashMap<String, Object>();

		attributes.put( ReducedSPModel.A_TRAVEL_TIME , travelTime );
		attributes.put( ReducedSPModel.A_COST , travelCost );
		attributes.put( ReducedSPModel.A_PARK_COST , parkingCost );
		attributes.put( ReducedSPModel.A_WALKING_TIME , walkingTime );

		return new AlternativeImpl( TransportMode.car , attributes );
	}

	private Alternative createCppAlternative(
			final double travelTime,
			final double travelCost,
			final double walkingTime) {
		Map<String, Object> attributes = new HashMap<String, Object>();

		attributes.put( ReducedSPModel.A_TRAVEL_TIME , travelTime );
		attributes.put( ReducedSPModel.A_COST , travelCost );
		attributes.put( ReducedSPModel.A_WALKING_TIME , walkingTime );

		return new AlternativeImpl( TripRequestImpl.PASSENGER_MODE , attributes );
	}

	private Alternative createCpdAlternative(
			final double travelTime,
			final double travelCost,
			final double parkingCost,
			final double walkingTime) {
		Map<String, Object> attributes = new HashMap<String, Object>();

		attributes.put( ReducedSPModel.A_TRAVEL_TIME , travelTime );
		attributes.put( ReducedSPModel.A_COST , travelCost );
		attributes.put( ReducedSPModel.A_PARK_COST , parkingCost );
		attributes.put( ReducedSPModel.A_WALKING_TIME , walkingTime );

		return new AlternativeImpl( TripRequestImpl.DRIVER_MODE , attributes );
	}

	private Alternative createPtAlternative(
			final double travelTime,
			final double travelCost,
			final double walkingTime,
			final double waitingTime,
			final int nTransfers) {
		Map<String, Object> attributes = new HashMap<String, Object>();

		attributes.put( ReducedSPModel.A_TRAVEL_TIME , travelTime );
		attributes.put( ReducedSPModel.A_COST , travelCost );
		attributes.put( ReducedSPModel.A_WALKING_TIME , walkingTime );
		attributes.put( ReducedSPModel.A_WAITING_TIME , waitingTime );
		attributes.put( ReducedSPModel.A_N_TRANSFERS , nTransfers );

		return new AlternativeImpl( TransportMode.pt , attributes );
	}
}

