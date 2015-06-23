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
import org.matsim.contrib.locationchoice.bestresponse.DestinationChoiceBestResponseContext;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.functions.CharyparNagelActivityScoring;
import org.matsim.core.scoring.functions.CharyparNagelAgentStuckScoring;
import org.matsim.core.scoring.functions.CharyparNagelLegScoring;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;

public class DCScoringFunctionFactory extends org.matsim.core.scoring.functions.CharyparNagelScoringFunctionFactory {
	private DestinationChoiceBestResponseContext lcContext;
	private Scenario scenario;
	private final static Logger log = Logger.getLogger(DCScoringFunctionFactory.class);

    public DCScoringFunctionFactory(Scenario scenario, DestinationChoiceBestResponseContext lcContext) {
		super(scenario.getConfig().planCalcScore(), scenario.getNetwork());
		this.scenario = scenario;
		this.lcContext = lcContext;
		log.info("creating DCScoringFunctionFactory");
	}
	
	private boolean usingConfigParamsForScoring = true ;
	public void setUsingConfigParamsForScoring( boolean val ) {
		usingConfigParamsForScoring = val ;
	}

	@Override
	public ScoringFunction createNewScoringFunction(Person person) {		
		SumScoringFunction scoringFunctionAccumulator = new SumScoringFunction();
		
		SumScoringFunction.BasicScoring scoringFunction ;
		if ( usingConfigParamsForScoring ) {
			scoringFunction = new DCActivityWOFacilitiesScoringFunction( person, this.lcContext);
			scoringFunctionAccumulator.addScoringFunction(new CharyparNagelActivityScoring( this.lcContext.getParams() ) ) ;
			// forgetting the previous line (which we did at some point) is not picked up by any test within the locationchoice contrib. kai, oct'14
		} else {
			scoringFunction = new DCActivityScoringFunction(person.getSelectedPlan(), this.lcContext);
		}
		scoringFunctionAccumulator.addScoringFunction(scoringFunction);
		scoringFunctionAccumulator.addScoringFunction(new CharyparNagelLegScoring(CharyparNagelScoringParameters.getBuilder(scenario.getConfig().planCalcScore()).create(), scenario.getNetwork()));
		scoringFunctionAccumulator.addScoringFunction(new CharyparNagelAgentStuckScoring(CharyparNagelScoringParameters.getBuilder(scenario.getConfig().planCalcScore()).create()));
		return scoringFunctionAccumulator;
	}
}
