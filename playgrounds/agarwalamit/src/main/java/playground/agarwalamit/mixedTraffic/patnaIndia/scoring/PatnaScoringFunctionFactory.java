/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.agarwalamit.mixedTraffic.patnaIndia.scoring;

import javax.inject.Inject;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ModeParams;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ScoringParameterSet;
import org.matsim.core.config.groups.ScenarioConfigGroup;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.functions.CharyparNagelActivityScoring;
import org.matsim.core.scoring.functions.CharyparNagelAgentStuckScoring;
import org.matsim.core.scoring.functions.CharyparNagelLegScoring;
import org.matsim.core.scoring.functions.CharyparNagelMoneyScoring;
import org.matsim.core.scoring.functions.ScoringParameters;
import org.matsim.core.scoring.functions.ScoringParametersForPerson;
import org.matsim.core.scoring.functions.SubpopulationScoringParameters;

import playground.agarwalamit.mixedTraffic.patnaIndia.utils.PatnaPersonFilter;
import playground.agarwalamit.mixedTraffic.patnaIndia.utils.PatnaUtils;

/**
 * @author amit
 */

public class PatnaScoringFunctionFactory implements ScoringFunctionFactory{
	
	public PatnaScoringFunctionFactory(Scenario sc) {
		parameters = new SubpopulationScoringParameters( sc );
	}
	
	final ScoringParametersForPerson parameters ;
	@Inject Network network;
	@Inject Population population;
	@Inject PlanCalcScoreConfigGroup planCalcScoreConfigGroup; // to modify the util parameters
	@Inject ScenarioConfigGroup scenarioConfig;
	
	@Override
	public ScoringFunction createNewScoringFunction(Person person) {
		final ScoringParameters params = parameters.getScoringParameters( person );

		SumScoringFunction sumScoringFunction = new SumScoringFunction();
		sumScoringFunction.addScoringFunction(new CharyparNagelActivityScoring(params)) ;
		sumScoringFunction.addScoringFunction(new CharyparNagelAgentStuckScoring(params));
		
		CharyparNagelLegScoring legScoringFunction = new CharyparNagelLegScoring(params, network);
		CharyparNagelMoneyScoring moneyScoringFunction = new CharyparNagelMoneyScoring(params);				

		if ( PatnaPersonFilter.isPersonBelongsToUrban(person.getId())) { // inc is not available for commuters and through traffic

			Double monthlyInc = (Double) population.getPersonAttributes().getAttribute(person.getId().toString(), PatnaUtils.INCOME_ATTRIBUTE);
			Double avgInc = PatnaUtils.MEADIAM_INCOME;//PatnaPersonFilter.isPersonBelongsToSlum(person.getId()) ? PatnaUtils.NONSLUM_AVG_INCOME : PatnaUtils.NONSLUM_AVG_INCOME; 

			double ratioOfInc = avgInc/monthlyInc;
			double marginalUtilityOfMoney = ratioOfInc; // marginalUtilMoney = avgIncome / monthlyIncome,
			
			// now modify the util_trav and util_money in mode params.
			planCalcScoreConfigGroup.setMarginalUtilityOfMoney(marginalUtilityOfMoney );
			
			for (String mode : PatnaUtils.URBAN_ALL_MODES ) { // dont need to do it for external modes too. Using average values for them.
				
				// setting this for all modes is better (instead of only for modes in the leg) because this will take care of mode choice.
				ModeParams mp = planCalcScoreConfigGroup.getModes().get(mode);
				
				// cant take the util_trav directly from modeParams, doing so will continue updating the util_trav (for all persons),
				// which eventaully become negative infinity.  Amit July, 16
				double marginalUtil_traveling_fromInitialConfig = Double.NEGATIVE_INFINITY;
				
				if(mode.equals("car")) marginalUtil_traveling_fromInitialConfig = -0.64;
				else if(mode.equals("motorbike")) marginalUtil_traveling_fromInitialConfig = -0.18;
				else if(mode.equals("pt")) marginalUtil_traveling_fromInitialConfig = -0.29;
				else marginalUtil_traveling_fromInitialConfig = -0.0;
				
				mp.setMarginalUtilityOfTraveling( marginalUtil_traveling_fromInitialConfig * ratioOfInc);
			}
			
			ScoringParameterSet scoringParameterSet = planCalcScoreConfigGroup.getScoringParameters( null ); // parameters set is same for all subPopulations 
			
			ScoringParameters.Builder builder = new ScoringParameters.Builder(
					planCalcScoreConfigGroup, scoringParameterSet, scenarioConfig);
			final ScoringParameters modifiedParams = builder.build();
			
			legScoringFunction = new CharyparNagelLegScoring(modifiedParams, network);
			moneyScoringFunction = new CharyparNagelMoneyScoring(modifiedParams); 
		}

		sumScoringFunction.addScoringFunction(moneyScoringFunction);
		sumScoringFunction.addScoringFunction(legScoringFunction);

		return sumScoringFunction;
	}
}