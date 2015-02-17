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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.interfaces.Mobsim;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;
import org.matsim.core.mobsim.qsim.qnetsimengine.External2QAdapterLink;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;

import playground.gregor.proto.ProtoMATSimInterface;
import playground.gregor.proto.ProtoMATSimInterface.Extern2MATSim;
import playground.gregor.proto.ProtoMATSimInterface.ExternDoSimStep;
import playground.gregor.proto.ProtoMATSimInterface.ExternDoSimStepReceived;
import playground.gregor.proto.ProtoMATSimInterface.ExternInterfaceService.BlockingInterface;
import playground.gregor.proto.ProtoMATSimInterface.MATSim2ExternHasSpace;
import playground.gregor.proto.ProtoMATSimInterface.MATSim2ExternHasSpaceConfirmed;
import playground.gregor.proto.ProtoMATSimInterface.MATSim2ExternPutAgent;
import playground.gregor.proto.ProtoMATSimInterface.MATSim2ExternPutAgent.Agent;
import playground.gregor.proto.ProtoMATSimInterface.MATSim2ExternPutAgentConfirmed;

import com.google.protobuf.RpcController;
import com.google.protobuf.ServiceException;
import com.googlecode.protobuf.pro.duplex.RpcClientChannel;
import com.googlecode.protobuf.pro.duplex.execute.ServerRpcController;

public class ExternalEngine implements MobsimEngine {

	private BlockingMATSimInterfaceService service;
	private Server server;
	private RpcController rpcCtr;

	CyclicBarrier simStepBarrier = new CyclicBarrier(2);
	private BlockingInterface clientService;
	private Map<Id<Person>, QVehicle> vehicles = new HashMap<>();
	private Map<Id<Link>, External2QAdapterLink> adapters = new HashMap<>();
	private EventsManager em;
	private Mobsim sim;

	public ExternalEngine(EventsManager eventsManager, Mobsim sim) {
		this.em = eventsManager;
		this.sim = sim;
	}

	@Override
	public void doSimStep(double time) {
		double to = time + 1;
		ExternDoSimStep doSimStepExternal = ExternDoSimStep.newBuilder()
				.setFromTime(time).setToTime(to).build();
		try {
			ExternDoSimStepReceived resp = clientService.reqExternDoSimStep(
					rpcCtr, doSimStepExternal);
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
		CyclicBarrier startupBarrier = new CyclicBarrier(2);

		this.service = new BlockingMATSimInterfaceService(startupBarrier,
				simStepBarrier, this);
		this.server = new Server(service);
		try {
			startupBarrier.await();// waiting for client to connect
		} catch (InterruptedException | BrokenBarrierException e) {
			throw new RuntimeException(e);
		}

	}

	@Override
	public void afterSim() {
		// TODO Auto-generated method stub

	}

	@Override
	public void setInternalInterface(InternalInterface internalInterface) {
		// TODO Auto-generated method stub

	}

	public void setRpcController(ServerRpcController controller) {
		RpcClientChannel c = ServerRpcController.getRpcChannel(controller);
		this.clientService = ProtoMATSimInterface.ExternInterfaceService
				.newBlockingStub(c);
		this.rpcCtr = c.newRpcController();
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
			spaceReqResp = clientService.reqMATSim2ExternHasSpace(rpcCtr,
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
			MATSim2ExternPutAgentConfirmed addReqResp = clientService
					.reqMATSim2ExternPutAgent(rpcCtr, addReq);
		} catch (ServiceException e) {
			throw new RuntimeException(e);
		}
	}

	public void registerAdapter(External2QAdapterLink external2qAdapterLink) {
		this.adapters.put(external2qAdapterLink.getLink().getId(),
				external2qAdapterLink);
	}

	public EventsManager getEventsManager() {
		return em;
	}

	public Mobsim getMobsim() {
		return this.sim;
	}

}
