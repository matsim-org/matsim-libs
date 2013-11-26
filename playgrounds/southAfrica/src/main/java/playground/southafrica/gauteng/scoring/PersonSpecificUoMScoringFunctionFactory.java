/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package playground.southafrica.gauteng.scoring;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionAccumulator;
import org.matsim.core.scoring.ScoringFunctionAccumulator.BasicScoring;
import org.matsim.core.scoring.ScoringFunctionAccumulator.MoneyScoring;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;

import playground.southafrica.gauteng.utilityofmoney.UtilityOfMoneyI;

/**
 * @author nagel after
 * @author kickhoefer after
 * @author dgrether
 */
public class PersonSpecificUoMScoringFunctionFactory implements ScoringFunctionFactory {

	private Config config;
	private PlanCalcScoreConfigGroup configGroup;
	private CharyparNagelScoringParameters params;
	private final Network network;
	private final UtilityOfMoneyI utlOfMon ;

	public PersonSpecificUoMScoringFunctionFactory(Config config, Network network, UtilityOfMoneyI utlOfMon) {
		this.config = config;
		this.configGroup = config.planCalcScore();
		this.params = new CharyparNagelScoringParameters(configGroup);
		this.network = network;
		this.utlOfMon = utlOfMon ;
	}

	@Override
	public ScoringFunction createNewScoringFunction(Plan plan) {
		// Design comment: This is the only place where the person is available (via plan.getPerson()).  Thus, all 
		// person-specific scoring actions need to be injected from here. kai, mar'12

		ScoringFunctionAccumulator scoringFunctionAccumulator = new ScoringFunctionAccumulator();

		scoringFunctionAccumulator.addScoringFunction(new org.matsim.core.scoring.functions.CharyparNagelActivityScoring(params));
		scoringFunctionAccumulator.addScoringFunction(new org.matsim.core.scoring.functions.CharyparNagelLegScoring(params, network));
		scoringFunctionAccumulator.addScoringFunction(new org.matsim.core.scoring.functions.CharyparNagelAgentStuckScoring(params));

		// person-dependent money scoring function (standard implementation contains person-indep scoring function):
		double utilityOfMoney_normally_positive = this.utlOfMon.getUtilityOfMoney_normally_positive(plan.getPerson().getId());
		scoringFunctionAccumulator.addScoringFunction( new MoneyScoringImpl(utilityOfMoney_normally_positive) ) ;
		
		return scoringFunctionAccumulator;
	}

}

class MoneyScoringImpl implements MoneyScoring, BasicScoring {
	final static private Logger log = Logger.getLogger(MoneyScoringImpl.class);

	private double score = 0.0;
	private double utilityOfMoney_normally_positive ;
	
	MoneyScoringImpl( double utilityOfMoney_normally_positive ) {
		this.utilityOfMoney_normally_positive = utilityOfMoney_normally_positive ;
	}
	
	@Override
	public void reset() {
		
	}

	private static int cnt = 0 ;
	@Override
	public void addMoney(final double amount_usually_negative) {
		
		this.score += this.utilityOfMoney_normally_positive * amount_usually_negative;
		// positive * negative = negative contribution to score, which is correct.
		
		if ( cnt < 10 ) {
			cnt++ ;
			log.info("toll paid: " + amount_usually_negative + "; resulting accumulated toll utility: " + this.score );
			if (cnt==10 ) {
				log.info(Gbl.FUTURE_SUPPRESSED) ;
			}
		}

	}

	@Override
	public void finish() {

	}
	
	@Override
	public double getScore() {
		return this.score;
	}

}

