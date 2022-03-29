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

package org.matsim.contrib.freight.controler;

import com.google.inject.Inject;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicleImpl;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleUtils;

import java.util.Collection;


/**
 * Created by IntelliJ IDEA. User: zilske Date: 10/31/11 Time: 5:59 PM To change
 * this template use File | Settings | File Templates.
 * 
 */
public class LSPAgentSource implements AgentSource {

	public static final String COMPONENT_NAME = LSPAgentSource.class.getSimpleName();
	private static final Logger log = Logger.getLogger( LSPAgentSource.class );
	
	private final Collection<Plan> vehicleRoutes;
	
	private final AgentFactory agentFactory;

	private final QSim qsim;

	@Inject LSPAgentSource( CarrierAgentTracker carrierAgentTracker, AgentFactory agentFactory, QSim qsim ) {
		this.vehicleRoutes = carrierAgentTracker.createPlans();
		this.agentFactory = agentFactory;
		this.qsim = qsim;
	}

	@Override
	public void insertAgentsIntoMobsim() {
		for ( Plan vRoute : vehicleRoutes) {

			MobsimAgent agent = this.agentFactory.createMobsimAgentFromPerson(vRoute.getPerson());
			Gbl.assertNotNull( agent );

			Vehicle vehicle;
			if( FreightControlerUtils.getVehicle( vRoute ) == null){
				vehicle = VehicleUtils.getFactory().createVehicle(Id.create(agent.getId(), Vehicle.class), VehicleUtils.getDefaultVehicleType());
				log.warn("vehicle for agent "+vRoute.getPerson().getId() + " is missing. set default vehicle where maxVelocity is solely defined by link.speed.");
			}
			else if( FreightControlerUtils.getVehicle( vRoute ).getType() == null){
				vehicle = VehicleUtils.getFactory().createVehicle(Id.create(agent.getId(), Vehicle.class), VehicleUtils.getDefaultVehicleType());
				log.warn("vehicleType for agent "+vRoute.getPerson().getId() + " is missing. set default vehicleType where maxVelocity is solely defined by link.speed.");
			}
			else vehicle = FreightControlerUtils.getVehicle( vRoute );
			Gbl.assertNotNull(vehicle);

			QVehicleImpl qVeh = new QVehicleImpl( vehicle );
			Gbl.assertNotNull( qVeh );

			qsim.addParkedVehicle( qVeh, agent.getCurrentLinkId() );
			qsim.insertAgentIntoMobsim(agent);
			log.warn( "just added vehicle with id=" + qVeh.getId() + " and agent with id=" + agent.getId() );
		}
	}

}
