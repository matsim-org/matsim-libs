/* *********************************************************************** *
 * project: org.matsim.*
 * DgFlightScoringFunctionFactory
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
package air.archive;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionAccumulator;
import org.matsim.core.scoring.functions.CharyparNagelAgentStuckScoring;
import org.matsim.core.scoring.functions.CharyparNagelLegScoring;
import org.matsim.core.scoring.functions.CharyparNagelMoneyScoring;
import org.matsim.core.scoring.functions.CharyparNagelScoringFunctionFactory;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;


/**
 * @author dgrether
 *
 */
public class DgFlightScoringFunctionFactory extends CharyparNagelScoringFunctionFactory {
	
	private static final Logger log = Logger.getLogger(DgFlightScoringFunctionFactory.class);
	
	private CharyparNagelScoringParameters params;
	private PlanCalcScoreConfigGroup plansCalcScoreConfigGroup;

	public DgFlightScoringFunctionFactory(PlanCalcScoreConfigGroup planCalcScore, Network network) {
		super(planCalcScore, network);
		this.params = new CharyparNagelScoringParameters(planCalcScore);
		this.plansCalcScoreConfigGroup = planCalcScore;
		log.info("Using " + this.getClass().getSimpleName() + " as scoring function factory...");
	}

	@Override
	public ScoringFunction createNewScoringFunction(Person person) {
		ScoringFunctionAccumulator scoringFunctionAccumulator = new ScoringFunctionAccumulator();
		scoringFunctionAccumulator.addScoringFunction(new DgFlightCharyparNagelActivityScoringFunction(params, this.plansCalcScoreConfigGroup.getUtilityOfLineSwitch()));
		scoringFunctionAccumulator.addScoringFunction(new CharyparNagelLegScoring(params, network));
		scoringFunctionAccumulator.addScoringFunction(new CharyparNagelMoneyScoring(params));
		scoringFunctionAccumulator.addScoringFunction(new CharyparNagelAgentStuckScoring(params));
		return scoringFunctionAccumulator;
	}

}
