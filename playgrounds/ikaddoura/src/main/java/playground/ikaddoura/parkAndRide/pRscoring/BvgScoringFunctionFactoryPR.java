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

package playground.ikaddoura.parkAndRide.pRscoring;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionAccumulator;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.functions.CharyparNagelActivityScoring;
import org.matsim.core.scoring.functions.CharyparNagelAgentStuckScoring;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;
import org.matsim.core.scoring.functions.CharyparNagelLegScoring;
import org.matsim.core.scoring.functions.CharyparNagelMoneyScoring;

/**
 * Scoring function accumulator using...
 * {@link BvgLegScoringFunctionPR} instead of {@link CharyparNagelLegScoring} and
 * {@link BvgActivityScoringFunctionPR} instead of {@link CharyparNagelActivityScoring}
 *
 * @author ikaddoura
 *
 */
public class BvgScoringFunctionFactoryPR implements ScoringFunctionFactory {

	private final CharyparNagelScoringParameters charyparNagelConfigParameters;
	private final BvgScoringFunctionParametersPR bvgParameters;
	private final Double utilityOfLineSwitch;
	private final Network network;

	public BvgScoringFunctionFactoryPR(final PlanCalcScoreConfigGroup charyparNagelConfig, final BvgScoringFunctionConfigGroupPR bvgConfig, Network network) {
		this.charyparNagelConfigParameters = new CharyparNagelScoringParameters(charyparNagelConfig);
		this.bvgParameters = new BvgScoringFunctionParametersPR(bvgConfig);
		this.utilityOfLineSwitch = charyparNagelConfig.getUtilityOfLineSwitch();
		this.network = network;
	}

	@Override
	public ScoringFunction createNewScoringFunction(Person person) {
		ScoringFunctionAccumulator scoringFunctionAccumulator = new ScoringFunctionAccumulator();
		scoringFunctionAccumulator.addScoringFunction(new BvgActivityScoringFunctionPR(person.getSelectedPlan(), this.charyparNagelConfigParameters, this.bvgParameters));
		scoringFunctionAccumulator.addScoringFunction(new BvgLegScoringFunctionPR(person.getSelectedPlan(), this.charyparNagelConfigParameters, this.bvgParameters, this.utilityOfLineSwitch, this.network));
		scoringFunctionAccumulator.addScoringFunction(new CharyparNagelMoneyScoring(this.charyparNagelConfigParameters));
		scoringFunctionAccumulator.addScoringFunction(new CharyparNagelAgentStuckScoring(this.charyparNagelConfigParameters));
		return scoringFunctionAccumulator;
	}

}
