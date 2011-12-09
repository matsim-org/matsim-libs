/* *********************************************************************** *
 * project: org.matsim.*
 * EstimatedModel.java
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

import java.lang.Override;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.router.util.LeastCostPathCalculator;

import playground.thibautd.agentsmating.logitbasedmating.basic.LogitModel;
import playground.thibautd.agentsmating.logitbasedmating.basic.TripRequestImpl;
import playground.thibautd.agentsmating.logitbasedmating.framework.Alternative;
import playground.thibautd.agentsmating.logitbasedmating.framework.ChoiceSetFactory;
import playground.thibautd.agentsmating.logitbasedmating.framework.DecisionMaker;
import playground.thibautd.agentsmating.logitbasedmating.framework.DecisionMakerFactory;
import playground.thibautd.agentsmating.logitbasedmating.framework.TripRequest;
import playground.thibautd.agentsmating.logitbasedmating.framework.UnexistingAttributeException;
import playground.thibautd.agentsmating.logitbasedmating.utils.SimpleLegTravelTimeEstimatorFactory;

/**
 * @author thibautd
 */
public class ReducedSPModel extends LogitModel {
	// factories
	private final DecisionMakerFactory decisionMakerFactory;
	private final ChoiceSetFactory choiceSetFactory;

	private final LeastCostPathCalculator leastCostAlgo;
	private final ReducedModelParametersConfigGroup params;

	private final Network network;

	// /////////////////////////////////////////////////////////////////////////
	// constructor
	// /////////////////////////////////////////////////////////////////////////
	public ReducedSPModel(
			final ReducedModelParametersConfigGroup parameters,
			final Scenario scenario,
			final SimpleLegTravelTimeEstimatorFactory estimatorFactory,
			final LeastCostPathCalculator leastCostAlgo) {
		this.params = parameters;
		this.network = scenario.getNetwork();
		this.leastCostAlgo = leastCostAlgo;
		choiceSetFactory = new ReducedSPModelChoiceSetFactory(
				parameters,
				scenario,
				estimatorFactory);
		decisionMakerFactory = new ReducedSPModelDecisionMakerFactory( parameters );
	}

	// /////////////////////////////////////////////////////////////////////////
	// getters
	// /////////////////////////////////////////////////////////////////////////
	@Override
	public DecisionMakerFactory getDecisionMakerFactory() {
		return decisionMakerFactory;
	}

	@Override
	public ChoiceSetFactory getChoiceSetFactory() {
		return choiceSetFactory;
	}

	// /////////////////////////////////////////////////////////////////////////
	// utility
	// /////////////////////////////////////////////////////////////////////////
	@Override
	public double getSystematicUtility(
			final DecisionMaker decisionMaker,
			final Alternative alternative) {
		try {
			if (alternative instanceof TripRequest) {
				switch ( ((TripRequest) alternative).getTripType() ) {
					case DRIVER:
						return cpdUtility( decisionMaker , alternative );
					case PASSENGER:
						return cppUtility( decisionMaker , alternative );
					default:
						throw new RuntimeException("unhandled trip type");
				}
			}
			
			String mode = alternative.getMode();

			if ( mode.equals( TransportMode.car ) ) {
				return carUtility( decisionMaker , alternative );
			}

			if ( mode.equals( TransportMode.pt ) ) {
				return ptUtility( decisionMaker , alternative );
			}
		} catch (UnexistingAttributeException e) {
			throw new RuntimeException( e );
		}

		throw new RuntimeException("unhandled mode: "+alternative.getMode());
	}

	private double carUtility(
			final DecisionMaker decisionMaker,
			final Alternative alternative) throws UnexistingAttributeException {
		return
			params.ascCar() +
			params.betaTtCar() * alternative.getAttribute( ReducedModelConstants.A_TRAVEL_TIME ) +
			params.betaCost() * alternative.getAttribute( ReducedModelConstants.A_COST ) +
			params.betaWalkCar() * alternative.getAttribute( ReducedModelConstants.A_WALKING_TIME ) +
			params.betaParkCar() * alternative.getAttribute( ReducedModelConstants.A_PARK_COST ) +
			params.betaMaleCar() * decisionMaker.getAttribute( ReducedModelConstants.A_IS_MALE ) +
			params.betaCarAvail() * decisionMaker.getAttribute( ReducedModelConstants.A_IS_CAR_ALWAYS_AVAIL );
	}

	private double ptUtility(
			final DecisionMaker decisionMaker,
			final Alternative alternative) throws UnexistingAttributeException {
		return
			params.ascPt() +
			params.betaTtPt() * alternative.getAttribute( ReducedModelConstants.A_TRAVEL_TIME ) +
			params.betaCost() * alternative.getAttribute( ReducedModelConstants.A_COST ) +
			params.betaWalkPt() * alternative.getAttribute( ReducedModelConstants.A_WALKING_TIME ) +
			params.betaWaitPt() * alternative.getAttribute( ReducedModelConstants.A_WAITING_TIME ) +
			params.betaAboPt() * decisionMaker.getAttribute( ReducedModelConstants.A_HAS_GENERAL_ABO ) +
			params.betaLogAgePt() * Math.log( decisionMaker.getAttribute( ReducedModelConstants.A_AGE ) ) +
			params.betaTransfersPt() * alternative.getAttribute( ReducedModelConstants.A_N_TRANSFERS );
	}

