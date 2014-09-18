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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleUtils;
/**
 * Created by IntelliJ IDEA. User: zilske Date: 10/31/11 Time: 5:59 PM To change
 * this template use File | Settings | File Templates.
 * 
 */
class FreightAgentSource implements AgentSource {

	private static Logger log = Logger.getLogger(FreightAgentSource.class);
	
	private Collection<MobSimVehicleRoute> vehicleRoutes;
	
	private Collection<MobsimAgent> mobSimAgents;

	private AgentFactory agentFactory;

	private QSim qsim;

	FreightAgentSource(Collection<MobSimVehicleRoute> vehicleRoutes, AgentFactory agentFactory, QSim qsim) {
		this.vehicleRoutes = vehicleRoutes;
		this.agentFactory = agentFactory;
		this.qsim = qsim;
		mobSimAgents = new ArrayList<MobsimAgent>();
	}

	@Override
	public void insertAgentsIntoMobsim() {
		for (MobSimVehicleRoute vRoute : vehicleRoutes) {
			MobsimAgent agent = this.agentFactory.createMobsimAgentFromPerson(vRoute.getPlan().getPerson());
			Vehicle vehicle = null;
			if(vRoute.getVehicle() == null){
				vehicle = VehicleUtils.getFactory().createVehicle(Id.create(agent.getId(), Vehicle.class), VehicleUtils.getDefaultVehicleType());
				log.warn("vehicle for agent "+vRoute.getPlan().getPerson().getId() + " is missing. set default vehicle where maxVelocity is solely defined by link.speed.");
			}
			else if(vRoute.getVehicle().getType() == null){
				vehicle = VehicleUtils.getFactory().createVehicle(Id.create(agent.getId(), Vehicle.class), VehicleUtils.getDefaultVehicleType());
				log.warn("vehicleType for agent "+vRoute.getPlan().getPerson().getId() + " is missing. set default vehicleType where maxVelocity is solely defined by link.speed.");
			}
			else vehicle = vRoute.getVehicle();
			qsim.createAndParkVehicleOnLink(vehicle, agent.getCurrentLinkId());
			qsim.insertAgentIntoMobsim(agent);
			mobSimAgents.add(agent);
		}
	}

	public Collection<MobsimAgent> getMobSimAgents() {
		return mobSimAgents;
	}
	
	

}
