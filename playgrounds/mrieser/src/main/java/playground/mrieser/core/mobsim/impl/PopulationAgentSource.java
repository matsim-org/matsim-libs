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

package playground.mrieser.core.mobsim.impl;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;

import playground.mrieser.core.mobsim.api.AgentSource;
import playground.mrieser.core.mobsim.api.PlanAgent;

public class PopulationAgentSource implements AgentSource {

	private final Population population;
	private final double agentWeight;

	public PopulationAgentSource(final Population population, final double agentWeight) {
		this.population = population;
		this.agentWeight = agentWeight;
	}

	@Override
	public List<PlanAgent> getAgents() {
		List<PlanAgent> agents = new ArrayList<PlanAgent>(this.population.getPersons().size());
		for (Person person : this.population.getPersons().values()) {
			Plan plan = person.getSelectedPlan();
			agents.add(new DefaultPlanAgent(plan, this.agentWeight));
		}
		return agents;
	}

}
