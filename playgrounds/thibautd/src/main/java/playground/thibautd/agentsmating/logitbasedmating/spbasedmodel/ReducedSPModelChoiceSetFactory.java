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

import org.apache.log4j.Logger;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.misc.RouteUtils;
import org.matsim.planomat.costestimators.LegTravelTimeEstimator;
import org.matsim.pt.router.TransitRouter;

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
	private static final Logger log =
		Logger.getLogger(ReducedSPModelChoiceSetFactory.class);

	private final SimpleLegTravelTimeEstimatorFactory estimatorFactory;
	private final Network network;
	private final Population population;
	private final  ReducedModelParametersConfigGroup configGroup;
	private final TransitRouter transitRouter;

	private int callCount = 0;

	// /////////////////////////////////////////////////////////////////////////
	// constructor
	// /////////////////////////////////////////////////////////////////////////
	/**
	 * short for <tt>ReducedSPModelChoiceSetFactory( configGroup, scenario, estimatorFactory, null )</tt>
	 * @param configGroup 
	 * @param scenario 
	 * @param estimatorFactory 
	 */
	public ReducedSPModelChoiceSetFactory(
			final ReducedModelParametersConfigGroup configGroup,
			final Scenario scenario,
			final SimpleLegTravelTimeEstimatorFactory estimatorFactory) {
		this( configGroup, scenario, estimatorFactory, null );
	}

	/**
	 * @param configGroup the parameters
	 * @param scenario A scenario containing the population of interest, at
	 * equilibrium (the departure and arrival times from the plans will be considered
	 * as the "desired" ones)
	 * @param estimatorFactory a factory for travel time estimators for non-pt legs
	 * @param transitRouter a router for detailed pt travel time estimation. Can be null,
	 * an than the estimator from <tt>transitRouter</tt> will be used
	 */
	public ReducedSPModelChoiceSetFactory(
			final ReducedModelParametersConfigGroup configGroup,
			final Scenario scenario,
			final SimpleLegTravelTimeEstimatorFactory estimatorFactory,
			final TransitRouter transitRouter) {
		this.configGroup = configGroup;
		this.estimatorFactory = estimatorFactory;
		this.network = scenario.getNetwork();
		this.population = scenario.getPopulation();
		this.transitRouter = transitRouter;

		if (transitRouter == null) {
			log.debug("init "+getClass().getSimpleName()+" without fine PT travel time estimation.");
		}
		else {
			log.debug("init "+getClass().getSimpleName()+" with fine PT travel time estimation.");
		}
	}

	// /////////////////////////////////////////////////////////////////////////
	// interface
	// /////////////////////////////////////////////////////////////////////////
	@Override
	public List<Alternative> createChoiceSet(
			final DecisionMaker decisionMaker,
			final Plan plan,
			final int indexOfLeg) {
		callCount++;
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
		
		if (departureTime < 0) {
			throw new RuntimeException("cannot handle negative time, got departure time "+
					departureTime+" secs for agent "+decisionMaker.getPersonId());
		}

		double arrivalTime = departureTime + leg.getTravelTime();

		double carTravelTime, ptTravelTime;
		double carTravelCost, ptTravelCost;
		double parkingCost;
		double distance;
		double walkingTime = 0;
		double waitingTime = 0;
		int nTransfers = 0;

		// car
		// ---------------------------------------------------------------------
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
		carTravelCost = distance * configGroup.getCarCostPerM();
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
		// ---------------------------------------------------------------------
		walkingTime = 0d;
		waitingTime = 0d;
		distance = 0;
		if (transitRouter != null) {
			List<Leg> ptLegs = transitRouter.calcRoute(
					origin.getCoord(),
					destination.getCoord(),
					departureTime,
					population.getPersons().get( personId ) );

			if (ptLegs != null && ptLegs.size() > 0) {
				ptTravelTime = 0;

				double lastArrivalTime = Double.NaN;
				for (Leg ptLeg : ptLegs) {
					if (!Double.isNaN( lastArrivalTime )) {
						waitingTime += ptLeg.getDepartureTime() - lastArrivalTime;
					}
					lastArrivalTime = ptLeg.getDepartureTime() + ptLeg.getTravelTime();

					if ( ptLeg.getMode().equals( TransportMode.walk ) ||
							ptLeg.getMode().equals( TransportMode.transit_walk ) ) {
						walkingTime += ptLeg.getTravelTime();
					}
					else {
						ptTravelTime += ptLeg.getTravelTime();
					}
				}

				nTransfers = Math.max( ptLegs.size() - 3 , 0 );
			}
			else {
				// it is not clear whether this can happen or not (a quick glance
				// at the code makes think that yes, but it is not documented)
				log.warn( "no valid pt route obtained: setting pt travel time to infinity" );
				ptTravelTime = Double.POSITIVE_INFINITY;
			}
		}
		else {
			leg.setMode( TransportMode.pt );
			ptTravelTime = ttEstimator.getLegTravelTimeEstimation(
					personId,
					departureTime,
					origin,
					destination,
					leg,
					true);
		}

		if (!Double.isInfinite( ptTravelTime )) {
			// the PT router does not allow to obtain easily the network distance:
			// use a bee-fly estimate
			distance = CoordUtils.calcDistance(
					origin.getCoord(),
					destination.getCoord() );

			ptTravelCost = distance * getPtTravelCostPerM( decisionMaker );

			currentAlternative = createPtAlternative(
					ptTravelTime,
					ptTravelCost,
					walkingTime,
					waitingTime,
					nTransfers);

			allAlternatives.add( currentAlternative );
			nonCpAlternatives.add( currentAlternative );

			nonCpAlternatives = Collections.unmodifiableList( nonCpAlternatives );
		}

		// cpd
		// ---------------------------------------------------------------------
		walkingTime = 0d;
		currentAlternative = createCpdAlternative(
				carTravelTime + configGroup.getSurplusDriver(),
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
		// ---------------------------------------------------------------------
		walkingTime = 0d;
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

	// /////////////////////////////////////////////////////////////////////////
	// helpers
	// /////////////////////////////////////////////////////////////////////////
	private double getPtTravelCostPerM( final DecisionMaker decisionMaker ) {
		if ( (Boolean) decisionMaker.getAttributes().get( ReducedModelConstants.A_HAS_GENERAL_ABO ) ) {
			return configGroup.getGaCostPerM();
		}

		if ( (Boolean) decisionMaker.getAttributes().get( ReducedModelConstants.A_HAS_HALBTAX ) ) {
			return configGroup.getHtCostPerM();
		}

		return configGroup.getPtCostPerM();
	}

	// /////////////////////////////////////////////////////////////////////////
	// actual creation methods
	// /////////////////////////////////////////////////////////////////////////
	private static Alternative createCarAlternative(
			final double travelTime,
			final double travelCost,
			final double parkingCost,
			final double walkingTime) {
		Map<String, Object> attributes = new HashMap<String, Object>();

		attributes.put( ReducedModelConstants.A_TRAVEL_TIME , travelTime );
		attributes.put( ReducedModelConstants.A_COST , travelCost );
		attributes.put( ReducedModelConstants.A_PARK_COST , parkingCost );
		attributes.put( ReducedModelConstants.A_WALKING_TIME , walkingTime );

		return new AlternativeImpl( TransportMode.car , attributes );
	}

	private static Alternative createCppAlternative(
			final double travelTime,
			final double travelCost,
			final double walkingTime) {
		Map<String, Object> attributes = new HashMap<String, Object>();

		attributes.put( ReducedModelConstants.A_TRAVEL_TIME , travelTime );
		attributes.put( ReducedModelConstants.A_COST , travelCost );
		attributes.put( ReducedModelConstants.A_WALKING_TIME , walkingTime );

		return new AlternativeImpl( TripRequestImpl.PASSENGER_MODE , attributes );
	}

	private static Alternative createCpdAlternative(
			final double travelTime,
			final double travelCost,
			final double parkingCost,
			final double walkingTime) {
		Map<String, Object> attributes = new HashMap<String, Object>();

		attributes.put( ReducedModelConstants.A_TRAVEL_TIME , travelTime );
		attributes.put( ReducedModelConstants.A_COST , travelCost );
		attributes.put( ReducedModelConstants.A_PARK_COST , parkingCost );
		attributes.put( ReducedModelConstants.A_WALKING_TIME , walkingTime );

		return new AlternativeImpl( TripRequestImpl.DRIVER_MODE , attributes );
	}

	private static Alternative createPtAlternative(
			final double travelTime,
			final double travelCost,
			final double walkingTime,
			final double waitingTime,
			final int nTransfers) {
		Map<String, Object> attributes = new HashMap<String, Object>();

		attributes.put( ReducedModelConstants.A_TRAVEL_TIME , travelTime );
		attributes.put( ReducedModelConstants.A_COST , travelCost );
		attributes.put( ReducedModelConstants.A_WALKING_TIME , walkingTime );
		attributes.put( ReducedModelConstants.A_WAITING_TIME , waitingTime );
		attributes.put( ReducedModelConstants.A_N_TRANSFERS , nTransfers );

		return new AlternativeImpl( TransportMode.pt , attributes );
	}

	public void notifyAffectationProcedureEnd() {
		log.info( "########### post-affectation procedure statistics: ##########" );
		log.info( "number of calls to createChoiceSet(): "+callCount );
	}
}

