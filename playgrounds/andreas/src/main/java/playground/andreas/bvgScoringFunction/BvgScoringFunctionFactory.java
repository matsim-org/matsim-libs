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

package playground.andreas.bvgScoringFunction;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.ScenarioConfigGroup;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionAccumulator;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.functions.CharyparNagelAgentStuckScoring;
import org.matsim.core.scoring.functions.CharyparNagelLegScoring;
import org.matsim.core.scoring.functions.CharyparNagelMoneyScoring;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;

/**
 * Scoring function accumulator using {@link BvgLegScoringFunction} instead of {@link CharyparNagelLegScoring}
 *
 * @author aneumann
 *
 */
public class BvgScoringFunctionFactory implements ScoringFunctionFactory {

	private static final Logger log = Logger.getLogger(BvgScoringFunctionFactory.class);

	private final CharyparNagelScoringParameters charyparNagelConfigParameters;
	private final BvgScoringFunctionParameters bvgParameters;
	private final Double utilityOfLineSwitch;
	private final Network network;

	public BvgScoringFunctionFactory(final PlanCalcScoreConfigGroup charyparNagelConfig, final ScenarioConfigGroup scenarioConfig, final BvgScoringFunctionConfigGroup bvgConfig, Network network) {
		this.charyparNagelConfigParameters = CharyparNagelScoringParameters.getBuilder(charyparNagelConfig, scenarioConfig).create();
		this.bvgParameters = new BvgScoringFunctionParameters(bvgConfig);
		this.utilityOfLineSwitch = charyparNagelConfig.getUtilityOfLineSwitch();
		this.network = network;
		log.info("...constructed.");
	}

	@Override
	public ScoringFunction createNewScoringFunction(Person person) {
		ScoringFunctionAccumulator scoringFunctionAccumulator = new ScoringFunctionAccumulator();
		scoringFunctionAccumulator.addScoringFunction(new BvgActivityScoringFunction(person.getSelectedPlan(), this.charyparNagelConfigParameters));
		scoringFunctionAccumulator.addScoringFunction(new BvgLegScoringFunction(person.getSelectedPlan(), this.charyparNagelConfigParameters, this.bvgParameters, this.utilityOfLineSwitch, this.network));
		scoringFunctionAccumulator.addScoringFunction(new CharyparNagelMoneyScoring(this.charyparNagelConfigParameters));
		scoringFunctionAccumulator.addScoringFunction(new CharyparNagelAgentStuckScoring(this.charyparNagelConfigParameters));
		return scoringFunctionAccumulator;
	}

}
