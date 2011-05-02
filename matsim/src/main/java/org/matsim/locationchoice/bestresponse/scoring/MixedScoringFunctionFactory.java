/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package org.matsim.locationchoice.bestresponse.scoring;

import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionAccumulator;
import org.matsim.core.scoring.charyparNagel.AgentStuckScoringFunction;
import org.matsim.core.scoring.charyparNagel.LegScoringFunction;


public class MixedScoringFunctionFactory extends org.matsim.core.scoring.charyparNagel.CharyparNagelScoringFunctionFactory {
	private final Controler controler;

	public MixedScoringFunctionFactory(PlanCalcScoreConfigGroup config, Controler controler) {
		super(config);				
		this.controler = controler;
	}

	public ScoringFunction createNewScoringFunction(Plan plan) {		
		ScoringFunctionAccumulator scoringFunctionAccumulator = new ScoringFunctionAccumulator();
		
		MixedActivityScoringFunction scoringFunction = new MixedActivityScoringFunction((PlanImpl)plan, super.getParams(), 
				this.controler.getFacilities(), this.controler.getFacilityPenalties(), this.controler.getConfig());
		
		scoringFunctionAccumulator.addScoringFunction(scoringFunction);
		scoringFunctionAccumulator.addScoringFunction(new LegScoringFunction(plan, super.getParams()));
		scoringFunctionAccumulator.addScoringFunction(new AgentStuckScoringFunction(super.getParams()));
		return scoringFunctionAccumulator;
	}
}
