/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.sergioo.passivePlanning2012.core.mobsim.passivePlanning.agents.agenda;

import org.matsim.api.core.v01.population.Person;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;

import playground.sergioo.passivePlanning2012.api.population.BasePerson;
import playground.sergioo.passivePlanning2012.core.mobsim.passivePlanning.agents.PassivePlannerTransitAgent;
import playground.sergioo.passivePlanning2012.population.parallelPassivePlanning.PassivePlannerManager;

public final class PassivePlannerTransitAgendaAgentFactory implements AgentFactory {

	//Attributes
	private final Netsim simulation;
	private final PassivePlannerManager passivePlannerManager;
	//Constructors
	public PassivePlannerTransitAgendaAgentFactory(final Netsim simulation) {
		this(simulation, null);
	}
	public PassivePlannerTransitAgendaAgentFactory(final Netsim simulation, final PassivePlannerManager passivePlannerManager) {
		this.simulation = simulation;
		this.passivePlannerManager = passivePlannerManager;
		//TODO
	}

	//Methods
	@Override
	public MobsimDriverAgent createMobsimAgentFromPerson(final Person person) {
		PassivePlannerTransitAgent agent = new PassivePlannerTransitAgendaAgent((BasePerson)person, simulation, passivePlannerManager); 
		return agent;
	}

}
