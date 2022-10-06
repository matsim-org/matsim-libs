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

import java.util.ArrayList;
import java.util.Collection;

import com.google.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.freight.utils.FreightUtils;
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicleImpl;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleUtils;
/**
 * Created by IntelliJ IDEA. User: zilske Date: 10/31/11 Time: 5:59 PM To change
 * this template use File | Settings | File Templates.
 * 
 */
 public final class FreightAgentSource implements AgentSource {
	 // made public so that it can be used from LSP.  Still has a package-private constructor, thus ok.  kai, jul'22

	public static final String COMPONENT_NAME=FreightAgentSource.class.getSimpleName();

	private static final  Logger log = LogManager.getLogger(FreightAgentSource.class);
	private final CarrierAgentTracker tracker;

	private final Collection<MobsimAgent> mobSimAgents;

	private final AgentFactory agentFactory;

	private final QSim qsim;

	@Inject FreightAgentSource(CarrierAgentTracker tracker, AgentFactory agentFactory, QSim qsim) {
		this.tracker = tracker;
		this.agentFactory = agentFactory;
		this.qsim = qsim;
		mobSimAgents = new ArrayList<>();
	}

	@Override public void insertAgentsIntoMobsim() {

		for (CarrierAgent carrierAgent : tracker.getCarrierAgents()) {
			for( Plan freightDriverPlan : carrierAgent.createFreightDriverPlans() ){

				MobsimAgent agent = this.agentFactory.createMobsimAgentFromPerson( freightDriverPlan.getPerson() );

				Vehicle vehicle = null;
				if( FreightUtils.getVehicle( freightDriverPlan ) == null ){
					vehicle = VehicleUtils.getFactory().createVehicle( Id.create( agent.getId(), Vehicle.class ), VehicleUtils.getDefaultVehicleType() );
					log.warn( "vehicle for agent " + freightDriverPlan.getPerson().getId() + " is missing. set default vehicle where maxVelocity is solely defined by link.speed." );
				} else if( FreightUtils.getVehicle( freightDriverPlan ).getType() == null ){
					vehicle = VehicleUtils.getFactory().createVehicle( Id.create( agent.getId(), Vehicle.class ), VehicleUtils.getDefaultVehicleType() );
					log.warn( "vehicleType for agent " + freightDriverPlan.getPerson().getId() + " is missing. set default vehicleType where maxVelocity is solely defined by link.speed." );
				} else {
					vehicle = FreightUtils.getVehicle( freightDriverPlan );
				}

				log.warn( "inserting vehicleId=" + vehicle.getId() + " into mobsim." );
				qsim.addParkedVehicle( new QVehicleImpl( vehicle ), agent.getCurrentLinkId() );
				// yyyyyy should rather use QVehicleFactory.  kai, nov'18

				qsim.insertAgentIntoMobsim( agent );

				mobSimAgents.add( agent );
			}
		}
	}

	public Collection<MobsimAgent> getMobSimAgents() {
		return mobSimAgents;
	}
	
	

}
