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

package playground.anhorni.LEGO.miniscenario.run.scoring;

import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionAccumulator;

import playground.anhorni.LEGO.miniscenario.ConfigReader;


public class MixedScoringFunctionFactory extends org.matsim.core.scoring.charyparNagel.CharyparNagelScoringFunctionFactory {
	private MixedActivityScoringFunction scoringFunction = null;
	private final Controler controler;
	private ConfigReader configReader = new ConfigReader();

	public MixedScoringFunctionFactory(PlanCalcScoreConfigGroup config, Controler controler, ConfigReader configReader) {
		super(config);				
		this.controler = controler;
		this.configReader = configReader;
	}

	public ScoringFunction createNewScoringFunction(Plan plan) {		
		ScoringFunctionAccumulator scoringFunctionAccumulator = new ScoringFunctionAccumulator();
		
		this.scoringFunction = new MixedActivityScoringFunction((PlanImpl)plan, super.getParams(), this.controler.getFacilities(), 
				MatsimRandom.getLocalInstance(), this.configReader, this.controler.getFacilityPenalties(), this.controler.getConfig());
		
		scoringFunctionAccumulator.addScoringFunction(this.scoringFunction);		
		return scoringFunctionAccumulator;
	}
}
