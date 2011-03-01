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
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.scoring.CharyparNagelScoringParameters;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionAccumulator;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.charyparNagel.AgentStuckScoringFunction;
import org.matsim.core.scoring.charyparNagel.LegScoringFunction;
import org.matsim.core.scoring.charyparNagel.MoneyScoringFunction;

/**
 * Scoring function accumulator using {@link BvgLegScoringFunction} instead of {@link LegScoringFunction}
 *
 * @author aneumann
 *
 */
public class BvgScoringFunctionFactory implements ScoringFunctionFactory {

	private static final Logger log = Logger.getLogger(BvgScoringFunctionFactory.class);

	private final CharyparNagelScoringParameters charyparNagelConfigParameters;
	private final BvgScoringFunctionParameters bvgParameters;
	private final NetworkImpl network;

	public BvgScoringFunctionFactory(final PlanCalcScoreConfigGroup charyparNagelConfig, final BvgScoringFunctionConfigGroup bvgConfig, NetworkImpl network){
		this.charyparNagelConfigParameters = new CharyparNagelScoringParameters(charyparNagelConfig);
		this.bvgParameters = new BvgScoringFunctionParameters(bvgConfig);
		this.network = network;
		log.info("...constructed.");
	}

	@Override
	public ScoringFunction createNewScoringFunction(Plan plan) {
		ScoringFunctionAccumulator scoringFunctionAccumulator = new ScoringFunctionAccumulator();
		scoringFunctionAccumulator.addScoringFunction(new BvgActivityScoringFunction(plan, this.charyparNagelConfigParameters));
		scoringFunctionAccumulator.addScoringFunction(new BvgLegScoringFunction(plan, this.charyparNagelConfigParameters, this.bvgParameters, this.network));
		scoringFunctionAccumulator.addScoringFunction(new MoneyScoringFunction(this.charyparNagelConfigParameters));
		scoringFunctionAccumulator.addScoringFunction(new AgentStuckScoringFunction(this.charyparNagelConfigParameters));
		return scoringFunctionAccumulator;
	}

}
