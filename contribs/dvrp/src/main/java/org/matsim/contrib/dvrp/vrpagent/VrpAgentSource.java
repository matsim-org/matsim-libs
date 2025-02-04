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

package org.matsim.contrib.dvrp.vrpagent;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.Fleet;
import org.matsim.contrib.dvrp.load.DvrpLoadType;
import org.matsim.contrib.dvrp.optimizer.VrpOptimizer;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentLogic.DynActionCreator;
import org.matsim.contrib.dynagent.DynAgent;
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicleImpl;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehiclesFactory;

public class VrpAgentSource implements AgentSource {
	private final DynActionCreator nextActionCreator;
	private final Fleet fleet;
	private final VrpOptimizer optimizer;
	private final String dvrpMode;
	private final QSim qSim;
	private final VehicleType vehicleType;
	private final DvrpLoadType dvrpLoadType;

	public VrpAgentSource(DynActionCreator nextActionCreator, Fleet fleet, VrpOptimizer optimizer, String dvrpMode,
			QSim qSim, VehicleType vehicleType, DvrpLoadType dvrpLoadType) {
		this.nextActionCreator = nextActionCreator;
		this.fleet = fleet;
		this.optimizer = optimizer;
		this.dvrpMode = dvrpMode;
		this.qSim = qSim;
		this.vehicleType = vehicleType;
		this.dvrpLoadType = dvrpLoadType;
	}

	@Override
	public void insertAgentsIntoMobsim() {
		VehiclesFactory vehicleFactory = this.qSim.getScenario().getVehicles().getFactory();

		for (DvrpVehicle dvrpVehicle : fleet.getVehicles().values()) {
			Id<DvrpVehicle> id = dvrpVehicle.getId();
			Id<Link> startLinkId = dvrpVehicle.getStartLink().getId();

			VrpAgentLogic vrpAgentLogic = new VrpAgentLogic(optimizer, nextActionCreator, dvrpVehicle, dvrpMode,
					qSim.getEventsManager(), dvrpLoadType);
			DynAgent vrpAgent = new DynAgent(Id.createPersonId(id), startLinkId, qSim.getEventsManager(),
					vrpAgentLogic);
			Vehicle matsimVehicle = dvrpVehicle.getSpecification()
					.getMatsimVehicle()
					.orElse(vehicleFactory.createVehicle(Id.create(id, Vehicle.class), vehicleType));
			QVehicle mobsimVehicle = new QVehicleImpl(matsimVehicle);
			vrpAgent.setVehicle(mobsimVehicle);
			mobsimVehicle.setDriver(vrpAgent);

			qSim.addParkedVehicle(mobsimVehicle, startLinkId);
			qSim.insertAgentIntoMobsim(vrpAgent);
		}
	}
}
