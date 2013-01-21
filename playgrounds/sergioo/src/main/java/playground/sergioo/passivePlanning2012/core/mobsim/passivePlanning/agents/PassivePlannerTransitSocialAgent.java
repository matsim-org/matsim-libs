/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.sergioo.passivePlanning2012.core.mobsim.passivePlanning.agents;

import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.router.IntermodalLeastCostPathCalculator;
import org.matsim.households.Household;

import playground.sergioo.passivePlanning2012.api.population.BasePerson;
import playground.sergioo.passivePlanning2012.core.scenario.ScenarioSimplerNetwork;
import playground.sergioo.passivePlanning2012.population.parallelPassivePlanning.PassivePlannerManager;

public class PassivePlannerTransitSocialAgent extends PassivePlannerTransitAgent  {

	//Constructors
	public PassivePlannerTransitSocialAgent(final BasePerson basePerson, final Netsim simulation, final PassivePlannerManager passivePlannerManager, final Household household, final IntermodalLeastCostPathCalculator leastCostPathCalculator) {
		super(basePerson, simulation, passivePlannerManager);
		planner = new SinglePlannerSocialAgent((ScenarioSimplerNetwork) simulation.getScenario(), household, leastCostPathCalculator, basePerson.getBasePlan());
	}

}
