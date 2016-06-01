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

package org.matsim.contrib.hybridsim.simulation;


import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.contrib.hybridsim.grpc.GRPCExternalClient;
import org.matsim.contrib.hybridsim.proto.HybridSimProto;
import org.matsim.contrib.hybridsim.run.Example;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.agents.PersonDriverAgentImpl;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.mobsim.qsim.qnetsimengine.QLinkInternalIAdapter;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;

import java.util.*;

public class ExternalEngine implements MobsimEngine {//, MATSimInterfaceServiceGrpc.MATSimInterfaceService {

	private static final Logger log = Logger.getLogger(ExternalEngine.class);

	//	private final CyclicBarrier simStepBarrier = new CyclicBarrier(2);
	private final Map<String, QVehicle> vehicles = new HashMap<>();
	private final Map<Id<Link>, QLinkInternalIAdapter> adapters = new HashMap<>();
	private final EventsManager em;
	private final Netsim sim;
	private final Network net;
	private final Scenario sc;
//	private final CyclicBarrier clientBarrier = new CyclicBarrier(2);

	private GRPCExternalClient client;

	public ExternalEngine(EventsManager eventsManager, Netsim sim) {
		this.em = eventsManager;
		this.sim = sim;
		this.net = sim.getScenario().getNetwork();
		this.client = new GRPCExternalClient(Example.REMOTE_HOST, Example.REMOTE_PORT);
		this.sc = sim.getScenario();
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

//	@Override
//	public void reqExtern2MATSim(Interfacedef.Extern2MATSim request,
//			StreamObserver<Interfacedef.Extern2MATSimConfirmed> responseObserver) {
//		Interfacedef.Extern2MATSim.Agent a = request
//				.getAgent();
//
////		log.info("Extern2MATSim " + request);
//
//		Id<Person> persId = Id.createPersonId(a.getId());
//		QVehicle veh = this.vehicles.get(persId);
//		Id<Link> nextLId = veh.getDriver().chooseNextLinkId();
//		QLinkInternalIAdapter ql = this.adapters.get(nextLId);
//		double now = this.sim.getSimTimer().getTimeOfDay();
//		Interfacedef.Extern2MATSimConfirmed.Builder b = Interfacedef.Extern2MATSimConfirmed.newBuilder();
//
//		if (ql.isAcceptingFromUpstream()) {
//			this.vehicles.remove(persId);
//			this.em.processEvent(new LinkLeaveEvent(now, veh.getId(), veh
//					.getDriver().getCurrentLinkId()));
//			veh.getDriver().notifyMoveOverNode(nextLId);
//
//			ql.addFromUpstream(veh);
//			b.setAccepted(true);
//		} else {
//			b.setAccepted(false);
//		}
//
//		Interfacedef.Extern2MATSimConfirmed resp = b.build();
//		responseObserver.onNext(resp);
//		responseObserver.onCompleted();
//	}


	//rpc extern --> MATSim

//	@Override
//	public void reqAgentStuck(Interfacedef.AgentsStuck request,
//			StreamObserver<Interfacedef.AgentsStuckConfirmed> responseObserver) {
//		double now = this.sim.getSimTimer().getTimeOfDay();
//
//		for (String id : request.getAgentIdList()){
//			Id<Person> persId = Id.createPersonId(id);
//			QVehicle veh = this.vehicles.get(persId);
//			PersonStuckEvent e = new PersonStuckEvent(now, persId, veh.getDriver().getCurrentLinkId(), veh.getDriver().getMode());
//			this.em.processEvent(e);
//		}
//
////		AgentsStuckConfirmed resp = AgentsStuckConfirmed.newBuilder().build();
//		Interfacedef.AgentsStuckConfirmed resp = Interfacedef.AgentsStuckConfirmed.getDefaultInstance();
//		responseObserver.onNext(resp);
//		responseObserver.onCompleted();
//	}

//	@Override
//	public void reqExternalConnect(Interfacedef.ExternalConnect request,
//			StreamObserver<Interfacedef.ExternalConnectConfirmed> responseObserver) {
//		String host = request.getHost();
//		host = "localhost";
//		int port = request.getPort();
//		log.info("client connected. openning backward channel to host:" + host + " at port:" + port);
//		this.client = new GRPCExternalClient(host,port);
//		Interfacedef.ExternalConnectConfirmed resp = Interfacedef.ExternalConnectConfirmed.getDefaultInstance();
//		responseObserver.onNext(resp);
//		responseObserver.onCompleted();
//		log.info("client up and running.");
//		try {
//			this.clientBarrier.await();
//		} catch (InterruptedException | BrokenBarrierException e) {
//			throw new RuntimeException(e);
//		}
//	}

//	@Override
//	public void reqExternSimStepFinished(Interfacedef.ExternSimStepFinished request,
//			StreamObserver<Interfacedef.ExternSimStepFinishedReceived> responseObserver) {
//		try {
//			this.simStepBarrier.await();
//		} catch (InterruptedException | BrokenBarrierException e) {
//			e.printStackTrace();
//		}
//		Interfacedef.ExternSimStepFinishedReceived resp = Interfacedef.ExternSimStepFinishedReceived.getDefaultInstance();
//		responseObserver.onNext(resp);
//		responseObserver.onCompleted();
//	}

//	@Override
//	public void reqMaximumNumberOfAgents(Interfacedef.MaximumNumberOfAgents request,
//			StreamObserver<Interfacedef.MaximumNumberOfAgentsConfirmed> responseObserver) {
//		Interfacedef.MaximumNumberOfAgentsConfirmed resp = Interfacedef.MaximumNumberOfAgentsConfirmed.newBuilder().setNumber(this.sim.getScenario().getPopulation().getPersons().size()).build();
//		responseObserver.onNext(resp);
//		responseObserver.onCompleted();
//
//	}

//	@Override
//	public void reqSendTrajectories(Interfacedef.Extern2MATSimTrajectories request,
//			StreamObserver<Interfacedef.MATSim2ExternTrajectoriesReceived> responseObserver) {
//
//		double time = this.sim.getSimTimer().getTimeOfDay();
//		for (Interfacedef.Extern2MATSimTrajectories.Agent a : request.getAgentList()) {
//			double x = a.getX();
//			double y = a.getY();
//			Id<Person> id = Id.createPersonId(a.getId());
//			if (!this.handled.contains(id)) {
//				this.handled.add(id);
//			}
//
//			double angle = a.getAngle();
//			double dx = Math.cos(Math.PI*angle/180);
//			double dy = Math.sin(Math.PI*angle/180);
//			XYVxVyEventImpl e = new XYVxVyEventImpl(id, x, y, dx, dy, time);
//			this.em.processEvent(e);
//		}
//
////		MATSim2ExternTrajectoriesReceived resp = MATSim2ExternTrajectoriesReceived.newBuilder().build();
//		Interfacedef.MATSim2ExternTrajectoriesReceived resp = Interfacedef.MATSim2ExternTrajectoriesReceived.getDefaultInstance();
//		responseObserver.onNext(resp);
//		responseObserver.onCompleted();
//	}

	@Override
	public void doSimStep(double time) {
		double to = time + 1;
		HybridSimProto.LeftClosedRightOpenTimeInterval req = HybridSimProto.LeftClosedRightOpenTimeInterval.newBuilder()
				.setFromTimeIncluding(time)
				.setToTimeExcluding(to).build();
		HybridSimProto.Empty resp = this.client.getBlockingStub().simulatedTimeInerval(req);

		//retrieve trajectories
		HybridSimProto.Empty reqTr = HybridSimProto.Empty.getDefaultInstance();
		HybridSimProto.Trajectories trs = this.client.getBlockingStub().receiveTrajectories(reqTr);
		for (HybridSimProto.Trajectory tr : trs.getTrajectoriesList()) {
			QVehicle veh = this.vehicles.get(tr.getId());
			Id<Link> nextLinkId = veh.getDriver().chooseNextLinkId();
			if (veh.getDriver().chooseNextLinkId().toString().equals(tr.getLinkId())) {

				this.em.processEvent(new LinkLeaveEvent(time, veh.getId(), veh.getCurrentLink().getId()));
				veh.getDriver().notifyMoveOverNode(nextLinkId);
				this.em.processEvent(new LinkEnterEvent(time, veh.getId(), nextLinkId));
			}
			//TODO xyvxvy events
		}

		//retrieve agents
		HybridSimProto.Empty reqRtrv = HybridSimProto.Empty.getDefaultInstance();
		HybridSimProto.Agents rtrvs = this.client.getBlockingStub().retrieveAgents(reqRtrv);
		//TODO check whether qsim has sapce (isAccaptingFromUpstream())
		for (HybridSimProto.Agent agent : rtrvs.getAgentsList()) {
			QVehicle v = this.vehicles.remove(agent.getId());
			QLinkInternalIAdapter ql = this.adapters.get(v.getDriver().chooseNextLinkId());
			if (!ql.isAcceptingFromUpstream()) {
				throw new RuntimeException("DS link is full, spill-back to external is not yet implemented!");
			}
			this.em.processEvent(new LinkLeaveEvent(time, v.getId(), v.getCurrentLink().getId()));
			v.getDriver().notifyMoveOverNode(ql.getLink().getId());
			ql.addFromUpstream(v);
		}

	}

	//rpc MATSim --> extern

	@Override
	public void onPrepareSim() {

		HybridSimProto.Scenario hsc = (HybridSimProto.Scenario) sc.getScenarioElement("hybrid_scenario");

		this.client.getBlockingStub().initScenario(hsc);

	}

	@Override
	public void afterSim() {
		HybridSimProto.Empty e = HybridSimProto.Empty.getDefaultInstance();
		this.client.getBlockingStub().shutdown(e);
		this.client.shutdown();

	}

	@Override
	public void setInternalInterface(InternalInterface internalInterface) {
		// TODO Auto-generated method stub

	}

	public boolean hasSpace(Id<Node> id) {
		//TODO: ask external sim if there is space
		return true;
	}

	public void addFromUpstream(QVehicle veh) {

		PersonDriverAgentImpl driver = (PersonDriverAgentImpl) veh.getDriver();

		this.vehicles.put(driver.getId().toString(), veh);
		Id<Link> currentLinkId = driver.getCurrentLinkId();


		Leg leg = (Leg) driver.getCurrentPlanElement();
		List<Id<Link>> linkIds = ((LinkNetworkRouteImpl) leg.getRoute()).getLinkIds();


		boolean extRd = false;

		HybridSimProto.Agent.Builder ab = HybridSimProto.Agent.newBuilder();
		ab.setId(driver.getId().toString());
		HybridSimProto.Leg.Builder lb = ab.getLegBuilder();
//		ab.setLeg(lb);

		for (Id<Link> linkId : linkIds) {

			if (!extRd && linkId == currentLinkId) {
				Link l = this.net.getLinks().get(linkId);
				ab.setEnterLocation(HybridSimProto.Coordinate.newBuilder().setX(l.getFromNode().getCoord().getX()).setY(l.getFromNode().getCoord().getY()));
				extRd = true;
			}
			if (extRd) {
				Link l = this.net.getLinks().get(linkId);
				if (!l.getAllowedModes().contains("ext")) {
					break;
				}
				HybridSimProto.Link.Builder llb = HybridSimProto.Link.newBuilder();
				llb.setId(linkId.toString());
				HybridSimProto.Coordinate.Builder cb = HybridSimProto.Coordinate.newBuilder();
				cb.setX(l.getCoord().getX());
				cb.setY(l.getCoord().getY());
				llb.setCentroid(cb);
				lb.addLink(llb);

				ab.setLeaveLocation(HybridSimProto.Coordinate.newBuilder().setX(l.getToNode().getCoord().getX()).setY(l.getToNode().getCoord().getY()));
			}
		}

		HybridSimProto.Boolean resp = this.client.getBlockingStub().transferAgent(ab.build());

//		if (resp.getVal()) {
//			log.info("success!");
//		}


//		double now = this.sim.getSimTimer().getTimeOfDay();
//		Id<Link> currentLID = veh.getDriver().getCurrentLinkId();
//		this.em.processEvent(new LinkLeaveEvent(now, veh.getId(), currentLID));
//		Id<Link> nextLId = veh.getDriver().chooseNextLinkId();
//		veh.getDriver().notifyMoveOverNode(nextLId);
//		this.em.processEvent(new LinkEnterEvent(now, veh.getId(), nextLId));


//		Interfacedef.MATSim2ExternPutAgent.Agent.Builder ab = Interfacedef.MATSim2ExternPutAgent.Agent.newBuilder();
//		ab.setEnterNode(enterId.toString()).setId(driverId.toString()).setLeaveNode(leaveId.toString());
//		Interfacedef.MATSim2ExternPutAgent addReq = Interfacedef.MATSim2ExternPutAgent.newBuilder().setAgent(ab).build();
//		Interfacedef.MATSim2ExternPutAgentConfirmed addReqResp = this.client.getBlockingStub().reqMATSim2ExternPutAgent(addReq);
	}


}
