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

import java.math.BigDecimal;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.log4j.Logger;

import playground.thibautd.agentsmating.logitbasedmating.framework.Alternative;
import playground.thibautd.agentsmating.logitbasedmating.framework.ChoiceModel;
import playground.thibautd.agentsmating.logitbasedmating.framework.DecisionMaker;

/**
 * Abstract class to define easily a logit model.
 * @author thibautd
 */
public abstract class LogitModel implements ChoiceModel {
	private static final Logger log =
		Logger.getLogger(LogitModel.class);

	private final Random random = new Random( 14325 );
	private static final double EPSILON = 1E-7;
	private static final double overflowEscapeStep = 100;

	private final double scale;
	private final boolean selfChecking;

	/**
	 * Creates a self-checking logit model with scale parameter 1.
	 */
	public LogitModel() {
		this( 1 , true );
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
	 * Creates a logit model with scale parameter 1.
	 *
	 * @param selfChecking if true, computed probabilities will
	 * be tested for consistency. Underflow and overflow prevention
	 * (which happen quite easily with exponentials) is alway performed.
	 */
	public LogitModel( final boolean selfChecking ) {
		this( 1 , selfChecking );
	}

	/**
	 * Creates a logit model.
	 *
	 * @param scale the scale parameter ("mu")
	 * @param selfChecking if true, computed probabilities will
	 * be tested for consistency. Underflow and overflow prevention
	 * (which happen quite easily with exponentials) is alway performed.
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
				return alt.getKey();
			}
		}

		throw new RuntimeException( "choice procedure failed!" );
	}

	/**
	 * Returns the choice probabilities, shifting all utilities if an over or 
	 * underflow happens.
	 *
	 * @throws RuntimeException in case of floating point arithmetic problem
	 * resulting in a NaN value
	 */
	@Override
	public Map<Alternative, Double> getChoiceProbabilities(
			final DecisionMaker decisionMaker,
			final List<Alternative> alternatives) {
		return getChoiceProbabilities(decisionMaker , alternatives , 0);
	}

	/**
	 * Recursive version of the probabilities computation: if an under- or overflow
	 * happens, the function is called with a shift in the utility.
	 *
	 * @param shift a value by which to shift all utilities. This does not
	 * change the probabilities, and can avoid under and overflows in case
	 * of largely negative or positive utilities (in fact, not that large:
	 * exp( -1000 ) results in an underflow).
	 */
	private Map<Alternative, Double> getChoiceProbabilities(
			final DecisionMaker decisionMaker,
			final List<Alternative> alternatives,
			final double shift) {
		Map<Alternative, Double> probabilities = new HashMap<Alternative, Double>();

		double cumul = 0;
		for (Alternative alt : alternatives) {
			double utility = shift + getSystematicUtility( decisionMaker , alt );

			double weight = Math.exp( scale * utility );

			if (weight == 0.0 || Double.isInfinite( weight ) ) {
				log.warn( "probable double under- or overflow in "+this.getClass().getSimpleName()
						+": got exp( "+scale+" * "+utility+" ) = "+weight
						+" for decision maker "+decisionMaker+" and alternative "+alt );

				double newShift = (weight == 0) ?
					// underflow: shift up
					shift + overflowEscapeStep :
					// overflow: shift down
					shift - overflowEscapeStep;
				log.warn( "utilities shifted by "+newShift);
				// shift the utility so that the term for this alternative is one
				return getChoiceProbabilities( decisionMaker , alternatives , newShift );
			}

			if ( selfChecking && Double.isNaN( weight ) ) {
				throw new RuntimeException( "probable utility mispecification in "+this.getClass().getSimpleName()
						+": got NaN term for exp( "+scale+" * "+utility+" )"
						+" for decision maker "+decisionMaker+" and alternative "+alt );
			}

			cumul += weight;
			probabilities.put( alt , weight );
		}

		if ( selfChecking && Double.isNaN( cumul )) {
			throw new RuntimeException( "got a NaN denominator in "+this.getClass().getSimpleName()
					+" for decision maker "+decisionMaker+" and alternatives "+alternatives );
		}

		double probSum = 0;
		for (Map.Entry<Alternative, Double> entry : probabilities.entrySet()) {
			double prob = entry.getValue() / cumul;

			if (selfChecking) {
				probSum += prob;
				// given the checks before, none of those cases could normally be reached
				if ( Double.isNaN( prob ) ) {
					throw new RuntimeException( "got a NaN probability in "+this.getClass().getSimpleName()
							+" for decision maker "+decisionMaker+" and alternative "+entry.getKey()
							// probably a division by zero. BigDecimal allows to avoid
							// to round the results before printing, and thus check
							// whether this is "truly" zero or if some digits are
							// still non zero.
							// This should not happen dure to underflow prevention
							+": prob = "+new BigDecimal(entry.getValue())+" / "+new BigDecimal(cumul));
				}
				if ( prob < 0 || prob > 1 ) {
					// this one should not happen, even with an ill-writen utility function
					throw new RuntimeException( "got a wrong probability in "+this.getClass().getSimpleName()
							+" for decision maker "+decisionMaker+" and alternative "+entry.getKey()
							+": prob = "+prob);
				}
			}

			entry.setValue( prob );
		}

		if (selfChecking && (Math.abs(probSum - 1) > EPSILON) ) {
			throw new RuntimeException( "got a sum of probabilities different from 1: "+probSum+" in "+this.getClass().getSimpleName()
					+" for decision maker "+decisionMaker+" and alternatives "+alternatives );
		}

		return probabilities;
	}
}

