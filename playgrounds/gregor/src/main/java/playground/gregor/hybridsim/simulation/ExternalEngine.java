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

package playground.gregor.hybridsim.simulation;


import io.grpc.stub.StreamObserver;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.mobsim.qsim.qnetsimengine.QLinkInternalIAdapter;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;
import org.matsim.hybrid.MATSimInterface;
import org.matsim.hybrid.MATSimInterface.AgentsStuck;
import org.matsim.hybrid.MATSimInterface.AgentsStuckConfirmed;
import org.matsim.hybrid.MATSimInterface.Extern2MATSim;
import org.matsim.hybrid.MATSimInterface.Extern2MATSimConfirmed;
import org.matsim.hybrid.MATSimInterface.ExternAfterSim;
import org.matsim.hybrid.MATSimInterface.ExternAfterSimConfirmed;
import org.matsim.hybrid.MATSimInterface.ExternDoSimStep;
import org.matsim.hybrid.MATSimInterface.ExternDoSimStepReceived;
import org.matsim.hybrid.MATSimInterface.ExternOnPrepareSim;
import org.matsim.hybrid.MATSimInterface.ExternOnPrepareSimConfirmed;
import org.matsim.hybrid.MATSimInterface.ExternSimStepFinished;
import org.matsim.hybrid.MATSimInterface.ExternSimStepFinishedReceived;
import org.matsim.hybrid.MATSimInterface.ExternalConnect;
import org.matsim.hybrid.MATSimInterface.ExternalConnectConfirmed;
import org.matsim.hybrid.MATSimInterface.MATSim2ExternHasSpace;
import org.matsim.hybrid.MATSimInterface.MATSim2ExternHasSpaceConfirmed;
import org.matsim.hybrid.MATSimInterface.MATSim2ExternPutAgent;
import org.matsim.hybrid.MATSimInterface.MATSim2ExternPutAgent.Agent.Builder;
import org.matsim.hybrid.MATSimInterface.MATSim2ExternPutAgentConfirmed;
import org.matsim.hybrid.MATSimInterface.MaximumNumberOfAgents;
import org.matsim.hybrid.MATSimInterfaceServiceGrpc.MATSimInterfaceService;

import playground.gregor.hybridsim.grpc.GRPCExternalClient;
import playground.gregor.hybridsim.grpc.GRPCInternalServer;

public class ExternalEngine implements MobsimEngine, MATSimInterfaceService {

	private static final Logger log = Logger.getLogger(ExternalEngine.class);

	private final CyclicBarrier simStepBarrier = new CyclicBarrier(2);
	private final Map<Id<Person>, QVehicle> vehicles = new HashMap<>();
	private final Map<Id<Link>, QLinkInternalIAdapter> adapters = new HashMap<>();
	private final EventsManager em;
	private final Netsim sim;
	private GRPCExternalClient client;

	private final GRPCInternalServer server;

	private final Thread t1;

	private final CyclicBarrier clientBarrier = new CyclicBarrier(2);

	public ExternalEngine(EventsManager eventsManager, Netsim sim) {
		this.em = eventsManager;
		this.sim = sim;
		CyclicBarrier startupBarrier = new CyclicBarrier(2);
		this.server = new GRPCInternalServer(this, startupBarrier );
		this.t1 = new Thread(this.server);
		this.t1.start();
		try {
			startupBarrier.await();
		} catch (InterruptedException | BrokenBarrierException e) {
			throw new RuntimeException(e);
		}
//				new Thread(new RunJupedSim()).start();
		//		DummyJuPedSim.main(null);
//		new Thread(new DummyJuPedSim()).start();


	}

	@Override
	public void setInternalInterface(InternalInterface internalInterface) {
		// TODO Auto-generated method stub

	}
	public void registerAdapter(QLinkInternalIAdapter external2qAdapterLink) {
		this.adapters.put(external2qAdapterLink.getLink().getId(),
				external2qAdapterLink);
	}

	public EventsManager getEventsManager() {
		return this.em;
	}

	public Netsim getMobsim() {
		return this.sim;
	}




	//rpc extern --> MATSim

	@Override
	public void reqExtern2MATSim(Extern2MATSim request,
			StreamObserver<Extern2MATSimConfirmed> responseObserver) {
		Extern2MATSim.Agent a = request
				.getAgent();
		Id<Person> persId = Id.createPersonId(a.getId());
		QVehicle veh = this.vehicles.get(persId);
		Id<Link> nextLId = veh.getDriver().chooseNextLinkId();
		QLinkInternalIAdapter ql = this.adapters.get(nextLId);
		double now = this.sim.getSimTimer().getTimeOfDay();
		if (ql == null) {
			this.em.processEvent(new LinkLeaveEvent(now, persId, veh
					.getDriver().getCurrentLinkId(), veh.getId()));
			veh.getDriver().notifyMoveOverNode(nextLId);
			nextLId = veh.getDriver().chooseNextLinkId();
			this.em.processEvent(new LinkEnterEvent(now, persId, nextLId, veh.getId()));
			ql = this.adapters.get(nextLId);
		}
		org.matsim.hybrid.MATSimInterface.Extern2MATSimConfirmed.Builder b = Extern2MATSimConfirmed.newBuilder();

		if (ql.isAcceptingFromUpstream()) {

			this.em.processEvent(new LinkLeaveEvent(now, persId, veh
					.getDriver().getCurrentLinkId(), veh.getId()));
			veh.getDriver().notifyMoveOverNode(nextLId);

			ql.addFromUpstream(veh);
			b.setAccepted(true);
		} else {
			b.setAccepted(false);
		}
		responseObserver.onValue(b.build());
		responseObserver.onCompleted();
	}



