/* *********************************************************************** *
 * project: org.matsim.*
 * KmlNetworkWriter.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
package org.matsim.withinday;

import org.matsim.config.groups.WithindayConfigGroup;
import org.matsim.mobsim.queuesim.AgentFactory;
import org.matsim.mobsim.queuesim.PersonAgent;
import org.matsim.population.Person;


/**
 * Factory for withinday replanning agents
 * @author dgrether
 *
 */
public class WithindayAgentFactory extends AgentFactory {
	
	
	private WithindayConfigGroup withindayConfigGroup;
	private WithindayAgentLogicFactory agentLogicFactory;

	public WithindayAgentFactory(WithindayConfigGroup withindayConfig,
			WithindayAgentLogicFactory agentLogicFactory) {
		this.withindayConfigGroup = withindayConfig;
		this.agentLogicFactory = agentLogicFactory;
		
	}

	@Override
	public PersonAgent createPersonAgent(Person p) {
		WithindayAgent agent = new WithindayAgent(p, this.withindayConfigGroup.getAgentVisibilityRange(), this.agentLogicFactory);
		//set the agent's replanning interval
		agent.setReplanningInterval(this.withindayConfigGroup.getReplanningInterval());
		//set the contentment threshold
		agent.setReplanningThreshold(this.withindayConfigGroup.getContentmentThreshold());
		return agent;
	}
	
	
}
