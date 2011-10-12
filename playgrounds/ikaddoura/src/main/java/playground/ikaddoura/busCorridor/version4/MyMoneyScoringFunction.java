/* *********************************************************************** *
 * project: org.matsim.*
 * MyMoneyScoringFunction.java
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

package playground.ikaddoura.busCorridor.version4;

import org.matsim.core.scoring.CharyparNagelScoringParameters;
import org.matsim.core.scoring.interfaces.BasicScoring;
import org.matsim.core.scoring.interfaces.MoneyScoring;

public class MyMoneyScoringFunction implements MoneyScoring, BasicScoring {

	protected double score;

	private static final double INITIAL_SCORE = 0.0;

	/** The parameters used for scoring */
	protected final CharyparNagelScoringParameters params;

	public MyMoneyScoringFunction(final CharyparNagelScoringParameters params) {
		this.params = params;
		this.reset();

	}

	@Override
	public void reset() {
		this.score = INITIAL_SCORE;
	}

	@Override
	public void addMoney(final double amount) {
		this.score += amount * this.params.marginalUtilityOfMoney ; // linear mapping of money to score
//		this.score += amount;
	}

	@Override
	public void finish() {

	}

	@Override
	public double getScore() {
		return this.score;
	}

}
