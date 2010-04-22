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

package playground.benjamin.income;

import org.matsim.core.scoring.CharyparNagelScoringParameters;
import org.matsim.core.scoring.interfaces.BasicScoring;
import org.matsim.core.scoring.interfaces.MoneyScoring;

/**
 * This is a re-implementation of the original CharyparNagel function, based on a
 * modular approach.
 * @see http://www.matsim.org/node/263
 * @author bkick after rashid_waraich
 */
public class ScoringFromToll implements MoneyScoring, BasicScoring {

	private double score = 0.0;
	
	private double betaIncomeCar;
	
	private double incomePerDay;
	
	
	/** The parameters used for scoring */
	protected final CharyparNagelScoringParameters params;

	public ScoringFromToll(final CharyparNagelScoringParameters params, double householdIncomePerDay) {
		this.params = params;
		this.incomePerDay = householdIncomePerDay;
	}	
	
	public void reset() {
		
	}

	public void addMoney(final double amount) {
		this.score += (betaIncomeCar / incomePerDay) * amount; // linear mapping of money to score
	}

	public void finish() {

	}
	
	public double getScore() {
		return this.score;
	}

}
