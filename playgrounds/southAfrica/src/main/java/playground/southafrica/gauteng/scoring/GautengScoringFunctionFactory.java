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
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.functions.CharyparNagelActivityScoring;
import org.matsim.core.scoring.functions.CharyparNagelAgentStuckScoring;
import org.matsim.core.scoring.functions.CharyparNagelLegScoring;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;
import org.matsim.utils.objectattributes.ObjectAttributes;

import playground.southafrica.gauteng.utilityofmoney.UtilityOfMoneyI;

/**
 * @author nagel after
 * @author kickhoefer after
 * @author dgrether
 */
public class GautengScoringFunctionFactory implements ScoringFunctionFactory {

	private final CharyparNagelScoringParameters params;
	private final UtilityOfMoneyI utlOfMon ;
	private final Scenario scenario ;
	private final String subPopulationAttributeName;
	final ObjectAttributes personAttributes ;

	public GautengScoringFunctionFactory(Scenario scenario, UtilityOfMoneyI utlOfMon) {
		this.scenario = scenario ;
		this.params = new CharyparNagelScoringParameters(scenario.getConfig().planCalcScore());
		this.utlOfMon = utlOfMon ;
		this.subPopulationAttributeName = scenario.getConfig().plans().getSubpopulationAttributeName() ;
		this.personAttributes = this.scenario.getPopulation().getPersonAttributes();
	}

	@Override
	public ScoringFunction createNewScoringFunction( Plan plan ) {
		SumScoringFunction sum = new SumScoringFunction() ;
		
		String subPopName = (String) personAttributes.getAttribute(plan.getPerson().getId().toString(), this.subPopulationAttributeName ) ;
		if ( subPopName.equals("commercial") ) {
			// do nothing
			// yy note that this will not be sufficient once time mutation is switched on ... freight agents may prolong activities
			// just to move the legs out of congestion. kai, jan'14
		} else {
			sum.addScoringFunction(new CharyparNagelActivityScoring(params));
		}
		sum.addScoringFunction(new CharyparNagelLegScoring(params,scenario.getNetwork()));
		sum.addScoringFunction(new CharyparNagelAgentStuckScoring(params));

		// person-dependent money scoring function (standard implementation contains person-indep scoring function):
		double utilityOfMoney_normally_positive = this.utlOfMon.getMarginalUtilityOfMoney(plan.getPerson().getId());
		sum.addScoringFunction( new MoneyScoringImpl(utilityOfMoney_normally_positive) ) ;
		
		return sum ;
	}

}

class MoneyScoringImpl implements org.matsim.core.scoring.ScoringFunctionAccumulator.MoneyScoring, org.matsim.core.scoring.SumScoringFunction.MoneyScoring {
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
			log.info("money added: " + amount_usually_negative + "; resulting accumulated money utility: " + this.score );
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

