/* *********************************************************************** *
 * project: org.matsim.*
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

package org.matsim.contrib.dynagent.examples.random;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.dynagent.DynAgent;
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicleImpl;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.VehiclesFactory;

import jakarta.inject.Inject;

public class RandomDynAgentSource implements AgentSource {
	private static final int AGENT_COUNT = 100;

	private final QSim qSim;

	@Inject
	public RandomDynAgentSource(QSim qSim) {
		this.qSim = qSim;
	}

	@Override
	public void insertAgentsIntoMobsim() {
		Scenario scenario = qSim.getScenario();
		Network network = scenario.getNetwork();
		VehiclesFactory qSimVehicleFactory = scenario.getVehicles().getFactory();

		for (int i = 0; i < AGENT_COUNT; i++) {
			RandomDynAgentLogic agentLogic = new RandomDynAgentLogic(network);

			Id<Person> id = Id.createPersonId(i);
			Id<Link> startLinkId = RandomDynAgentLogic.chooseRandomElement(network.getLinks().keySet());
			DynAgent agent = new DynAgent(id, startLinkId, qSim.getEventsManager(), agentLogic);

//			qSim.createAndParkVehicleOnLink(qSimVehicleFactory.createVehicle(Id.create(id, Vehicle.class),
//					VehicleUtils.getDefaultVehicleType()), startLinkId);

			final Vehicle vehicle = qSimVehicleFactory.createVehicle( Id.create( id, Vehicle.class ), VehicleUtils.createDefaultVehicleType() ) ;
			QVehicle qVehicle = new QVehicleImpl( vehicle ) ; // yyyyyy should use factory.  kai, nov'18
			qSim.addParkedVehicle( qVehicle, startLinkId );

			qSim.insertAgentIntoMobsim( agent );
		}
	}
}
