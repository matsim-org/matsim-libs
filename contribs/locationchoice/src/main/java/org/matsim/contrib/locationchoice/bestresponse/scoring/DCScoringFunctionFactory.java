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
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.locationchoice.bestresponse.DestinationChoiceBestResponseContext;
import org.matsim.contrib.locationchoice.facilityload.FacilityPenalties;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionAccumulator;
import org.matsim.core.scoring.functions.CharyparNagelActivityScoring;
import org.matsim.core.scoring.functions.CharyparNagelAgentStuckScoring;
import org.matsim.core.scoring.functions.CharyparNagelLegScoring;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;

public class DCScoringFunctionFactory extends org.matsim.core.scoring.functions.CharyparNagelScoringFunctionFactory {
	private final Controler controler;
	private DestinationChoiceBestResponseContext lcContext;
	private Config config;
	private final static Logger log = Logger.getLogger(DCScoringFunctionFactory.class);

	/*
	 * TODO: remove unused params
	 */
	public DCScoringFunctionFactory(Config config, Controler controler, DestinationChoiceBestResponseContext lcContext) {
		super(config.planCalcScore(), controler.getNetwork());
		this.controler = controler;
		this.lcContext = lcContext;
		this.config = config;
		log.info("creating DCScoringFunctionFactory");
	}
		
	private boolean usingFacilityOpeningTimes = true ;
	public void setUsingFacilityOpeningTimes( boolean val ) {
		usingFacilityOpeningTimes = val ;
	}

	@Override
	public ScoringFunction createNewScoringFunction(Plan plan) {		
		ScoringFunctionAccumulator scoringFunctionAccumulator = new ScoringFunctionAccumulator();
		
		CharyparNagelActivityScoring scoringFunction ;
		if ( usingFacilityOpeningTimes ) {
			scoringFunction = new DCActivityScoringFunction(
					(PlanImpl)plan, 
					this.controler.getScenario().getScenarioElement(FacilityPenalties.class).getFacilityPenalties(), 
					lcContext);
		} else {
			scoringFunction = new DCActivityWOFacilitiesScoringFunction(
					(PlanImpl)plan, 
					this.lcContext);
		}
		scoringFunctionAccumulator.addScoringFunction(scoringFunction);		
		scoringFunctionAccumulator.addScoringFunction(new CharyparNagelLegScoring(new CharyparNagelScoringParameters(config.planCalcScore()), controler.getNetwork()));
		scoringFunctionAccumulator.addScoringFunction(new CharyparNagelAgentStuckScoring(new CharyparNagelScoringParameters(config.planCalcScore())));
		return scoringFunctionAccumulator;
	}
}
