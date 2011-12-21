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
 * It includes a basic prevention of over and underflow, which
 * happen quite easily due to the exponential formulation (the
 * minimal value that the scaled utility can take is around -744, the
 * maximal around 711).
 *
 * <br>
 * The under/overflow prevention works in the following way:
 * <ul>
 * <li> the scaled utilities are checked to lay in an interval
 * which does not result in a problem at exponentiation, that is,
 * in <tt>[ Math.log( Double.MIN_VALUE ) + e , Math.log( Double.MAX_VALUE ) - e]</tt>
 * , with <tt>e</tt> a positive double.
 * <li> if some utility values are out of this interval, the utilities
 * are shifted so that the lower utility is on the lower bound:
 * the aim is to have the smallest possible exponential terms, so that the
 * sum does not produce an overflow.
 * <li> if, after doing this, overflow still exist, the values
 * are shifted to that the upper utility is on the upper bound.
 * This will result in (the minimal possible number of)
 * underflows, so some probabilities will be 0.0.
 * <li> if the sum produces an overflow, all terms are divided
 * by the number of terms.
 * </ul>
 *
 * Corrections resulting in allowing undeflows produce log messages if
 * the model is set verbose (false by default).
 * Unexpected under and overflows produce exceptions.
 * Other sources of inconsistencies are checked if the self-checking
 * flag is set to true (default).
 *
 * @author thibautd
 */
public abstract class LogitModel implements ChoiceModel {
	private static final Logger log =
		Logger.getLogger(LogitModel.class);

	private final Random random = new Random( 14325 );
	private static final double EPSILON = 1E-7;

	// Defines the range of possible utility values which
	// do not result in an under or overflow.
	private static final double MIN_UTILITY = Math.log( Double.MIN_VALUE ) + 1;
	private static final double MAX_UTILITY = Math.log( Double.MAX_VALUE ) - 1;

	private final double scale;
	private final boolean selfChecking;
	private final boolean isVerbose;

	// /////////////////////////////////////////////////////////////////////////
	// constructors
	// /////////////////////////////////////////////////////////////////////////
	/**
	 * Creates a non verbose self-checking logit model with scale parameter 1.
	 */
	public LogitModel() {
		this( 1 , true , false );
	}

	/**
	 * Creates a non verbose self-checking logit model.
	 *
	 * @param scale the scale parameter ("mu")
	 */
	public LogitModel( final double scale ) {
		this( scale , true , false );
	}

	/**
	 * Creates a non verbose logit model with scale parameter 1.
	 *
	 * @param selfChecking if true, computed probabilities will
	 * be tested for consistency. Underflow and overflow prevention
	 * (which happen quite easily with exponentials) is alway performed.
	 */
	public LogitModel( final boolean selfChecking ) {
		this( 1 , selfChecking , false );
	}

	/**
	 * Creates a logit model.
	 *
	 * @param scale the scale parameter ("mu")
	 * @param selfChecking if true, computed probabilities will
	 * be tested for consistency. Underflow and overflow prevention
	 * (which happen quite easily with exponentials) is alway performed.
	 * @param isVerbose if true, information about the under/overflow detection
	 * process will be logged.
	 */
	public LogitModel(
			final double scale,
			final boolean selfChecking,
			final boolean isVerbose) {
		log.info( "init of "+this.getClass().getSimpleName()+" with scale parameter "+scale );
		this.scale = scale;
		this.selfChecking = selfChecking;
		this.isVerbose = isVerbose;
		log.info( "over/underflow prevention: (scaled) utilities will be shifted"
				+" to lie in the interval [ "+MIN_UTILITY+" ; "+MAX_UTILITY+" ]" );
	}

