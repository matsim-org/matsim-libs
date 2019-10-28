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
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.functions.CharyparNagelAgentStuckScoring;
import org.matsim.core.scoring.functions.CharyparNagelLegScoring;
import org.matsim.core.scoring.functions.CharyparNagelMoneyScoring;
import org.matsim.core.scoring.functions.ScoringParametersForPerson;
import org.matsim.core.scoring.functions.SubpopulationScoringParameters;
import org.matsim.core.scoring.functions.ScoringParameters;

/**
 * Park-and-ride specific scoring function accumulator which extends the CharyparNagelActivityScoring
 *
 * @author ikaddoura
 *
 */
public class PRScoringFunctionFactory implements ScoringFunctionFactory {
	
	private static final Logger log = Logger.getLogger(PRScoringFunctionFactory.class);
	private final ScoringParametersForPerson charyparNagelConfigParameters;
	private final double interModalTransferPenalty;
	private final Network network;

	public PRScoringFunctionFactory(final Scenario scenario, double intermodalTransferPenalty) {
		log.info("Extending the ordinary activity scoring function by a park-and-ride specific activity scoring function.");
		this.charyparNagelConfigParameters = new SubpopulationScoringParameters( scenario );
		this.interModalTransferPenalty = intermodalTransferPenalty;
		log.info("The intermodal transfer penalty for each park-and-ride activity is set to " + this.interModalTransferPenalty);
		this.network = scenario.getNetwork();
	}

	@Override
	public ScoringFunction createNewScoringFunction(Person person) {
		SumScoringFunction scoringFunctionAccumulator = new SumScoringFunction();

		final ScoringParameters parameters = charyparNagelConfigParameters.getScoringParameters( person );
		
		// Park-and-ride specific activity scoring extension
		scoringFunctionAccumulator.addScoringFunction(new PRActivityScoringFunction( parameters , this.interModalTransferPenalty));
		
		// standard scoring functions
		scoringFunctionAccumulator.addScoringFunction(new CharyparNagelLegScoring( parameters , this.network));
		scoringFunctionAccumulator.addScoringFunction(new CharyparNagelMoneyScoring( parameters ));
		scoringFunctionAccumulator.addScoringFunction(new CharyparNagelAgentStuckScoring( parameters ));
		return scoringFunctionAccumulator;
	}

}
