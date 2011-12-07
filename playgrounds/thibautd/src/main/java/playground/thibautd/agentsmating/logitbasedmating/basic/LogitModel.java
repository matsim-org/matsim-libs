/* *********************************************************************** *
 * project: org.matsim.*
 * LogitModel.java
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
package playground.thibautd.agentsmating.logitbasedmating.basic;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import playground.thibautd.agentsmating.logitbasedmating.framework.Alternative;
import playground.thibautd.agentsmating.logitbasedmating.framework.ChoiceModel;
import playground.thibautd.agentsmating.logitbasedmating.framework.DecisionMaker;
import playground.thibautd.agentsmating.logitbasedmating.framework.TripRequest;

/**
 * Abstract class to define easily a logit model.
 * @author thibautd
 */
public abstract class LogitModel implements ChoiceModel {
	private final Random random = new Random( 14325 );
	private static final double EPSILON = 1E-7;
	private final double scale;
	private final boolean selfChecking;

	/**
	 * Creates a self-checking logit model with scale parameter 1.
	 */
	public LogitModel() {
		this( 1 , true );
	}

	/**
	 * Creates a logit model with scale parameter 1.
	 *
	 * @param selfCheking if true, the model will check
	 * the validatiy of each utility value
	 */
	public LogitModel( final boolean selfChecking ) {
		this( 1 , selfChecking );
	}

	/**
	 * Creates a self-checking logit model.
	 *
	 * @param scale the scale parameter ("mu")
	 */
	public LogitModel( final double scale ) {
		this( scale , true );
	}

	/**
	 * Creates a logit model.
	 *
	 * @param selfCheking if true, the model will check
	 * the validatiy of each utility value
	 * @param scale the scale parameter ("mu")
	 */
	public LogitModel( final double scale , final boolean selfChecking ) {
		this.scale = scale;
		this.selfChecking = selfChecking;
	}

	@Override
	public Alternative performChoice(
			final DecisionMaker decisionMaker,
			final List<Alternative> alternatives) {
		Map<Alternative, Double> probs =
			getChoiceProbabilities(
				decisionMaker,
				alternatives);

		double choice = random.nextDouble();

		double cumul = EPSILON;

		for (Map.Entry<Alternative, Double> alt : probs.entrySet()) {
			cumul += alt.getValue();
			if (choice <= cumul) {
				//if (alt.getKey() instanceof TripRequest) {
				//	System.out.println( ((TripRequest) alt.getKey()).getTripType() );
				//}
				return alt.getKey();
			}
		}

		throw new RuntimeException( "choice procedure failed!" );
	}

	@Override
	public Map<Alternative, Double> getChoiceProbabilities(
			final DecisionMaker decisionMaker,
			final List<Alternative> alternatives) {
		Map<Alternative, Double> probabilities = new HashMap<Alternative, Double>();

		double cumul = 0;
		for (Alternative alt : alternatives) {
			double utility = getSystematicUtility( decisionMaker , alt );

			if (selfChecking && Double.isNaN( utility )) {
				throw new RuntimeException( "got a NaN utility in "+this.getClass().getSimpleName()
						+" for decision maker "+decisionMaker+" and alternative "+alt );
			}

			double weight = Math.exp( scale * utility );
			cumul += weight;
			probabilities.put( alt , weight );
		}

		for (Map.Entry<Alternative, Double> entry : probabilities.entrySet()) {
			double prob = entry.getValue() / cumul;
			entry.setValue( prob );
		}

		return probabilities;
	}
}

