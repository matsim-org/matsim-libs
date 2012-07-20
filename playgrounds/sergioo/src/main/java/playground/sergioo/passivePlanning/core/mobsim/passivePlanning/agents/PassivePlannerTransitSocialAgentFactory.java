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

package playground.sergioo.passivePlanning.core.mobsim.passivePlanning.agents;

import org.matsim.api.core.v01.population.Person;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.router.IntermodalLeastCostPathCalculator;
import org.matsim.households.PersonHouseholdMapping;

import playground.sergioo.passivePlanning.api.population.BasePerson;
import playground.sergioo.passivePlanning.population.parallelPassivePlanning.PassivePlannerManager;

public final class PassivePlannerTransitSocialAgentFactory implements AgentFactory {

	//Attributes
	private final Netsim simulation;
	private final PassivePlannerManager passivePlannerManager;
	private final PersonHouseholdMapping personHouseholdMapping;
	private final IntermodalLeastCostPathCalculator leastCostPathCalculator;

	//Constructors
	public PassivePlannerTransitSocialAgentFactory(final Netsim simulation, final PersonHouseholdMapping personHouseholdMapping, final IntermodalLeastCostPathCalculator leastCostPathCalculator) {
		this(simulation, null, personHouseholdMapping, leastCostPathCalculator);
	}
	public PassivePlannerTransitSocialAgentFactory(final Netsim simulation, final PassivePlannerManager passivePlannerManager, final PersonHouseholdMapping personHouseholdMapping, final IntermodalLeastCostPathCalculator leastCostPathCalculator) {
		this.simulation = simulation;
		this.passivePlannerManager = passivePlannerManager;
		this.personHouseholdMapping = personHouseholdMapping;
		this.leastCostPathCalculator = leastCostPathCalculator;
	}

	//Methods
	@Override
	public MobsimDriverAgent createMobsimAgentFromPerson(final Person person) {
		PassivePlannerTransitAgent agent = new PassivePlannerTransitSocialAgent((BasePerson)person, simulation, passivePlannerManager, personHouseholdMapping.getHousehold(person.getId()), leastCostPathCalculator); 
		return agent;
	}

}
