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
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.functions.CharyparNagelActivityScoring;
import org.matsim.core.scoring.functions.CharyparNagelAgentStuckScoring;
import org.matsim.core.scoring.functions.CharyparNagelLegScoring;
import org.matsim.core.scoring.functions.CharyparNagelScoringFunctionFactory.CharyparNagelScoringParametersForPerson;
import org.matsim.core.scoring.functions.CharyparNagelScoringFunctionFactory.SubpopulationCharyparNagelScoringParameters;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;
import org.matsim.utils.objectattributes.ObjectAttributes;

/**
 * @author nagel after
 * @author kickhoefer after
 * @author dgrether
 */
public class GautengScoringFunctionFactory implements ScoringFunctionFactory {

	private final CharyparNagelScoringParametersForPerson parameters;
	private final UtilityOfMoneyI utlOfMon ;
	private final Scenario scenario ;
	private final String subPopulationAttributeName;
	final ObjectAttributes personAttributes ;

	public GautengScoringFunctionFactory(Scenario scenario, double baseValueOfTime, double valueOfTimeMultiplier) {
		this.scenario = scenario ;
		this.parameters = new SubpopulationCharyparNagelScoringParameters( scenario );
		this.utlOfMon = new GautengUtilityOfMoney( scenario, baseValueOfTime, valueOfTimeMultiplier) ;
		this.subPopulationAttributeName = scenario.getConfig().plans().getSubpopulationAttributeName() ;
		this.personAttributes = this.scenario.getPopulation().getPersonAttributes();
	}

	@Override
	public ScoringFunction createNewScoringFunction( Person person ) {
		SumScoringFunction sum = new SumScoringFunction() ;

		final CharyparNagelScoringParameters params = parameters.getScoringParameters( person );
		
		String subPopName = (String) personAttributes.getAttribute(person.getId().toString(), this.subPopulationAttributeName ) ;
		if ( subPopName != null && subPopName.equals("commercial") ) {
			sum.addScoringFunction( new SumScoringFunction.ActivityScoring() {
				private final double margUtlOfTime_s = scenario.getConfig().planCalcScore().getPerforming_utils_hr()/3600. ;
				private double score = 0. ;
				private double overnightActEndTime = Double.NaN ;
				@Override public void handleActivity(Activity act) { }
				@Override public void handleFirstActivity(Activity act) { 
					overnightActEndTime = act.getEndTime() ;
				}
				@Override public void handleLastActivity(Activity act) { 
					score += margUtlOfTime_s * ( overnightActEndTime + 24.*3600. - act.getStartTime() );
					// (The idea is that we get positive score from getting home earlier.  Could also be the standard log functional
					// form. -- If the benefit goes to the employer, the marginal wage rate is in theory the same as the marginal utility
					// of leisure.  In practice, the employer pays much more so that this does not hold.  But we are using a much larger
					// utility of money instead! )
					// (Overall, the effect is that the mUTTS (marginal utility of travel time savings) is the same for commercial as
					// for others: beta_trav - beta_perf , and so differences are caught in the utility of money.)
					// (Now with sub-populations, might consider alternatives to this approach: utility of money same for everybody, and
					// utility of time as a resource different. ---???  kai, jan'14)
				}
				@Override 
				public void finish() { 
					// if the last activity hasn't happened yet, it is not clear what to do.  Hopefully the simulation end time is beyond this.
				}
				@Override
				public double getScore() {
					if ( Double.isNaN(score) ) {
						throw new RuntimeException ("trying to get score when it is not yet ready") ;
					}
					return score ;
				}
			} ) ;
		} else {
			sum.addScoringFunction(new CharyparNagelActivityScoring(params));
		}
		sum.addScoringFunction(new CharyparNagelLegScoring(params,scenario.getNetwork()));
		sum.addScoringFunction(new CharyparNagelAgentStuckScoring(params));

		// person-dependent money scoring function (standard implementation contains person-indep scoring function):
		double utilityOfMoney_normally_positive = this.utlOfMon.getMarginalUtilityOfMoney(person.getId());
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

