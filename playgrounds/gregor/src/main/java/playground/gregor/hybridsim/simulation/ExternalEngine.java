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
import org.matsim.hybrid.MATSimInterface.*;
import org.matsim.hybrid.MATSimInterface.Extern2MATSimTrajectories.Agent;
import org.matsim.hybrid.MATSimInterface.MATSim2ExternPutAgent.Agent.Builder;
import org.matsim.hybrid.MATSimInterfaceServiceGrpc.MATSimInterfaceService;
import playground.gregor.hybridsim.events.ExternalAgentConstructEvent;
import playground.gregor.hybridsim.grpc.GRPCExternalClient;
import playground.gregor.hybridsim.grpc.GRPCInternalServer;
import playground.gregor.sim2d_v4.events.XYVxVyEventImpl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

public class ExternalEngine implements MobsimEngine, MATSimInterfaceService {

	private static final Logger log = Logger.getLogger(ExternalEngine.class);

	private final CyclicBarrier simStepBarrier = new CyclicBarrier(2);
	private final Map<Id<Person>, QVehicle> vehicles = new HashMap<>();
	private final Map<Id<Link>, QLinkInternalIAdapter> adapters = new HashMap<>();
	private final EventsManager em;
	private final Netsim sim;
	private final GRPCInternalServer server;
	private final Thread t1;
	private final CyclicBarrier clientBarrier = new CyclicBarrier(2);
	//testing only
	private final Set<Id<Person>> handled = new HashSet<>();
	private GRPCExternalClient client;
	private double onsetX = 1113711.9263903373;
	private double onsetY = 7041180.26922217;
	
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

	@Override
	public void reqExtern2MATSim(Extern2MATSim request,
			StreamObserver<Extern2MATSimConfirmed> responseObserver) {
		Extern2MATSim.Agent a = request
				.getAgent();

//		log.info("Extern2MATSim " + request);

		Id<Person> persId = Id.createPersonId(a.getId());
		QVehicle veh = this.vehicles.get(persId);
		Id<Link> nextLId = veh.getDriver().chooseNextLinkId();
		QLinkInternalIAdapter ql = this.adapters.get(nextLId);
		double now = this.sim.getSimTimer().getTimeOfDay();
		org.matsim.hybrid.MATSimInterface.Extern2MATSimConfirmed.Builder b = Extern2MATSimConfirmed.newBuilder();

		if (ql.isAcceptingFromUpstream()) {
			this.vehicles.remove(persId);
			this.em.processEvent(new LinkLeaveEvent(now, veh.getId(), veh
					.getDriver().getCurrentLinkId()));
			veh.getDriver().notifyMoveOverNode(nextLId);

			ql.addFromUpstream(veh);
			b.setAccepted(true);
		} else {
			b.setAccepted(false);
		}

		Extern2MATSimConfirmed resp = b.build();
		responseObserver.onNext(resp);
		responseObserver.onCompleted();
	}


	//rpc extern --> MATSim

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

//		AgentsStuckConfirmed resp = AgentsStuckConfirmed.newBuilder().build();
		AgentsStuckConfirmed resp = AgentsStuckConfirmed.getDefaultInstance();
		responseObserver.onNext(resp);
		responseObserver.onCompleted();
	}

	@Override
	public void reqExternalConnect(ExternalConnect request,
			StreamObserver<ExternalConnectConfirmed> responseObserver) {
		String host = request.getHost();
		host = "localhost";
		int port = request.getPort();
		log.info("client connected. openning backward channel to host:" + host + " at port:" + port);
		this.client = new GRPCExternalClient(host,port);
		ExternalConnectConfirmed resp = ExternalConnectConfirmed.getDefaultInstance();
		responseObserver.onNext(resp);
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
		ExternSimStepFinishedReceived resp = ExternSimStepFinishedReceived.getDefaultInstance();
		responseObserver.onNext(resp);
		responseObserver.onCompleted();
	}

	@Override
	public void reqMaximumNumberOfAgents(MaximumNumberOfAgents request,
			StreamObserver<MaximumNumberOfAgentsConfirmed> responseObserver) {
		MaximumNumberOfAgentsConfirmed resp = MaximumNumberOfAgentsConfirmed.newBuilder().setNumber(this.sim.getScenario().getPopulation().getPersons().size()).build();
		responseObserver.onNext(resp);
		responseObserver.onCompleted();

	}

	@Override
	public void reqSendTrajectories(Extern2MATSimTrajectories request,
			StreamObserver<MATSim2ExternTrajectoriesReceived> responseObserver) {

		double time = this.sim.getSimTimer().getTimeOfDay();
		for (Agent a : request.getAgentList()) {
			double x = a.getX();
			double y = a.getY();
			Id<Person> id = Id.createPersonId(a.getId());
			if (!this.handled.contains(id)) {
				ExternalAgentConstructEvent ee = new ExternalAgentConstructEvent(time, id);
				this.em.processEvent(ee);
				this.handled.add(id);

			}

			double angle = a.getAngle();
			double dx = Math.cos(Math.PI*angle/180);
			double dy = Math.sin(Math.PI*angle/180);
			XYVxVyEventImpl e = new XYVxVyEventImpl(id, x+onsetX, y+onsetY, dx, dy, time);
			this.em.processEvent(e);
		}

//		MATSim2ExternTrajectoriesReceived resp = MATSim2ExternTrajectoriesReceived.newBuilder().build();
		MATSim2ExternTrajectoriesReceived resp = MATSim2ExternTrajectoriesReceived.getDefaultInstance();
		responseObserver.onNext(resp);
		responseObserver.onCompleted();
	}

	@Override
	public void doSimStep(double time) {
		double to = time + 1;
		ExternDoSimStep doSimStepExternal = ExternDoSimStep.newBuilder()
				.setFromTime(time).setToTime(to).build();
		if (((int)time) % 60 == 0){
			log.info(doSimStepExternal);
		}
		ExternDoSimStepReceived resp = this.client.getBlockingStub().reqExternDoSimStep(doSimStepExternal);
		try {
			this.simStepBarrier.await();
		} catch (InterruptedException | BrokenBarrierException e) {
			throw new RuntimeException(e);
		}

	}

	//rpc MATSim --> extern

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

	@Override
	public void setInternalInterface(InternalInterface internalInterface) {
		// TODO Auto-generated method stub

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

		double now = this.sim.getSimTimer().getTimeOfDay();
		Id<Link> currentLID = veh.getDriver().getCurrentLinkId();
		this.em.processEvent(new LinkLeaveEvent(now, veh.getId(), currentLID));
		Id<Link> nextLId = veh.getDriver().chooseNextLinkId();
		veh.getDriver().notifyMoveOverNode(nextLId);
		this.em.processEvent(new LinkEnterEvent(now, veh.getId(), nextLId));



		Builder ab = MATSimInterface.MATSim2ExternPutAgent.Agent.newBuilder();
		ab.setEnterNode(enterId.toString()).setId(driverId.toString()).setLeaveNode(leaveId.toString());
		MATSim2ExternPutAgent addReq = MATSim2ExternPutAgent.newBuilder().setAgent(ab).build();
		MATSim2ExternPutAgentConfirmed addReqResp = this.client.getBlockingStub().reqMATSim2ExternPutAgent(addReq);
	}





}
