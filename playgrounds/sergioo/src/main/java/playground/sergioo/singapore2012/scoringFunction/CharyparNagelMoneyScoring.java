/* *********************************************************************** *
 * project: org.matsim.*
 * CharyparNagelOpenTimesScoringFunctionFactory.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.sergioo.singapore2012.scoringFunction;

import org.matsim.core.scoring.SumScoringFunction;

/**
 * This is a re-implementation of the original CharyparNagel function, based on a
 * modular approach.
 * @see <a href="http://www.matsim.org/node/263">http://www.matsim.org/node/263</a>
 * @author rashid_waraich
 */
public class CharyparNagelMoneyScoring implements SumScoringFunction.MoneyScoring {

	protected double score;

	private static final double INITIAL_SCORE = 0.0;

	/** The parameters used for scoring */
	protected final CharyparNagelScoringParameters params;

	public CharyparNagelMoneyScoring(final CharyparNagelScoringParameters params) {
		this.params = params;
		this.score = INITIAL_SCORE;
	}

	@Override
	public void addMoney(final double amount) {
		this.score += amount * this.params.marginalUtilityOfMoney ; // linear mapping of money to score
	}

	@Override
	public void finish() {

	}

	@Override
	public double getScore() {
		return this.score;
	}

}
