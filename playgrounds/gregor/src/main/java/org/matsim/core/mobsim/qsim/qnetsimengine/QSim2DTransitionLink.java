/* *********************************************************************** *
 * project: kai
 * KaiHiResLink.java
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

package org.matsim.core.mobsim.qsim.qnetsimengine;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.vis.snapshotwriters.VisData;
import playground.gregor.sim2d_v4.events.Sim2DAgentConstructEvent;
import playground.gregor.sim2d_v4.scenario.Sim2DEnvironment;
import playground.gregor.sim2d_v4.simulation.Sim2DAgentFactory;
import playground.gregor.sim2d_v4.simulation.Sim2DEngine;
import playground.gregor.sim2d_v4.simulation.physics.Sim2DAgent;
import playground.gregor.sim2d_v4.simulation.physics.TransitionAreaI;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

public class QSim2DTransitionLink extends AbstractQLink {

	private static final Logger log = Logger.getLogger(QSim2DTransitionLink.class);

	private final QNode toQueueNode;
	private final QNetwork qNetwork;
	private final Link link;
	private final Sim2DEngine sim2DEngine;

	private final QLinkInternalI qLinkDelegate;

	private final boolean transferToSim2D = true;

	private final Sim2DEnvironment env;


	private final Sim2DAgentFactory agentBuilder;
	private final double area;
	private final QLinkInternalI qPred;
	private TransitionAreaI ta;
	private double spawnX;
	private double spawnY;

	QSim2DTransitionLink(Link link, QNetwork network, QNode toQueueNode, Sim2DEngine hybridEngine, QLinkInternalI qLinkImpl, Sim2DEnvironment env, Sim2DAgentFactory builder) {
		super(link,network);
		this.link = link;
		this.qNetwork = network;
		this.toQueueNode = toQueueNode;
		this.sim2DEngine = hybridEngine;
		this.qLinkDelegate = qLinkImpl;
		this.env = env;
		this.agentBuilder = builder;
		
		
		Iterator<? extends Link> it = link.getFromNode().getInLinks().values().iterator();
		Link pred = it.next();
		if (pred.getAllowedModes().contains("walk2d") && it.hasNext()){
			pred = it.next();
		}
//		if (it.hasNext()) {
//			throw new RuntimeException();
//		}
		this.qPred = this.qNetwork.getNetsimLink(pred.getId());
		this.area = pred.getLength() * (pred.getCapacity()/1.3)/this.qNetwork.getNetwork().getCapacityPeriod();
		init();
	}

	private void init() {
		
//		Section sec = this.env.getSection(this.link);
//		if (sec != null) {
//			throw new RuntimeException();
//		}
		//TODO 
		//departure boxes based on flow capacity 
		//2D Agent initialization??
		//departure box section implementation 
		//section coupling 
	}

	@Override
	boolean doSimStep(double now) {
		if (!this.transferToSim2D) {
			return this.qLinkDelegate.doSimStep(now);
		}
		else {
			throw new UnsupportedOperationException();
		}
	}

	@Override
	void addFromUpstream(QVehicle veh) {
		if (!this.transferToSim2D) {
			this.qLinkDelegate.addFromUpstream(veh);
		} else {


			Sim2DAgent agent = this.agentBuilder.buildAgent(veh,this.spawnX+MatsimRandom.getRandom().nextDouble()-.5,this.spawnY+MatsimRandom.getRandom().nextDouble()-.5, this.ta.getPhysicalEnvironment());
			agent.setDesiredSpeed(this.getLink().getFreespeed());
//			int v = this.qPred.getAllNonParkedVehicles().size();
			this.ta.addAgentTransitionBuffer(agent,0);
			double now = this.qNetwork.simEngine.getMobsim().getSimTimer().getTimeOfDay();
			this.qNetwork.simEngine.getMobsim().getEventsManager().processEvent(
					new LinkEnterEvent(now, veh.getId(),
							this.getLink().getId()));
			this.qNetwork.simEngine.getMobsim().getEventsManager().processEvent(new Sim2DAgentConstructEvent(now, agent));
		}
	}

	@Override
	boolean isNotOfferingVehicle() {
		if (!this.transferToSim2D) {
			return this.qLinkDelegate.isNotOfferingVehicle();
		} else {

//			throw new UnsupportedOperationException() ;
			return this.qLinkDelegate.isNotOfferingVehicle();
		}
	}

	@Override
	QVehicle popFirstVehicle() {
		if (!this.transferToSim2D) {
			return this.qLinkDelegate.popFirstVehicle();
		} else {
			return this.qLinkDelegate.popFirstVehicle();
//			throw new UnsupportedOperationException() ;
		}
	}

	@Override
	QVehicle getFirstVehicle() {
		if (!this.transferToSim2D) {
			return this.qLinkDelegate.getFirstVehicle();
		} else {
			return this.qLinkDelegate.getFirstVehicle();
//			throw new UnsupportedOperationException() ;
		}
	}

	@Override
	double getLastMovementTimeOfFirstVehicle() {
		if (!this.transferToSim2D) {
			return this.qLinkDelegate.getLastMovementTimeOfFirstVehicle();
		} else {

			throw new UnsupportedOperationException() ;
		}
	}

	@Override
	boolean hasGreenForToLink(Id toLinkId) {
		if (!this.transferToSim2D) {
			return this.qLinkDelegate.hasGreenForToLink(toLinkId);
		} else {
			return this.qLinkDelegate.hasGreenForToLink(toLinkId);
//			throw new UnsupportedOperationException() ;
		}
	}

	@Override
	boolean isAcceptingFromUpstream() {
		if (!this.transferToSim2D) {
			return this.qLinkDelegate.isAcceptingFromUpstream();
		} else {
			return this.ta.hasBufferSpace();
		}
	}

	@Override
	public Link getLink() {
		if (!this.transferToSim2D) {
			return this.qLinkDelegate.getLink();
		} else {
			return this.link;
		}
	}

	@Override
	public void recalcTimeVariantAttributes(double time) {
		if (!this.transferToSim2D) {
			this.qLinkDelegate.recalcTimeVariantAttributes(time);
		} else {

			throw new UnsupportedOperationException();
		}
	}

	@Override
	public Collection<MobsimVehicle> getAllNonParkedVehicles() {
		if (!this.transferToSim2D) {
			return this.qLinkDelegate.getAllNonParkedVehicles();
		} else {

			throw new UnsupportedOperationException();
		}
	}

	@Override
	QNode getToNode() {
		if (!this.transferToSim2D) {
			return this.qLinkDelegate.getToNode();
		} else {

			throw new UnsupportedOperationException() ;
		}
	}

	@Override
	public VisData getVisData() {
		if (!this.transferToSim2D) {
			return this.qLinkDelegate.getVisData();
		} else {

			throw new UnsupportedOperationException();
		}
	}

	@Override
	QVehicle getParkedVehicle(Id vehicleId) {
		if (!this.transferToSim2D) {
			return this.qLinkDelegate.getParkedVehicle(vehicleId);
		} else {

			throw new UnsupportedOperationException();
		}
	}

	@Override
	void registerAdditionalAgentOnLink(MobsimAgent planAgent) {
		if (!this.transferToSim2D) {
			this.qLinkDelegate.registerAdditionalAgentOnLink(planAgent);
		} else {
			this.qLinkDelegate.registerAdditionalAgentOnLink(planAgent);
//			throw new UnsupportedOperationException() ;
		}
	}

	@Override
	MobsimAgent unregisterAdditionalAgentOnLink(Id mobsimAgentId) {
		if (!this.transferToSim2D) {
			return this.qLinkDelegate.unregisterAdditionalAgentOnLink(mobsimAgentId);
		} else {
			return this.qLinkDelegate.unregisterAdditionalAgentOnLink(mobsimAgentId);
//			throw new UnsupportedOperationException() ;
		}
	}

	@Override
	Collection<MobsimAgent> getAdditionalAgentsOnLink() {
		if (!this.transferToSim2D) {
			return this.qLinkDelegate.getAdditionalAgentsOnLink();
		} else {

			throw new UnsupportedOperationException() ;
		}
	}

	@Override
	void clearVehicles() {
		if (!this.transferToSim2D) {
			this.qLinkDelegate.clearVehicles();
		} else {
//			throw new UnsupportedOperationException() ;

		}
	}

	@Override
	QVehicle getVehicle(Id vehicleId) {
		if (!this.transferToSim2D) {
			return this.qLinkDelegate.getVehicle(vehicleId);
		} else {

			throw new UnsupportedOperationException() ;
		}
	}

	@Override
	void registerDriverAgentWaitingForCar(MobsimDriverAgent agent) {
		if (!this.transferToSim2D) {
			this.qLinkDelegate.registerDriverAgentWaitingForCar(agent);
		} else {

			throw new UnsupportedOperationException() ;
		}
	}

	@Override
	void registerDriverAgentWaitingForPassengers(MobsimDriverAgent agent) {
		if (!this.transferToSim2D) {
			this.qLinkDelegate.registerDriverAgentWaitingForPassengers(agent);
		} else {

			throw new UnsupportedOperationException() ;
		}
	}

	@Override
	MobsimAgent unregisterDriverAgentWaitingForPassengers(Id agentId) {
		if (!this.transferToSim2D) {
			return this.qLinkDelegate.unregisterDriverAgentWaitingForPassengers(agentId);
		} else {

			throw new UnsupportedOperationException() ;
		}
	}

	@Override
	void registerPassengerAgentWaitingForCar(MobsimAgent agent, Id vehicleId) {
		if (!this.transferToSim2D) {
			this.qLinkDelegate.registerPassengerAgentWaitingForCar(agent, vehicleId);
		} else {

			throw new UnsupportedOperationException() ;
		}
	}

	@Override
	MobsimAgent unregisterPassengerAgentWaitingForCar(MobsimAgent agent,
			Id vehicleId) {
		if (!this.transferToSim2D) {
			return this.qLinkDelegate.unregisterPassengerAgentWaitingForCar(agent, vehicleId);
		} else {

			throw new UnsupportedOperationException() ;
		}
	}

	@Override
	Set<MobsimAgent> getAgentsWaitingForCar(Id vehicleId) {
		if (!this.transferToSim2D) {
			return this.qLinkDelegate.getAgentsWaitingForCar(vehicleId);
		} else {

			throw new UnsupportedOperationException() ;
		}
	}

	@Deprecated
	public void createDepartureBox(TransitionAreaI psecBox, double spawnX,
			double spawnY) {
		this.ta = psecBox;
		this.spawnX = spawnX;
		this.spawnY = spawnY;
	}
	

}
