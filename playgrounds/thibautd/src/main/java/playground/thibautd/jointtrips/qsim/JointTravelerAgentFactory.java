/* *********************************************************************** *
 * project: org.matsim.*
 * JointTravelerAgentFactory.java
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
package playground.thibautd.jointtrips.qsim;

import org.matsim.api.core.v01.population.Person;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;

/**
 * @author thibautd
 */
public class JointTravelerAgentFactory implements AgentFactory {
	private final AgentFactory factory;

	public JointTravelerAgentFactory(final AgentFactory wrapped) {
		factory = wrapped;
	}

	@Override
	public MobsimAgent createMobsimAgentFromPerson(final Person p) {
		return new JointTravelerAgent( (MobsimDriverAgent) factory.createMobsimAgentFromPerson( p ) );
	}
}

