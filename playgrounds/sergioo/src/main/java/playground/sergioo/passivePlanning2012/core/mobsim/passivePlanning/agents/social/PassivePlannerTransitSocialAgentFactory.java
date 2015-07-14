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

package playground.sergioo.passivePlanning2012.core.mobsim.passivePlanning.agents.social;

import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.population.Person;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.households.PersonHouseholdMapping;

import playground.sergioo.passivePlanning2012.api.population.BasePerson;
import playground.sergioo.passivePlanning2012.core.mobsim.passivePlanning.agents.PassivePlannerTransitAgent;
import playground.sergioo.passivePlanning2012.population.parallelPassivePlanning.PassivePlannerManager;

public final class PassivePlannerTransitSocialAgentFactory implements AgentFactory {

	//Attributes
	private final Netsim simulation;
	private final PassivePlannerManager passivePlannerManager;
	private final PersonHouseholdMapping personHouseholdMapping;
	private Set<String> modes;

	//Constructors
	public PassivePlannerTransitSocialAgentFactory(final Netsim simulation, final PersonHouseholdMapping personHouseholdMapping) {
		this(simulation, null, personHouseholdMapping);
	}
	public PassivePlannerTransitSocialAgentFactory(final Netsim simulation, final PassivePlannerManager passivePlannerManager, final PersonHouseholdMapping personHouseholdMapping) {
		this.simulation = simulation;
		this.passivePlannerManager = passivePlannerManager;
		this.personHouseholdMapping = personHouseholdMapping;
		modes = new HashSet<String>();
		modes.addAll(simulation.getScenario().getConfig().plansCalcRoute().getNetworkModes());
		if(simulation.getScenario().getConfig().transit().isUseTransit())
			modes.add("pt");
	}

	//Methods
	@Override
	public MobsimDriverAgent createMobsimAgentFromPerson(final Person person) {
		PassivePlannerTransitAgent agent = new PassivePlannerTransitSocialAgent((BasePerson)person, simulation, passivePlannerManager, personHouseholdMapping.getHousehold(person.getId()), modes); 
		return agent;
	}

}
