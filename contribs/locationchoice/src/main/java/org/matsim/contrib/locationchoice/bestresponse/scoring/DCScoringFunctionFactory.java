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

package org.matsim.contrib.locationchoice.bestresponse.scoring;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.locationchoice.DestinationChoiceConfigGroup;
import org.matsim.contrib.locationchoice.bestresponse.DestinationChoiceBestResponseContext;
import org.matsim.core.config.Config;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.functions.CharyparNagelActivityScoring;
import org.matsim.core.scoring.functions.CharyparNagelAgentStuckScoring;
import org.matsim.core.scoring.functions.CharyparNagelLegScoring;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;

public class DCScoringFunctionFactory implements ScoringFunctionFactory {
	
	private final static Logger log = Logger.getLogger(DCScoringFunctionFactory.class);
	
	private final Scenario scenario;
	private final DestinationChoiceBestResponseContext lcContext;
	
	private boolean usingConfigParamsForScoring = true;
	private boolean usingIndividualScoringParameters = true;
	private CharyparNagelScoringParameters nonPersonalizedScoringParameters = null;

    public DCScoringFunctionFactory(Scenario scenario, DestinationChoiceBestResponseContext lcContext) {
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
			this.nonPersonalizedScoringParameters = new CharyparNagelScoringParameters.Builder(config.planCalcScore(), config.planCalcScore().getScoringParameters(subPopulationAttributeName), config.scenario()).build();
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
			CharyparNagelScoringParameters scoringParameters = new CharyparNagelScoringParameters.Builder(this.scenario, person.getId()).build();
			scoringFunctionAccumulator.addScoringFunction(new CharyparNagelLegScoring(scoringParameters, this.scenario.getNetwork()));
			scoringFunctionAccumulator.addScoringFunction(new CharyparNagelAgentStuckScoring(scoringParameters));
		} else {
			scoringFunctionAccumulator.addScoringFunction(new CharyparNagelLegScoring(this.nonPersonalizedScoringParameters, this.scenario.getNetwork()));
			scoringFunctionAccumulator.addScoringFunction(new CharyparNagelAgentStuckScoring(this.nonPersonalizedScoringParameters));
		}
		
		return scoringFunctionAccumulator;
	}
}