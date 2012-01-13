/* *********************************************************************** *
 * project: org.matsim.*
 * EmissionScoringFunctionFactory.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.benjamin.internalization;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.scoring.CharyparNagelScoringParameters;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionAccumulator;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.charyparNagel.ActivityScoringFunction;
import org.matsim.core.scoring.charyparNagel.AgentStuckScoringFunction;
import org.matsim.core.scoring.charyparNagel.LegScoringFunction;

/**
 * @author benjamin
 *
 */
public class EmissionScoringFunctionFactory implements ScoringFunctionFactory {
	
	Controler controler;
	PlanCalcScoreConfigGroup configGroup;
	CharyparNagelScoringParameters params;
	Network network;
	ScoringFromEmissions scoringFromEmissions;
	
	/**
	 * @return the scoringFromEmissions
	 */
	public ScoringFromEmissions getScoringFromEmissions() {
		return scoringFromEmissions;
	}

	public EmissionScoringFunctionFactory(Controler controler) {
		this.controler = controler;
		this.configGroup = controler.getConfig().planCalcScore();
		this.params = new CharyparNagelScoringParameters(configGroup);
		this.network = controler.getScenario().getNetwork();
		this.scoringFromEmissions = new ScoringFromEmissions(params);
	}

	@Override
	public ScoringFunction createNewScoringFunction(Plan plan) {
		
		PlanCalcScoreConfigGroup configGroup = controler.getConfig().planCalcScore();
		CharyparNagelScoringParameters params = new CharyparNagelScoringParameters(configGroup);
		Network network = controler.getScenario().getNetwork();
		
		ScoringFunctionAccumulator accumulator = new ScoringFunctionAccumulator();
		
		accumulator.addScoringFunction(new ActivityScoringFunction(params));
		accumulator.addScoringFunction(new LegScoringFunction(params, network));
		accumulator.addScoringFunction(new AgentStuckScoringFunction(params));
		
		accumulator.addScoringFunction(this.scoringFromEmissions);

		return accumulator;
	}

}
