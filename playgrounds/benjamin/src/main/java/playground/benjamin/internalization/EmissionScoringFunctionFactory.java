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
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.deprecated.scoring.ScoringFunctionAccumulator;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.deprecated.scoring.functions.CharyparNagelActivityScoring;
import org.matsim.deprecated.scoring.functions.CharyparNagelAgentStuckScoring;
import org.matsim.deprecated.scoring.functions.CharyparNagelLegScoring;
import org.matsim.core.scoring.functions.ScoringParameters;

/**
 * @author benjamin
 *
 */
@Deprecated
public class EmissionScoringFunctionFactory implements ScoringFunctionFactory {
	
	MatsimServices controler;
	PlanCalcScoreConfigGroup configGroup;
	ScoringParameters params;
	Network network;
	ScoringFromEmissions scoringFromEmissions;
	
	public EmissionScoringFunctionFactory(MatsimServices controler) {
		this.controler = controler;
		this.configGroup = controler.getConfig().planCalcScore();
		this.params = new ScoringParameters.Builder(configGroup, configGroup.getScoringParameters(null), controler.getConfig().scenario()).build();
		this.network = controler.getScenario().getNetwork();
		this.scoringFromEmissions = new ScoringFromEmissions(params);
	}

	@Override
	public ScoringFunction createNewScoringFunction(Person person) {
		
		PlanCalcScoreConfigGroup configGroup = controler.getConfig().planCalcScore();
		ScoringParameters params = new ScoringParameters.Builder(configGroup, configGroup.getScoringParameters(null), controler.getConfig().scenario()).build();
		Network network = controler.getScenario().getNetwork();
		
		ScoringFunctionAccumulator accumulator = new ScoringFunctionAccumulator();
		
		accumulator.addScoringFunction(new CharyparNagelActivityScoring(params));
		accumulator.addScoringFunction(new CharyparNagelLegScoring(params, network));
		accumulator.addScoringFunction(new CharyparNagelAgentStuckScoring(params));
		
		accumulator.addScoringFunction(this.scoringFromEmissions);

		return accumulator;
	}

	public ScoringFromEmissions getScoringFromEmissions() {
		return scoringFromEmissions;
	}

}
