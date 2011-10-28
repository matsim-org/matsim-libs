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

import org.matsim.api.core.v01.TransportMode;

import playground.thibautd.agentsmating.logitbasedmating.basic.LogitModel;
import playground.thibautd.agentsmating.logitbasedmating.framework.Alternative;
import playground.thibautd.agentsmating.logitbasedmating.framework.ChoiceSetFactory;
import playground.thibautd.agentsmating.logitbasedmating.framework.DecisionMaker;
import playground.thibautd.agentsmating.logitbasedmating.framework.DecisionMakerFactory;
import playground.thibautd.agentsmating.logitbasedmating.framework.TripRequest;
import playground.thibautd.agentsmating.logitbasedmating.framework.UnexistingAttributeException;

/**
 * @author thibautd
 */
public class ReducedSPModel extends LogitModel {

	// model attributes names: alternative
	public static final String A_TRAVEL_TIME = "travelTime";
	public static final String A_COST = "cost";
	public static final String A_WALKING_TIME = "walkingTime";
	public static final String A_PARK_COST = "parkingCost";
	public static final String A_WAITING_TIME = "waitingTime";
	public static final String A_N_TRANSFERS = "nTransfers";

	// model attributes names: decider
	public static final String A_AGE = "age";
	public static final String A_IS_MALE = "isMale";
	public static final String A_SPEAKS_GERMAN = "speaksGerman";
	public static final String A_HAS_PT_ABO = "hasPtAbo";
	public static final String A_IS_CAR_ALWAYS_AVAIL = "carAvailability";

	// model parameters.
	// TODO: less hard-coded?
	public static final double P_ASC_CPD = 0.201;
	public static final double P_ASC_CAR = -0.890;
	public static final double P_ASC_PT = -6.25;
	public static final double P_BETA_ABO_PT = 1.94;
	public static final double P_BETA_AGE_LOG_PT = 1.00;
	public static final double P_BETA_WALK_CAR = -0.0480;
	public static final double P_BETA_WALK_CPD = -0.0285;
	public static final double P_BETA_WALK_CPP = -0.0766;
	public static final double P_BETA_WALK_PT = -0.0227;
	public static final double P_BETA_FEMALE_CP = -0.272;
	public static final double P_BETA_GERMAN_CP = 0.215;
	public static final double P_BETA_PARK_CPD = -0.165;
	public static final double P_BETA_PARK_CAR = -0.0314;
	public static final double P_BETA_COST = -0.0541;
	public static final double P_BETA_TT_CPD = -0.0378;
	public static final double P_BETA_TT_CPP = -0.0399;
	public static final double P_BETA_TT_CAR = -0.0348;
	public static final double P_BETA_TT_PT = -0.00892;
	public static final double P_BETA_TRANSFERS_PT = -0.118;
	public static final double P_BETA_WAIT_PT = -0.0939;
	public static final double P_BETA_CAR_AVAIL = 0.708;
	public static final double P_BETA_MALE_CAR = 0.355;

	// factories
	private final DecisionMakerFactory decisionMakerFactory = new ReducedSPModelDecisionMakerFactory();
	private final ChoiceSetFactory choiceSetFactory = new ReducedSPModelChoiceSetFactory();

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

		throw new RuntimeException("unhandled mode");
	}

	private double carUtility(
			final DecisionMaker decisionMaker,
			final Alternative alternative) throws UnexistingAttributeException {
		return
			P_ASC_CAR +
			P_BETA_TT_CAR * alternative.getAttribute( A_TRAVEL_TIME ) +
			P_BETA_COST * alternative.getAttribute( A_COST ) +
			P_BETA_WALK_CAR * alternative.getAttribute( A_WALKING_TIME ) +
			P_BETA_PARK_CAR * alternative.getAttribute( A_PARK_COST ) +
			P_BETA_MALE_CAR * decisionMaker.getAttribute( A_IS_MALE ) +
			P_BETA_CAR_AVAIL * decisionMaker.getAttribute( A_IS_CAR_ALWAYS_AVAIL );
	}

	private double ptUtility(
			final DecisionMaker decisionMaker,
			final Alternative alternative) throws UnexistingAttributeException {
		return
			P_ASC_PT +
			P_BETA_TT_PT * alternative.getAttribute( A_TRAVEL_TIME ) +
			P_BETA_COST * alternative.getAttribute( A_COST ) +
			P_BETA_WALK_PT * alternative.getAttribute( A_WALKING_TIME ) +
			P_BETA_WAIT_PT * alternative.getAttribute( A_WAITING_TIME ) +
			P_BETA_ABO_PT * decisionMaker.getAttribute( A_HAS_PT_ABO ) +
			P_BETA_AGE_LOG_PT * Math.log( decisionMaker.getAttribute( A_AGE ) ) +
			P_BETA_TRANSFERS_PT * alternative.getAttribute( A_N_TRANSFERS );
	}

	private double cppUtility(
			final DecisionMaker decisionMaker,
			final Alternative alternative) throws UnexistingAttributeException {
		return
			P_BETA_TT_CPP * alternative.getAttribute( A_TRAVEL_TIME ) +
			P_BETA_COST * alternative.getAttribute( A_COST ) +
			P_BETA_WALK_CPP * alternative.getAttribute( A_WALKING_TIME ) +
			P_BETA_FEMALE_CP * (1 - decisionMaker.getAttribute( A_IS_MALE )) +
			P_BETA_GERMAN_CP * decisionMaker.getAttribute( A_SPEAKS_GERMAN );
	}

	private double cpdUtility(
			final DecisionMaker decisionMaker,
			final Alternative alternative) throws UnexistingAttributeException {
		return
			P_ASC_CPD +
			P_BETA_TT_CPD * alternative.getAttribute( A_TRAVEL_TIME ) +
			P_BETA_COST * alternative.getAttribute( A_COST ) +
			P_BETA_WALK_CPD * alternative.getAttribute( A_WALKING_TIME ) +
			P_BETA_PARK_CPD * alternative.getAttribute( A_PARK_COST ) +
			P_BETA_FEMALE_CP * (1 - decisionMaker.getAttribute( A_IS_MALE )) +
			P_BETA_GERMAN_CP * decisionMaker.getAttribute( A_SPEAKS_GERMAN );
	}


	// /////////////////////////////////////////////////////////////////////////
	// perspective changing
	// /////////////////////////////////////////////////////////////////////////
	@Override
	public TripRequest changePerspective(
			final TripRequest tripToConsider,
			final TripRequest perspective) {
		// TODO Auto-generated method stub
		return null;
	}
}

