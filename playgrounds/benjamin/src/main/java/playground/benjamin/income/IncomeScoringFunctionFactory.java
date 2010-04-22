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
package playground.benjamin.income;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.groups.CharyparNagelScoringConfigGroup;
import org.matsim.core.scoring.CharyparNagelScoringParameters;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionAccumulator;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.charyparNagel.ActivityScoringFunction;
import org.matsim.core.scoring.charyparNagel.AgentStuckScoringFunction;
import org.matsim.core.scoring.charyparNagel.MoneyScoringFunction;
import org.matsim.households.PersonHouseholdMapping;


/**
 * @author dgrether
 *
 */
public class IncomeScoringFunctionFactory implements ScoringFunctionFactory {

	private CharyparNagelScoringConfigGroup configGroup;
	private CharyparNagelScoringParameters params;
	private PersonHouseholdMapping hhdb;
	private final Network network;

	public IncomeScoringFunctionFactory(CharyparNagelScoringConfigGroup charyparNagelScoring, PersonHouseholdMapping hhmapping, Network network) {
		this.configGroup = charyparNagelScoring;
		this.params = new CharyparNagelScoringParameters(configGroup);
		this.hhdb = hhmapping;
		this.network = network;
	}

	public ScoringFunction getNewScoringFunction(Plan plan) {

		//summing up all relevant ulitlites
		ScoringFunctionAccumulator scoringFunctionAccumulator = new ScoringFunctionAccumulator();
		
		//utility earned from daily income
		//income dependent!
		scoringFunctionAccumulator.addScoringFunction(new ScoringFromDailyIncome(params, this.hhdb));

		//utility earned from activities
		scoringFunctionAccumulator.addScoringFunction(new ActivityScoringFunction(plan, params));

		//utility spend for traveling (in this case: travel time and distance costs)
		//income dependent!
		scoringFunctionAccumulator.addScoringFunction(new ScoringFromLeg(plan, params, this.hhdb, this.network));

		//utility spend for traveling (toll costs)
		//income dependent!
		scoringFunctionAccumulator.addScoringFunction(new ScoringFromToll(params, this.hhdb));

		//utility spend for being stuck
		scoringFunctionAccumulator.addScoringFunction(new AgentStuckScoringFunction(params));

		return scoringFunctionAccumulator;

	}

}
