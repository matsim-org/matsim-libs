/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.gregor.external;

import com.google.protobuf.RpcController;
import com.google.protobuf.ServiceException;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.ObservableMobsim;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;
import org.matsim.core.mobsim.qsim.qnetsimengine.External2QAdapterLink;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;
import playground.gregor.proto.ProtoMATSimInterface.*;
import playground.gregor.proto.ProtoMATSimInterface.ExternInterfaceService.BlockingInterface;
import playground.gregor.proto.ProtoMATSimInterface.MATSim2ExternPutAgent.Agent;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

public class ExternalEngine implements MobsimEngine {

	private final RpcController rpcCtr;
	private final BlockingInterface clientService;

	private final CyclicBarrier simStepBarrier;
	private final Map<Id<Person>, QVehicle> vehicles = new HashMap<>();
	private final Map<Id<Link>, External2QAdapterLink> adapters = new HashMap<>();
	private final EventsManager em;
	private final ObservableMobsim sim;

	public ExternalEngine(EventsManager eventsManager, ObservableMobsim sim,RpcController rpcCtr,BlockingInterface clientService, CyclicBarrier simStepBarrier) {
		this.em = eventsManager;
		this.sim = sim;
		this.rpcCtr = rpcCtr;
		this.clientService = clientService;
		this.simStepBarrier = simStepBarrier;
	}


	
	@Override
	public void doSimStep(double time) {
		double to = time + 1;
		ExternDoSimStep doSimStepExternal = ExternDoSimStep.newBuilder()
				.setFromTime(time).setToTime(to).build();
		try {
			ExternDoSimStepReceived resp = this.clientService.reqExternDoSimStep(
					this.rpcCtr, doSimStepExternal);
		} catch (ServiceException e) {
			throw new RuntimeException(e);
		}
		try {
			this.simStepBarrier.await();
		} catch (InterruptedException | BrokenBarrierException e) {
			throw new RuntimeException(e);
		}

	}

	@Override
	public void onPrepareSim() {
		ExternOnPrepareSim prep = ExternOnPrepareSim.newBuilder().build();
		try {
			ExternOnPrepareSimConfirmed resp = this.clientService.reqExternOnPrepareSim(this.rpcCtr, prep);
		} catch (ServiceException e) {
			throw new RuntimeException();
		}
	}

	@Override
	public void afterSim() {
		ExternAfterSim aft = ExternAfterSim.newBuilder().build();
		try {
			ExternAfterSimConfirmed resp = this.clientService.reqExternAfterSim(this.rpcCtr, aft);
		} catch (ServiceException e) {
			throw new RuntimeException();
		}

	}

	@Override
	public void setInternalInterface(InternalInterface internalInterface) {
		// TODO Auto-generated method stub

	}



	public boolean tryAddAgent(Extern2MATSim request) {
		playground.gregor.proto.ProtoMATSimInterface.Extern2MATSim.Agent a = request
				.getAgent();
		Id<Person> persId = Id.createPersonId(a.getId());
		QVehicle veh = this.vehicles.get(persId);
		Id<Link> nextLId = veh.getDriver().chooseNextLinkId();
		External2QAdapterLink ql = this.adapters.get(nextLId);
		if (ql.isAcceptingFromUpstream()) {
			double now = this.sim.getSimTimer().getTimeOfDay();
			this.em.processEvent(new LinkLeaveEvent(now, persId, veh
					.getDriver().getCurrentLinkId(), veh.getId()));

			veh.getDriver().notifyMoveOverNode(nextLId);
			ql.addFromUpstream(veh);
			return true;
		}
		return false;
	}

	public boolean hasSpace(Id<Node> id) {
		MATSim2ExternHasSpace spaceReq = MATSim2ExternHasSpace.newBuilder()
				.setNodeId(id.toString()).build();
		MATSim2ExternHasSpaceConfirmed spaceReqResp = null;
		try {
			spaceReqResp = this.clientService.reqMATSim2ExternHasSpace(this.rpcCtr,
					spaceReq);
		} catch (ServiceException e) {
			throw new RuntimeException(e);
		}
		return spaceReqResp.getHasSpace();
	}

	public void addFromUpstream(Id<Node> enterId, Id<Node> leaveId, QVehicle veh) {
		Id<Person> driverId = veh.getDriver().getId();
		this.vehicles.put(driverId, veh);
		MATSim2ExternPutAgent addReq = MATSim2ExternPutAgent
				.newBuilder()
				.setAgent(
						Agent.newBuilder().setId(driverId.toString())
								.setEnterNode(enterId.toString())
								.addNodes(leaveId.toString()).build()).build();
		try {
			MATSim2ExternPutAgentConfirmed addReqResp = this.clientService
					.reqMATSim2ExternPutAgent(this.rpcCtr, addReq);
		} catch (ServiceException e) {
			throw new RuntimeException(e);
		}
	}

	public void registerAdapter(External2QAdapterLink external2qAdapterLink) {
		this.adapters.put(external2qAdapterLink.getLink().getId(),
				external2qAdapterLink);
	}

	public EventsManager getEventsManager() {
		return this.em;
	}

	public ObservableMobsim getMobsim() {
		return this.sim;
	}



	public void fireStuckEvents(AgentsStuck request) {
		double now = this.sim.getSimTimer().getTimeOfDay();
		for (String id : request.getAgentIdList()){
			Id<Person> persId = Id.createPersonId(id);
			QVehicle veh = this.vehicles.get(persId);
			PersonStuckEvent e = new PersonStuckEvent(now, persId, veh.getDriver().getCurrentLinkId(), veh.getDriver().getMode());
			this.em.processEvent(e);
		}
		
	}

}
