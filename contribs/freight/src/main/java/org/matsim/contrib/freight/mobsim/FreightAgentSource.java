/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * ${file_name}
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) ${year} by the members listed in the COPYING,        *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 *
 * ${filecomment}
 * ${package_declaration}
 *
 * ${typecomment}
 * ${type_declaration}
 */

package org.matsim.contrib.freight.mobsim;

import java.util.ArrayList;
import java.util.Collection;

import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.vehicles.VehicleUtils;

/**
 * Created by IntelliJ IDEA. User: zilske Date: 10/31/11 Time: 5:59 PM To change
 * this template use File | Settings | File Templates.
 * 
 */
class FreightAgentSource implements AgentSource {

	private Collection<Plan> plans;
	
	private Collection<MobsimAgent> mobSimAgents;

	private AgentFactory agentFactory;

	private QSim qsim;

	FreightAgentSource(Collection<Plan> plans, AgentFactory agentFactory, QSim qsim) {
		this.plans = plans;
		this.agentFactory = agentFactory;
		this.qsim = qsim;
		mobSimAgents = new ArrayList<MobsimAgent>();
	}

	@Override
	public void insertAgentsIntoMobsim() {
		for (Plan plan : plans) {
			MobsimAgent agent = this.agentFactory.createMobsimAgentFromPerson(plan.getPerson());
			qsim.insertAgentIntoMobsim(agent);
			qsim.createAndParkVehicleOnLink(VehicleUtils.getFactory().createVehicle(agent.getId(), VehicleUtils.getDefaultVehicleType()), agent.getCurrentLinkId());
			mobSimAgents.add(agent);
		}
	}

	public Collection<MobsimAgent> getMobSimAgents() {
		return mobSimAgents;
	}
	
	

}
