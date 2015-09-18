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

package playground.vsp.parkAndRide.scoring;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.ScenarioConfigGroup;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.functions.CharyparNagelAgentStuckScoring;
import org.matsim.core.scoring.functions.CharyparNagelLegScoring;
import org.matsim.core.scoring.functions.CharyparNagelMoneyScoring;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;

/**
 * Park-and-ride specific scoring function accumulator which extends the CharyparNagelActivityScoring
 *
 * @author ikaddoura
 *
 */
public class PRScoringFunctionFactory implements ScoringFunctionFactory {
	
	private static final Logger log = Logger.getLogger(PRScoringFunctionFactory.class);
	private final CharyparNagelScoringParameters charyparNagelConfigParameters;
	private final double interModalTransferPenalty;
	private final Network network;

	public PRScoringFunctionFactory(final PlanCalcScoreConfigGroup charyparNagelConfig, final ScenarioConfigGroup scenarioConfig, Network network, double intermodalTransferPenalty) {
		log.info("Extending the ordinary activity scoring function by a park-and-ride specific activity scoring function.");
		this.charyparNagelConfigParameters = CharyparNagelScoringParameters.getBuilder(charyparNagelConfig, scenarioConfig).create();
		this.interModalTransferPenalty = intermodalTransferPenalty;
		log.info("The intermodal transfer penalty for each park-and-ride activity is set to " + this.interModalTransferPenalty);
		this.network = network;
	}

	@Override
	public ScoringFunction createNewScoringFunction(Person person) {
		SumScoringFunction scoringFunctionAccumulator = new SumScoringFunction();
		
		// Park-and-ride specific activity scoring extension
		scoringFunctionAccumulator.addScoringFunction(new PRActivityScoringFunction(this.charyparNagelConfigParameters, this.interModalTransferPenalty));
		
		// standard scoring functions
		scoringFunctionAccumulator.addScoringFunction(new CharyparNagelLegScoring(this.charyparNagelConfigParameters, this.network));
		scoringFunctionAccumulator.addScoringFunction(new CharyparNagelMoneyScoring(this.charyparNagelConfigParameters));
		scoringFunctionAccumulator.addScoringFunction(new CharyparNagelAgentStuckScoring(this.charyparNagelConfigParameters));
		return scoringFunctionAccumulator;
	}

}