	@Override
	public void reqAgentStuck(AgentsStuck request,
			StreamObserver<AgentsStuckConfirmed> responseObserver) {
		double now = this.sim.getSimTimer().getTimeOfDay();

		for (String id : request.getAgentIdList()){
			Id<Person> persId = Id.createPersonId(id);
			QVehicle veh = this.vehicles.get(persId);
			PersonStuckEvent e = new PersonStuckEvent(now, persId, veh.getDriver().getCurrentLinkId(), veh.getDriver().getMode());
			this.em.processEvent(e);
		}

		AgentsStuckConfirmed resp = AgentsStuckConfirmed.newBuilder().build();
		responseObserver.onValue(resp);
		responseObserver.onCompleted();
	}



	@Override
	public void reqExternalConnect(ExternalConnect request,
			StreamObserver<ExternalConnectConfirmed> responseObserver) {
		String host = request.getHost();
		int port = request.getPort();
		log.info("client connected. openning backward channel to host:" + host + " at port:" + port);
		this.client = new GRPCExternalClient("localhost",port);
		ExternalConnectConfirmed resp = ExternalConnectConfirmed.newBuilder().build();
		responseObserver.onValue(resp);
		responseObserver.onCompleted();
		log.info("client up and running.");
		try {
			this.clientBarrier.await();
		} catch (InterruptedException | BrokenBarrierException e) {
			throw new RuntimeException(e);
		}
	}



	@Override
	public void reqExternSimStepFinished(ExternSimStepFinished request,
			StreamObserver<ExternSimStepFinishedReceived> responseObserver) {
		try {
			this.simStepBarrier.await();
		} catch (InterruptedException | BrokenBarrierException e) {
			e.printStackTrace();
		}
		ExternSimStepFinishedReceived resp = ExternSimStepFinishedReceived.newBuilder().build();
		responseObserver.onValue(resp);
		responseObserver.onCompleted();
	}

	//rpc MATSim --> extern

	@Override
	public void doSimStep(double time) {
		double to = time + 1;
		ExternDoSimStep doSimStepExternal = ExternDoSimStep.newBuilder()
				.setFromTime(time).setToTime(to).build();
		ExternDoSimStepReceived resp = this.client.getBlockingStub().reqExternDoSimStep(doSimStepExternal);
		try {
			this.simStepBarrier.await();
		} catch (InterruptedException | BrokenBarrierException e) {
			throw new RuntimeException(e);
		}

	}

	@Override
	public void onPrepareSim() {
		
		try {
			this.clientBarrier.await(); //to make sure matsim --> extern connection is working 
		} catch (InterruptedException | BrokenBarrierException e) {
			throw new RuntimeException(e);
		}
		if (this.client == null){
			throw new RuntimeException("client is null");
		}
		
		MaximumNumberOfAgents n = MaximumNumberOfAgents.newBuilder().setNumber(this.sim.getScenario().getPopulation().getPersons().size()).build();
		this.client.getBlockingStub().reqMaximumNumberOfAgents(n);
		ExternOnPrepareSim prep = ExternOnPrepareSim.newBuilder().build();
		ExternOnPrepareSimConfirmed resp = this.client.getBlockingStub().reqExternOnPrepareSim(prep);
		
	}

	@Override
	public void afterSim() {
		ExternAfterSim aft = ExternAfterSim.newBuilder().build();
		ExternAfterSimConfirmed resp = this.client.getBlockingStub().reqExternAfterSim(aft);
		try {
			this.client.shutdown();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		this.server.shutdown();
	}

	public boolean hasSpace(Id<Node> id) {
		MATSim2ExternHasSpace spaceReq = MATSim2ExternHasSpace.newBuilder()
				.setNodeId(id.toString()).build();
		MATSim2ExternHasSpaceConfirmed spaceReqResp = null;
		spaceReqResp = this.client.getBlockingStub().reqMATSim2ExternHasSpace(spaceReq);
		return spaceReqResp.getHasSpace();
	}

	public void addFromUpstream(Id<Node> enterId, Id<Node> leaveId, QVehicle veh) {
		Id<Person> driverId = veh.getDriver().getId();
		this.vehicles.put(driverId, veh);
		Builder ab = MATSimInterface.MATSim2ExternPutAgent.Agent.newBuilder();
		ab.setEnterNode(enterId.toString()).setId(driverId.toString()).setLeaveNode(leaveId.toString());
		MATSim2ExternPutAgent addReq = MATSim2ExternPutAgent.newBuilder().setAgent(ab).build();
		MATSim2ExternPutAgentConfirmed addReqResp = this.client.getBlockingStub().reqMATSim2ExternPutAgent(addReq);
	}

}
