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

package org.matsim.core.scoring.functions;

import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.SumScoringFunction;

/**
 * This is a re-implementation of the original CharyparNagel function, based on a
 * modular approach.
 * @see <a href="http://www.matsim.org/node/263">http://www.matsim.org/node/263</a>
 * @author rashid_waraich
 */
public final class CharyparNagelMoneyScoring implements SumScoringFunction.MoneyScoring {

	private double price;

	private final double marginalUtilityOfMoney;

	public CharyparNagelMoneyScoring(final ScoringParameters params) {
		this.marginalUtilityOfMoney = params.marginalUtilityOfMoney;
	}

	public CharyparNagelMoneyScoring(final double marginalUtilityOfMoney) {
		this.marginalUtilityOfMoney = marginalUtilityOfMoney;
	}

	@Override
	public void addMoney(final double amount) {
		this.price += amount;
	}

	@Override
	public void finish() {
	}

	@Override
	public double getScore() {
		// linear mapping of money to score
		return this.price * this.marginalUtilityOfMoney;
	}

	@Override
	public void explainScore(StringBuilder out) {
		out.append("money_util=").append(getScore()).append(ScoringFunction.SCORE_DELIMITER);
		out.append("money_price=").append(price);
	}
}
