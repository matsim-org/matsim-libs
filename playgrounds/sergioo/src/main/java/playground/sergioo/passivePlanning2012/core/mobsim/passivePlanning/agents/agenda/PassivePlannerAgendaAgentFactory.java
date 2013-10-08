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

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;

import playground.sergioo.passivePlanning2012.api.population.BasePerson;
import playground.sergioo.passivePlanning2012.core.mobsim.passivePlanning.agents.PassivePlannerDriverAgent;
import playground.sergioo.passivePlanning2012.core.population.agenda.Agenda;
import playground.sergioo.passivePlanning2012.population.parallelPassivePlanning.PassivePlannerManager;

public final class PassivePlannerAgendaAgentFactory implements AgentFactory {

	//Attributes
	private final Netsim simulation;
	private final PassivePlannerManager passivePlannerManager;
	private Set<String> modes;
	private Map<Id, Agenda> agendas;

	//Constructors
	public PassivePlannerAgendaAgentFactory(final Netsim simulation) {
		this(simulation, null);
	}
	public PassivePlannerAgendaAgentFactory(final Netsim simulation, final PassivePlannerManager passivePlannerManager) {
		this.simulation = simulation;
		this.passivePlannerManager = passivePlannerManager;
		modes = new HashSet<String>();
		modes.addAll(simulation.getScenario().getConfig().plansCalcRoute().getNetworkModes());
		modes.addAll(simulation.getScenario().getConfig().plansCalcRoute().getTeleportedModeFreespeedFactors().keySet());
		modes.addAll(simulation.getScenario().getConfig().plansCalcRoute().getTeleportedModeSpeeds().keySet());
		modes.remove("empty");
		try {
			BufferedReader reader = new BufferedReader(new FileReader("./data/agendas"));
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	//Methods
	@Override
	public MobsimDriverAgent createMobsimAgentFromPerson(final Person person) {
		PassivePlannerDriverAgent agent = new PassivePlannerAgendaAgent((BasePerson)person, simulation, passivePlannerManager, modes, agendas.get(person.getId())); 
		return agent;
	}

}
