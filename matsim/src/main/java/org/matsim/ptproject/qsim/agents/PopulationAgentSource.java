/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package org.matsim.ptproject.qsim.agents;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.framework.MobsimAgent;

import java.util.ArrayList;
import java.util.List;

public class PopulationAgentSource implements AgentSource {

    private Population population;
    private AgentFactory agentFactory;

    public PopulationAgentSource(Population population, AgentFactory agentFactory) {
        this.population = population;
        this.agentFactory = agentFactory;
    }

    @Override
    public List<MobsimAgent> getAgents() {
        List<MobsimAgent> agents = new ArrayList<MobsimAgent>();
		for (Person p : population.getPersons().values()) {
			MobsimAgent agent = this.agentFactory.createMobsimAgentFromPerson(p);
			agents.add(agent);
		}
        return agents;
    }

}
