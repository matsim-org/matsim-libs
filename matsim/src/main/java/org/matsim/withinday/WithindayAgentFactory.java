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

package org.matsim.withinday;

import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.groups.WithindayConfigGroup;
import org.matsim.ptproject.qsim.AgentFactory;
import org.matsim.ptproject.qsim.QPersonAgent;
import org.matsim.ptproject.qsim.QSim;

/**
 * Factory for withinday replanning agents
 *
 * @author dgrether
 */
public class WithindayAgentFactory extends AgentFactory {


	private final WithindayConfigGroup withindayConfigGroup;
	private final WithindayAgentLogicFactory agentLogicFactory;

	public WithindayAgentFactory(final QSim simulation, final WithindayConfigGroup withindayConfig,
			final WithindayAgentLogicFactory agentLogicFactory) {
		super(simulation);
		this.withindayConfigGroup = withindayConfig;
		this.agentLogicFactory = agentLogicFactory;

	}

	@Override
	public QPersonAgent createPersonAgent(final Person p) {
		WithindayAgent agent = new WithindayAgent(p, this.simulation, this.withindayConfigGroup.getAgentVisibilityRange(), this.agentLogicFactory);
		//set the agent's replanning interval
		agent.setReplanningInterval(this.withindayConfigGroup.getReplanningInterval());
		//set the contentment threshold
		agent.setReplanningThreshold(this.withindayConfigGroup.getContentmentThreshold());
		return agent;
	}

}
