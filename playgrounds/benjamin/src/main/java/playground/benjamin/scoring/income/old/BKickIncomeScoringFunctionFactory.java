/* *********************************************************************** *
 * project: org.matsim.*																															*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package playground.benjamin.scoring.income.old;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.ScenarioConfigGroup;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionAccumulator;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.functions.CharyparNagelActivityScoring;
import org.matsim.core.scoring.functions.CharyparNagelAgentStuckScoring;
import org.matsim.core.scoring.functions.CharyparNagelMoneyScoring;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;
import org.matsim.households.PersonHouseholdMapping;


/**
 * @author dgrether
 *
 */
public class BKickIncomeScoringFunctionFactory implements ScoringFunctionFactory {

	private PlanCalcScoreConfigGroup configGroup;
	private CharyparNagelScoringParameters params;
	private PersonHouseholdMapping hhdb;
    private Network network;

    public BKickIncomeScoringFunctionFactory(
            PlanCalcScoreConfigGroup charyparNagelScoring, final ScenarioConfigGroup scenarioConfig, PersonHouseholdMapping hhmapping, Network network) {
		this.configGroup = charyparNagelScoring;
		this.params = CharyparNagelScoringParameters.getBuilder(configGroup, scenarioConfig).create();
		this.hhdb = hhmapping;
        this.network = network;
	}

	@Override
	public ScoringFunction createNewScoringFunction(Person person) {

		ScoringFunctionAccumulator scoringFunctionAccumulator = new ScoringFunctionAccumulator();

		scoringFunctionAccumulator.addScoringFunction(new CharyparNagelActivityScoring(params));

		scoringFunctionAccumulator.addScoringFunction(new BKickLegScoring(person.getSelectedPlan(), params, this.hhdb, network));

		scoringFunctionAccumulator.addScoringFunction(new CharyparNagelMoneyScoring(params));

		scoringFunctionAccumulator.addScoringFunction(new CharyparNagelAgentStuckScoring(params));

		return scoringFunctionAccumulator;
	
	}

}
