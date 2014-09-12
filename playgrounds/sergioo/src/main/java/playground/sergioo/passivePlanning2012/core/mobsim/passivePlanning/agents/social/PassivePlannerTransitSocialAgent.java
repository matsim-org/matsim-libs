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

package playground.sergioo.passivePlanning2012.core.mobsim.passivePlanning.agents.social;

import java.util.Collection;
import java.util.Set;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.households.Household;

import playground.sergioo.passivePlanning2012.api.population.BasePerson;
import playground.sergioo.passivePlanning2012.core.mobsim.passivePlanning.agents.PassivePlannerTransitAgent;
import playground.sergioo.passivePlanning2012.core.mobsim.passivePlanning.agents.social.SinglePlannerSocialAgent;
import playground.sergioo.passivePlanning2012.core.scenario.ScenarioSimplerNetwork;
import playground.sergioo.passivePlanning2012.population.parallelPassivePlanning.PassivePlannerManager;

public class PassivePlannerTransitSocialAgent extends PassivePlannerTransitAgent  {

	//Constructors
	public PassivePlannerTransitSocialAgent(final BasePerson basePerson, final Netsim simulation, final PassivePlannerManager passivePlannerManager, final Household household, Set<String> modes) {
		super(basePerson, simulation, passivePlannerManager);
		boolean carAvailability = false;
		Collection<String> mainModes = simulation.getScenario().getConfig().qsim().getMainModes();
		for(PlanElement planElement:basePerson.getBasePlan().getPlanElements())
			if(planElement instanceof Leg)
				if(mainModes.contains(((Leg)planElement).getMode()))
					carAvailability = true;
		planner = new SinglePlannerSocialAgent((ScenarioSimplerNetwork) simulation.getScenario(), carAvailability, household, modes, this);
	}
	
	@Override
	public void endActivityAndComputeNextState(double now) {
		Activity prevAct = (Activity)getCurrentPlanElement();
		((SinglePlannerSocialAgent)planner).shareKnownPlace(prevAct.getFacilityId(), prevAct.getStartTime(), prevAct.getType());
		super.endActivityAndComputeNextState(now);
	}

}
