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

package playground.kai.gauteng.scoring;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.scoring.CharyparNagelScoringParameters;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionAccumulator;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.interfaces.BasicScoring;
import org.matsim.core.scoring.interfaces.MoneyScoring;

import playground.kai.gauteng.utilityofmoney.UtilityOfMoneyI;

/**
 * @author nagel after
 * @author kickhoefer after
 * @author dgrether
 */
public class GautengScoringFunctionFactory implements ScoringFunctionFactory {

	private Config config;
	private PlanCalcScoreConfigGroup configGroup;
	private CharyparNagelScoringParameters params;
	private final Network network;
	private final UtilityOfMoneyI utlOfMon ;

	public GautengScoringFunctionFactory(Config config, Network network, UtilityOfMoneyI utlOfMon) {
		this.config = config;
		this.configGroup = config.planCalcScore();
		this.params = new CharyparNagelScoringParameters(configGroup);
		this.network = network;
		this.utlOfMon = utlOfMon ;
	}

	public ScoringFunction createNewScoringFunction(Plan plan) {
		// Design comment: This is the only place where the person is available (via plan.getPerson()).  Thus, all 
		// person-specific scoring actions need to be injected from here. kai, mar'12

		//summing up all relevant ulitlites
		ScoringFunctionAccumulator scoringFunctionAccumulator = new ScoringFunctionAccumulator();
		
		//utility earned from activities
		scoringFunctionAccumulator.addScoringFunction(new org.matsim.core.scoring.charyparNagel.ActivityScoringFunction(params));

		//utility spend for traveling (in this case: travel time and distance costs)
		scoringFunctionAccumulator.addScoringFunction(new org.matsim.core.scoring.charyparNagel.LegScoringFunction(params, network));
		// yy careful: The standard leg scoring function assumes uniform utilities of time.  If this does not hold, the leg
		// scoring function needs to be replaced.  (But then, presumably, also the activity scoring function
		// needs to be replaced.)  kai, mar'12

		//utility spend for traveling (toll costs) if there is a toll
		double utilityOfMoney_normally_positive = this.utlOfMon.getUtilityOfMoney_normally_positive(plan.getPerson().getId());
		scoringFunctionAccumulator.addScoringFunction( new MoneyScoringImpl(utilityOfMoney_normally_positive) ) ;
		
		//utility spend for being stuck
		scoringFunctionAccumulator.addScoringFunction(new org.matsim.core.scoring.charyparNagel.AgentStuckScoringFunction(params));

		return scoringFunctionAccumulator;

	}

}

/**
 * Notes:<ul>
 * <li> This class is only supposed to work as expected if there are NO OTHER money events than those from road pricing!
 * [[I don't see why that should be so.  Only problem: The logging statement would be wrong. kai, mar'12]]
 * <li> One may be tempted to include this into the standard Charypar Nagel scoring function.  However, it is currently 
 * not possible/not useful to have person-specific values of money in that scoring function.  So it is better to leave it here.
 * kai, mar'12
 * </ul>
 * 
 * @see http://www.matsim.org/node/263
 * @author bkick and michaz after rashid_waraich
 */
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

