/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package org.matsim.vis.otfvis;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.events.AdditionalTeleportationDepartureEvent;
import org.matsim.core.events.handler.AdditionalTeleportationDepartureEventHandler;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.PlanAgent;
import org.matsim.core.mobsim.framework.events.MobsimAfterSimStepEvent;
import org.matsim.core.mobsim.framework.events.MobsimBeforeCleanupEvent;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.events.MobsimInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimAfterSimStepListener;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeCleanupListener;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeSimStepListener;
import org.matsim.core.mobsim.framework.listeners.MobsimInitializedListener;
import org.matsim.vis.snapshotwriters.AgentSnapshotInfo;
import org.matsim.vis.snapshotwriters.TeleportationVisData;
import org.matsim.vis.snapshotwriters.VisLink;
import org.matsim.vis.snapshotwriters.VisMobsim;
import org.matsim.vis.snapshotwriters.TeleportationVisData.Cache;

public class OTFVisMobsimFeature implements VisMobsimFeature, MobsimInitializedListener, MobsimBeforeSimStepListener, MobsimAfterSimStepListener, MobsimBeforeCleanupListener,
AgentArrivalEventHandler, AdditionalTeleportationDepartureEventHandler {

	private static final Logger log = Logger.getLogger(OTFVisMobsimFeature.class);

	private final OnTheFlyServer server;

	private boolean doVisualizeTeleportedAgents = false;

	private final VisMobsim queueSimulation;

	private final LinkedHashMap<Id, TeleportationVisData> teleportationData = new LinkedHashMap<Id, TeleportationVisData>();

	private final LinkedHashMap<Id, MobsimAgent> agents = new LinkedHashMap<Id, MobsimAgent>();

	private final Set<Id> trackedAgents = new HashSet<Id>();

	private Scenario scenario;

	/**
	 * I need a cache in TeleportationVisData that is not per Object, but for the run, and I don't want to use a 
	 * static variable.  Thus this cache.  kai, jul'12
	 */
	private Cache cache = null ;

	public OTFVisMobsimFeature(Scenario scenario, OnTheFlyServer server, VisMobsim queueSimulation) {
		this.scenario = scenario;
		this.server = server;
		this.queueSimulation = queueSimulation;
	}

	@Override
	public void notifyMobsimInitialized(MobsimInitializedEvent ev) {
		log.info("receiving simulationInitializedEvent");
		for (MobsimAgent mag : this.queueSimulation.getAgents()) {
			agents.put(mag.getId(), mag);
		}
	}

	@Override
	public void notifyMobsimBeforeCleanup( MobsimBeforeCleanupEvent ev ) {
		this.server.getSnapshotReceiver().finish();
	}

	@Override
	public void handleEvent( AgentArrivalEvent ev ) {
		this.teleportationData.remove(ev.getPersonId());
	}

	@Override
	public void notifyMobsimBeforeSimStep(MobsimBeforeSimStepEvent e) {
		this.server.blockUpdates();
	}
	
	@Override
	public void notifyMobsimAfterSimStep(MobsimAfterSimStepEvent event) {
		this.server.unblockUpdates();
		double time = event.getSimulationTime() ;
		this.updateTeleportedAgents(time);
		this.visualizeTrackedAndTeleportingAgents(time);
		this.server.updateStatus(time);
	}

	private void visualizeTrackedAndTeleportingAgents(double time) {
		server.getSnapshotReceiver().beginSnapshot(time);
		for (TeleportationVisData agentInfo : teleportationData.values()) {
			if (this.doVisualizeTeleportedAgents || trackedAgents.contains(agentInfo.getId())) {
				server.getSnapshotReceiver().addAgent(agentInfo);
			}
		}
		for (Id personId : trackedAgents) {
			Collection<AgentSnapshotInfo> positions = new ArrayList<AgentSnapshotInfo>();
			MobsimAgent agent = agents.get(personId);
			VisLink visLink = queueSimulation.getVisNetwork().getVisLinks().get(agent.getCurrentLinkId());
			visLink.getVisData().getVehiclePositions(positions);
			for (AgentSnapshotInfo position : positions) {
				if (position.getId().equals(personId)) {
					server.getSnapshotReceiver().addAgent(position);
				}
			}
		}
		server.getSnapshotReceiver().endSnapshot();
	}

	@Override
	public void reset( @SuppressWarnings("unused") int cnt ) {
		throw new UnsupportedOperationException("although it would be nice to have and should not be that difficult, at this point"
				+ " live mode does not support iterations. kai, aug'10" ) ;
	}

	@Override
	public void handleEvent( AdditionalTeleportationDepartureEvent ev ) {
		/*
		 * Note: I cannot, from the transport mode alone, differentiate between teleported and other agents, since teleportation has
		 * to do with interaction between mode and mobsim capabilities. Therefore, I need a separate event. My own intuition would
		 * be to move this into the mobsim ... since the mobsim should know where agents are, not the visualization. kai, aug'10
		 */
		Id agentId = ev.getAgentId() ;
		double now = ev.getTime() ;
		Link currLink = this.scenario.getNetwork().getLinks().get( ev.getLinkId() ) ;
		Link destLink = this.scenario.getNetwork().getLinks().get( ev.getDestinationLinkId() ) ;
		double travTime = ev.getTravelTime() ;
		TeleportationVisData agentInfo = new TeleportationVisData( now, agentId, currLink, destLink, travTime, this.cache  );
		this.teleportationData.put( agentId , agentInfo );
	}

	private void updateTeleportedAgents(double time) {
		for (TeleportationVisData teleportationVisData : teleportationData.values()) {
			if (this.doVisualizeTeleportedAgents || trackedAgents.contains(teleportationVisData.getId())) {
				teleportationVisData.calculatePosition(time);
			}
		}
	}

	public void setVisualizeTeleportedAgents(boolean active) {
		this.doVisualizeTeleportedAgents = active;
	}

	@Override
	public void removeTrackedAgent(Id id) {
		trackedAgents.remove(id);
	}

	@Override
	public VisMobsim getVisMobsim() {
		return queueSimulation;
	}

	@Override
	public Plan findPlan(Id agentId) {
		MobsimAgent agent = agents.get(agentId);
		if (agent != null && agent instanceof PlanAgent ) {
			return ((PlanAgent) agent).getSelectedPlan();
		}
		return null;
	}

	@Override
	public void addTrackedAgent(Id agentId) {
		trackedAgents.add(agentId);
	}

}