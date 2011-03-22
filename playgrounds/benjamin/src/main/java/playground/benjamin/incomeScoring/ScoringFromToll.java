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

package playground.benjamin.incomeScoring;

import org.apache.log4j.Logger;
import org.matsim.core.scoring.CharyparNagelScoringParameters;
import org.matsim.core.scoring.interfaces.BasicScoring;
import org.matsim.core.scoring.interfaces.MoneyScoring;

import playground.benjamin.incomeScoring.ScoringFromToll;

/**
 * This is a re-implementation of the original CharyparNagel function, based on a
 * modular approach.
 * 
 * ATTENTION:
 * This class is only supposed to work as expected if there are NO OTHER money events than those from road pricing!
 * 
 * @see http://www.matsim.org/node/263
 * @author bkick and michaz after rashid_waraich
 */
public class ScoringFromToll implements MoneyScoring, BasicScoring {
	
	final static private Logger log = Logger.getLogger(ScoringFromToll.class);

	private double score = 0.0;
	
	/*	in order to convert utility units into money terms, this parameter has to be equal
	to the one in ScoringFromToll, ScoringFromDailyIncome and other money related parts of the scoring function.
	"Car" in the parameter name is not relevant for the same reason.*/
	private double betaIncomeCar = 4.58;
	
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
		
		//Attention: negative income could cause utility gains due to toll!
		this.score += (betaIncomeCar / incomePerDay) * amount;
		
//		log.info("toll paid: " + amount + " CHF; resulting utility change: " + this.score );
	}

	public void finish() {

	}
	
	public double getScore() {
		return this.score;
	}

}
