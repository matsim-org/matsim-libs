/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package org.matsim.contrib.locationchoice.frozenepsilons;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.functions.CharyparNagelActivityScoring;
import org.matsim.core.scoring.functions.CharyparNagelAgentStuckScoring;
import org.matsim.core.scoring.functions.CharyparNagelLegScoring;
import org.matsim.core.scoring.functions.ScoringParameters;

class DCScoringFunctionFactory implements ScoringFunctionFactory {
	
	private final static Logger log = Logger.getLogger(DCScoringFunctionFactory.class);
	
	private final Scenario scenario;
	private final DestinationChoiceContext lcContext;
	
	private boolean usingConfigParamsForScoring = true;
	private boolean usingIndividualScoringParameters = true;
	private ScoringParameters nonPersonalizedScoringParameters = null;

    public DCScoringFunctionFactory(Scenario scenario, DestinationChoiceContext lcContext) {
		this.scenario = scenario;
		this.lcContext = lcContext;
		log.info("creating DCScoringFunctionFactory");
		
		// configure ScoringFunction according to config
		DestinationChoiceConfigGroup dccg = (DestinationChoiceConfigGroup) scenario.getConfig().getModule(DestinationChoiceConfigGroup.GROUP_NAME);
		if (dccg != null) {
			this.setUsingConfigParamsForScoring(dccg.getUseConfigParamsForScoring());
			this.setUsingIndividualScoringParameters(dccg.getUseIndividualScoringParameters());
		} else log.warn("No DestinationChoiceConfigGroup was found in the config - cannot configure DCScoringFunctionFactory according to it!");
	}
	
	public void setUsingConfigParamsForScoring(boolean val) {
		this.usingConfigParamsForScoring = val;
	}

	public void setUsingIndividualScoringParameters(boolean val) {
		this.usingIndividualScoringParameters = val;
		if (!this.usingIndividualScoringParameters) {
			Config config = this.scenario.getConfig();
			String subPopulationAttributeName = null;
			this.nonPersonalizedScoringParameters = new ScoringParameters.Builder(config.planCalcScore(), config.planCalcScore().getScoringParameters(subPopulationAttributeName), config.scenario()).build();
		}
	}
	
	@Override
	public ScoringFunction createNewScoringFunction(Person person) {
		
		SumScoringFunction scoringFunctionAccumulator = new SumScoringFunction();
		
		SumScoringFunction.BasicScoring scoringFunction;
		if (this.usingConfigParamsForScoring) {
			scoringFunction = new DCActivityWOFacilitiesScoringFunction(person, this.lcContext);
			scoringFunctionAccumulator.addScoringFunction(new CharyparNagelActivityScoring(this.lcContext.getParams()));
			// forgetting the previous line (which we did at some point) is not picked up by any test within the locationchoice contrib. kai, oct'14
		} else {
			scoringFunction = new DCActivityScoringFunction(person.getSelectedPlan(), this.lcContext);
		}
		scoringFunctionAccumulator.addScoringFunction(scoringFunction);
		
		if (this.usingIndividualScoringParameters) {
			ScoringParameters scoringParameters = new ScoringParameters.Builder(this.scenario, person.getId()).build();
			scoringFunctionAccumulator.addScoringFunction(new CharyparNagelLegScoring(scoringParameters, this.scenario.getNetwork(), this.scenario.getConfig().transit().getTransitModes()));
			scoringFunctionAccumulator.addScoringFunction(new CharyparNagelAgentStuckScoring(scoringParameters));
		} else {
			scoringFunctionAccumulator.addScoringFunction(new CharyparNagelLegScoring(this.nonPersonalizedScoringParameters, this.scenario.getNetwork(), this.scenario.getConfig().transit().getTransitModes()));
			scoringFunctionAccumulator.addScoringFunction(new CharyparNagelAgentStuckScoring(this.nonPersonalizedScoringParameters));
		}
		
		return scoringFunctionAccumulator;
	}
}
