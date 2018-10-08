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
import org.matsim.contrib.dvrp.data.Fleet;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.optimizer.VrpOptimizer;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentLogic.DynActionCreator;
import org.matsim.contrib.dynagent.DynAgent;
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.VehiclesFactory;

import com.google.inject.Inject;
import com.google.inject.name.Named;

public class VrpAgentSource implements AgentSource {
	public static final String DVRP_VEHICLE_TYPE = "dvrp_vehicle_type";

	private final DynActionCreator nextActionCreator;
	private final Fleet fleet;
	private final VrpOptimizer optimizer;
	private final QSim qSim;

	@Inject(optional = true)
	private @Named(DVRP_VEHICLE_TYPE)
	VehicleType vehicleType;

	public VrpAgentSource(DynActionCreator nextActionCreator, Fleet fleet, VrpOptimizer optimizer, QSim qSim) {
		this.nextActionCreator = nextActionCreator;
		this.fleet = fleet;
		this.optimizer = optimizer;
		this.qSim = qSim;
	}

	public VrpAgentSource(DynActionCreator nextActionCreator, Fleet fleet, VrpOptimizer optimizer, QSim qSim,
			VehicleType vehicleType) {
		this(nextActionCreator, fleet, optimizer, qSim);
		this.vehicleType = vehicleType;
	}

	@Override
	public void insertAgentsIntoMobsim() {
		VehiclesFactory vehicleFactory = VehicleUtils.getFactory();
		if (vehicleType == null) {
			vehicleType = VehicleUtils.getDefaultVehicleType();
		}

		for (Vehicle vrpVeh : fleet.getVehicles().values()) {
			Id<Vehicle> id = vrpVeh.getId();
			Id<Link> startLinkId = vrpVeh.getStartLink().getId();

			VrpAgentLogic vrpAgentLogic = new VrpAgentLogic(optimizer, nextActionCreator, vrpVeh);
			DynAgent vrpAgent = new DynAgent(Id.createPersonId(id), startLinkId, qSim.getEventsManager(),
					vrpAgentLogic);
			QVehicle mobsimVehicle = new QVehicle(
					vehicleFactory.createVehicle(Id.create(id, org.matsim.vehicles.Vehicle.class), vehicleType));
			vrpAgent.setVehicle(mobsimVehicle);
			mobsimVehicle.setDriver(vrpAgent);

			qSim.addParkedVehicle(mobsimVehicle, startLinkId);
			qSim.insertAgentIntoMobsim(vrpAgent);
		}
	}
}
