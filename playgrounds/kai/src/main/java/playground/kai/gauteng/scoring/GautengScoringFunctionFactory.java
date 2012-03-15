/* *********************************************************************** *
 * project: org.matsim.*												   *
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

package playground.kai.gauteng.scoring;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.scoring.CharyparNagelScoringParameters;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionAccumulator;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.charyparNagel.ActivityScoringFunction;
import org.matsim.core.scoring.charyparNagel.AgentStuckScoringFunction;
import org.matsim.households.Income;
import org.matsim.households.PersonHouseholdMapping;
import org.matsim.households.Income.IncomePeriod;

/**
 * @author nagel after
 * @author kickhoefer after
 * @author dgrether
 */
public class GautengScoringFunctionFactory implements ScoringFunctionFactory {

	private Config config;
	private PlanCalcScoreConfigGroup configGroup;
	private CharyparNagelScoringParameters params;
	private final Network network;

	public GautengScoringFunctionFactory(Config config, Network network) {
		this.config = config;
		this.configGroup = config.planCalcScore();
		this.params = new CharyparNagelScoringParameters(configGroup);
		this.network = network;
	}

	public ScoringFunction createNewScoringFunction(Plan plan) {
		// Design comment: This is the only place where the person is available (via plan.getPerson()).  Thus, all 
		// person-specific scoring actions need to be injected from here. kai, mar'12

		double householdIncomePerDay = -1. ;
		
		//summing up all relevant ulitlites
		ScoringFunctionAccumulator scoringFunctionAccumulator = new ScoringFunctionAccumulator();
		
		//utility earned from activities
		scoringFunctionAccumulator.addScoringFunction(new ActivityScoringFunction(params));

		//utility spend for traveling (in this case: travel time and distance costs)
		scoringFunctionAccumulator.addScoringFunction(new ScoringFromLeg(params, this.network, householdIncomePerDay, plan.getPerson().getId() ));

		//utility spend for traveling (toll costs) if there is a toll
		if(config.scenario().isUseRoadpricing()){
			scoringFunctionAccumulator.addScoringFunction(new ScoringFromToll(params, householdIncomePerDay));
		}
		
		//utility spend for being stuck
		scoringFunctionAccumulator.addScoringFunction(new AgentStuckScoringFunction(params));

		return scoringFunctionAccumulator;

	}

}
