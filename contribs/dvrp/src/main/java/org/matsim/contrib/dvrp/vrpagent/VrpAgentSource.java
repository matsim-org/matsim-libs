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

import jakarta.annotation.Nullable;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Message;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.NetworkPartition;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.Fleet;
import org.matsim.contrib.dvrp.load.DvrpLoadType;
import org.matsim.contrib.dvrp.optimizer.VrpOptimizer;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentLogic.DynActionCreator;
import org.matsim.contrib.dynagent.DynAgent;
import org.matsim.contrib.dynagent.DynAgentMessage;
import org.matsim.core.mobsim.dsim.DistributedAgentSource;
import org.matsim.core.mobsim.dsim.DistributedMobsimAgent;
import org.matsim.core.mobsim.dsim.DistributedMobsimVehicle;
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.qsim.interfaces.InsertableMobsim;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicleImpl;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehiclesFactory;

import java.util.Set;

public class VrpAgentSource implements AgentSource, DistributedAgentSource {
	private final DynActionCreator nextActionCreator;
	private final Fleet fleet;
	private final VrpOptimizer optimizer;
	private final String dvrpMode;
	private final Netsim qSim;
	private final VehicleType vehicleType;
	private final DvrpLoadType dvrpLoadType;

	public VrpAgentSource(DynActionCreator nextActionCreator, Fleet fleet, VrpOptimizer optimizer, String dvrpMode,
			Netsim qSim, VehicleType vehicleType, DvrpLoadType dvrpLoadType) {
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

	@Override
	public void createAgentsAndVehicles(NetworkPartition partition, InsertableMobsim mobsim) {

		VehiclesFactory vehicleFactory = this.qSim.getScenario().getVehicles().getFactory();

		for (DvrpVehicle dvrpVehicle : fleet.getVehicles().values()) {
			Id<DvrpVehicle> id = dvrpVehicle.getId();
			Id<Link> startLinkId = dvrpVehicle.getStartLink().getId();

			if (!partition.containsLink(startLinkId)) {
				continue;
			}

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

			mobsim.addParkedVehicle(mobsimVehicle, startLinkId);
			mobsim.insertAgentIntoMobsim(vrpAgent);
		}

	}


	@Override
	public Set<Class<? extends DistributedMobsimAgent>> getAgentClasses() {
		return Set.of(DynAgent.class);
	}

	@Override
	public DistributedMobsimAgent agentFromMessage(Class<? extends DistributedMobsimAgent> type, Message message) {

		if (!(message instanceof DynAgentMessage m)) {
			throw new IllegalArgumentException("Message must be of type DynAgentMessage");
		}

		DvrpVehicle dvrpVehicle = fleet.getVehicles().get(m.vehicleId());

		// If the vehicle is not found, it belongs to a different provider
		if (dvrpVehicle == null)
			return null;

		VrpAgentLogic vrpAgentLogic = new VrpAgentLogic(optimizer, nextActionCreator, dvrpVehicle, dvrpMode,
			qSim.getEventsManager(), dvrpLoadType);

		return new DynAgent(m, qSim.getEventsManager(), vrpAgentLogic);
	}

	@Nullable
	@Override
	public DistributedMobsimVehicle vehicleFromMessage(Class<? extends DistributedMobsimVehicle> type, Message message) {
		throw new UnsupportedOperationException("Not implemented");
	}
}
