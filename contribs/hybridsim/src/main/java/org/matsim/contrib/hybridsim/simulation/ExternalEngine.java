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


import java.util.HashMap;
import java.util.List;
import java.util.Map;

//import de.dlr.sumo.hybridsim.HybridSimProto;
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
//import org.matsim.contrib.hybridsim.proto.HybridSimProto;
import org.matsim.contrib.hybridsim.proto.HybridSimProto;
import org.matsim.contrib.hybridsim.run.RunExample;
import org.matsim.contrib.hybridsim.utils.IdIntMapper;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.agents.PersonDriverAgentImpl;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.mobsim.qsim.qnetsimengine.QLinkInternalIAdapter;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;
import org.matsim.core.population.routes.NetworkRoute;

public class ExternalEngine implements MobsimEngine {//, MATSimInterfaceServiceGrpc.MATSimInterfaceService {

	private static final Logger log = Logger.getLogger(ExternalEngine.class);

	//	private final CyclicBarrier simStepBarrier = new CyclicBarrier(2);
	private final Map<String, QVehicle> vehicles = new HashMap<>();
	private final Map<Id<Link>, QLinkInternalIAdapter> adapters = new HashMap<>();
	private final EventsManager em;
	private final Network net;
	private final Scenario sc;
    private final IdIntMapper mapper;
//	private final CyclicBarrier clientBarrier = new CyclicBarrier(2);

	private GRPCExternalClient client;

    public ExternalEngine(EventsManager eventsManager, Scenario scenario, IdIntMapper mapper) {
        this.mapper = mapper;
        this.em = eventsManager;
		this.net = scenario.getNetwork();
		this.client = new GRPCExternalClient(RunExample.REMOTE_HOST, RunExample.REMOTE_PORT);
		this.sc = scenario;
	}

	public void registerAdapter(QLinkInternalIAdapter external2qAdapterLink) {
		this.adapters.put(external2qAdapterLink.getLink().getId(),
				external2qAdapterLink);
	}

	public EventsManager getEventsManager() {
		return this.em;
	}



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

	/*@Override
	public void setInternalInterface(InternalInterface internalInterface) {
		// TODO Auto-generated method stub

	}*/

	public boolean hasSpace(Id<Node> id) {
		//TODO: ask external sim if there is space
		return true;
	}

	public void addFromUpstream(QVehicle veh) {

		PersonDriverAgentImpl driver = (PersonDriverAgentImpl) veh.getDriver();

		this.vehicles.put(driver.getId().toString(), veh);
		Id<Link> currentLinkId = driver.getCurrentLinkId();


		Leg leg = (Leg) driver.getCurrentPlanElement();
		List<Id<Link>> linkIds = ((NetworkRoute) leg.getRoute()).getLinkIds();


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
                llb.setId(mapper.getIntLink(linkId));
                HybridSimProto.Coordinate.Builder cb = HybridSimProto.Coordinate.newBuilder();
				cb.setX(l.getCoord().getX());
				cb.setY(l.getCoord().getY());
				llb.setCentroid(cb);
//                llb.setFrTr(mapper.getIntNode(l.getFromNode().getId()));
//                llb.setToTr(mapper.getIntNode(l.getToNode().getId()));
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