	// /////////////////////////////////////////////////////////////////////////
	// Logit methods
	// /////////////////////////////////////////////////////////////////////////
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
	 * It is final, as overriding it would make no sense (it is basically the only
	 * difference between the mere interface and this abstract class).
	 *
	 * @throws RuntimeException in case of floating point arithmetic problem
	 * unsolved by the over/underflow prevention mechanism.
	 */
	@Override
	public final Map<Alternative, Double> getChoiceProbabilities(
			final DecisionMaker decisionMaker,
			final List<Alternative> alternatives) {
		Map<Alternative, Double> probabilities = new HashMap<Alternative, Double>();

		boolean allowUnderFlows = false;
		double cumul = 0;
		double shift = 0;
		double lowerUtility = Double.POSITIVE_INFINITY;
		double upperUtility = Double.NEGATIVE_INFINITY;

		// first, get all scaled utilities
		for (Alternative alt : alternatives) {
			double utility = scale * getSystematicUtility( decisionMaker , alt );

			lowerUtility = Math.min( lowerUtility , utility );
			upperUtility = Math.max( upperUtility , utility );

			probabilities.put( alt , utility );
		}

		// then, check whether the exponential risks to produce over/underflow,
		// and set the shift accordingly.
		boolean low = lowerUtility < MIN_UTILITY;
		boolean high = upperUtility > MAX_UTILITY;
		if ( low || high ) {
			if (isVerbose) {
				if (low) log.info( "detected upcoming underflow" );
				if (high) log.info( "detected upcoming overflow" );
			}

			// put the lowest utility on the lower bound
			shift = MIN_UTILITY - lowerUtility;

			if (upperUtility + shift > MAX_UTILITY) {
				if (isVerbose) {
					log.warn( "could not shift utility to prevent "
							+" over and under flow for decision maker "+decisionMaker
							+" : ["+(lowerUtility + shift)+" ; "
							+(upperUtility + shift)+"] does not fit into "
							+"["+MIN_UTILITY+" ; "+MAX_UTILITY+"]"
							+" (corresponding to a shift of "+shift+")" );
					log.warn( "underflow prevention is aborted." );
				}
				shift = MAX_UTILITY - upperUtility;
				allowUnderFlows = true;
			}

			if (isVerbose) log.info( "shifting all utilities by "+shift );
		}

		// now, pass to the exponentials.
		for ( Map.Entry<Alternative, Double> entry : probabilities.entrySet() ) {
			double utility = shift + entry.getValue();
			double weight = Math.exp( utility );

			if (selfChecking) {
				if ( (!allowUnderFlows && weight == 0.0) || Double.isInfinite( weight ) ) {
					throw new RuntimeException( "[BUG] probable double under- or overflow in "+this.getClass().getSimpleName()
							+": got exp( "+utility+" ) = "+weight
							+". This should not happen due to the prevention mechanism!"
							+" for decision maker "+decisionMaker+" and alternative "+entry.getKey() );
				}

				if ( Double.isNaN( weight ) ) {
					throw new RuntimeException( "probable utility mispecification in "+this.getClass().getSimpleName()
							+": got NaN term for exp( "+utility+" )"
							+" for decision maker "+decisionMaker+" and alternative "+entry.getKey() );
				}
			}

			cumul += weight;
			entry.setValue( weight );
		}

		// As we sum the exponentials, it is still possible to have an overflow
		if ( Double.isInfinite( cumul ) ) {
			if (isVerbose) log.info( "got an overflow in the sum of exponentials. Correcting that." );
			int n = probabilities.size();
			cumul = 0;
			for ( Map.Entry<Alternative, Double> entry : probabilities.entrySet() ) {
				double newValue = entry.getValue() / n;
				cumul += newValue;
				entry.setValue( newValue );
			}
		}

		// Check again
		if ( selfChecking ) {
			if ( Double.isInfinite( cumul ) ) {
				throw new RuntimeException( "[BUG] Overflow in the sum of exponentials: "
					+"this cannot happen if overflows are correctly checked for every exponential!" );
			}
			if (cumul == 0) {
				throw new RuntimeException( "[BUG] Underflow in the sum of exponentials: "
					+"this cannot happen if underflows are correctly checked for every exponential!" );
			}
			if ( Double.isNaN( cumul ) ) {
				throw new RuntimeException( "got a NaN denominator in "+this.getClass().getSimpleName()
						+" for decision maker "+decisionMaker+" and alternatives "+alternatives );
			}
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

