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

package org.matsim.core.mobsim.qsim.agents;

import jakarta.inject.Inject;
import org.matsim.api.core.v01.Message;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.mobsim.dsim.DistributedMobsimAgent;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.utils.timing.TimeInterpretation;

/**
 * Design decisions:<ul>
 * <li> Class is final since there is an interface.  Please use interface directly. kai, nov'11
 * </ul>
 */
public final class DefaultAgentFactory implements AgentFactory {

	private final Netsim simulation;
	private final TimeInterpretation timeInterpretation;

	@Inject
	public DefaultAgentFactory(final Netsim simulation, TimeInterpretation timeInterpretation) {
		this.simulation = simulation;
		this.timeInterpretation = timeInterpretation;
	}

	@Override
	public MobsimDriverAgent createMobsimAgentFromPerson(final Person p) {

		// ( BasicPlanAgentImpl (inside PersonDriverAgentImpl) makes the plan unmodifiable. )

		return new PersonDriverAgentImpl(p.getSelectedPlan(), this.simulation, this.timeInterpretation);
	}

	@Override
	public DistributedMobsimAgent createMobsimAgentFromMessage(Message message) {
		return new PersonDriverAgentImpl((BasicPlanAgentImpl.BasicPlanAgentMessage) message, this.simulation, this.timeInterpretation);
	}
}