	private double cppUtility(
			final DecisionMaker decisionMaker,
			final Alternative alternative) throws UnexistingAttributeException {
		return
			params.betaTtCpp() * alternative.getAttribute( ReducedModelConstants.A_TRAVEL_TIME ) +
			params.betaCost() * alternative.getAttribute( ReducedModelConstants.A_COST ) +
			params.betaWalkCpp() * alternative.getAttribute( ReducedModelConstants.A_WALKING_TIME ) +
			params.betaFemaleCp() * (1 - decisionMaker.getAttribute( ReducedModelConstants.A_IS_MALE )) +
			params.betaGermanCp() * decisionMaker.getAttribute( ReducedModelConstants.A_SPEAKS_GERMAN );
	}

	private double cpdUtility(
			final DecisionMaker decisionMaker,
			final Alternative alternative) throws UnexistingAttributeException {
		return
			params.ascCpd() +
			params.betaTtCpd() * alternative.getAttribute( ReducedModelConstants.A_TRAVEL_TIME ) +
			params.betaCost() * alternative.getAttribute( ReducedModelConstants.A_COST ) +
			params.betaWalkCpd() * alternative.getAttribute( ReducedModelConstants.A_WALKING_TIME ) +
			params.betaParkCpd() * alternative.getAttribute( ReducedModelConstants.A_PARK_COST ) +
			params.betaFemaleCp() * (1 - decisionMaker.getAttribute( ReducedModelConstants.A_IS_MALE )) +
			params.betaGermanCp() * decisionMaker.getAttribute( ReducedModelConstants.A_SPEAKS_GERMAN );
	}


	// /////////////////////////////////////////////////////////////////////////
	// perspective changing
	// /////////////////////////////////////////////////////////////////////////
	/**
	 * CHanges the perspective in the following way:
	 * <ul>
	 * <li> for a passenger, nothing is done: the "perspective" is returned
	 * <li> for a driver, the travel time is set using a bee fly estimate for
	 * pick-up and drop-off legs, and the trip duration set in the passenger leg.
	 * </ul>
	 * 
	 * @throws IllegalArgumentException if the perspective is neither DRIVER or PASSENGER
	 * or if the passenger TripToConsider does not have a travel time attribute.
	 */
	@Override
	public TripRequest changePerspective(
			final TripRequest tripToConsider,
			final TripRequest perspective) {
		try {
			switch ( perspective.getTripType() ) {
				case PASSENGER:
					return perspective;

				case DRIVER:
					double ttEstimate = tripToConsider.getAttribute( ReducedModelConstants.A_TRAVEL_TIME );
					double tCostEstimate =  tripToConsider.getAttribute( ReducedModelConstants.A_COST );

					// access to pick up
					LeastCostPathCalculator.Path path =
						leastCostAlgo.calcLeastCostPath(
							network.getLinks().get( perspective.getOrigin().getLinkId() ).getFromNode(),
							network.getLinks().get( tripToConsider.getOrigin().getLinkId() ).getFromNode(),
							perspective.getDepartureTime());
					ttEstimate += path.travelTime;

					double dist = 0;
					for (Link link : path.links) {
						dist += link.getLength();
					}
					tCostEstimate += dist * params.getCarCostPerM();

					// egres from drop off
					path =
						leastCostAlgo.calcLeastCostPath(
							network.getLinks().get( tripToConsider.getDestination().getLinkId() ).getFromNode(),
							network.getLinks().get( perspective.getDestination().getLinkId() ).getFromNode(),
							perspective.getDepartureTime() + ttEstimate );
					ttEstimate += path.travelTime;

					dist = 0;
					for (Link link : path.links) {
						dist += link.getLength();
					}
					tCostEstimate += dist * params.getCarCostPerM();

					Map<String , Object> attrs = new HashMap<String , Object>(perspective.getAttributes());
					attrs.put( ReducedModelConstants.A_TRAVEL_TIME , ttEstimate );
					attrs.put( ReducedModelConstants.A_COST , tCostEstimate );

					return new TripRequestImpl(
							perspective.getMode(),
							attrs,
							perspective.getIndexInPlan(),
							perspective.getOrigin(),
							perspective.getDestination(),
							perspective.getPlanArrivalTime(),
							perspective.getDecisionMaker(),
							perspective.getAlternatives());

				default:
					throw new IllegalArgumentException("unhandled trip type");
			}
		}
		catch (UnexistingAttributeException e) {
			throw new IllegalArgumentException("trip request does not have required attributes", e);
		}
	}
}

