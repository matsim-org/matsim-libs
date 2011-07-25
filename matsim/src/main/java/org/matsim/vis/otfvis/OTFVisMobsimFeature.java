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
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.events.AdditionalTeleportationDepartureEvent;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.PlanAgent;
import org.matsim.core.mobsim.framework.events.SimulationAfterSimStepEvent;
import org.matsim.core.mobsim.framework.events.SimulationBeforeCleanupEvent;
import org.matsim.core.mobsim.framework.events.SimulationBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.events.SimulationInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.SimulationAfterSimStepListener;
import org.matsim.core.mobsim.framework.listeners.SimulationBeforeCleanupListener;
import org.matsim.core.mobsim.framework.listeners.SimulationBeforeSimStepListener;
import org.matsim.core.mobsim.framework.listeners.SimulationInitializedListener;
import org.matsim.vis.otfvis.handler.OTFAgentsListHandler;
import org.matsim.vis.snapshots.writers.AgentSnapshotInfo;
import org.matsim.vis.snapshots.writers.TeleportationVisData;
import org.matsim.vis.snapshots.writers.VisLink;
import org.matsim.vis.snapshots.writers.VisMobsim;
import org.matsim.vis.snapshots.writers.VisMobsimFeature;

public class OTFVisMobsimFeature implements VisMobsimFeature,
SimulationInitializedListener, SimulationBeforeSimStepListener, SimulationAfterSimStepListener, SimulationBeforeCleanupListener {

	private static final Logger log = Logger.getLogger(OTFVisMobsimFeature.class);

	private final OnTheFlyServer server;

	private boolean doVisualizeTeleportedAgents = false;

	private final OTFAgentsListHandler.Writer teleportationWriter;

	public OTFAgentsListHandler.Writer getTeleportationWriter() {
		return teleportationWriter;
	}

	private final VisMobsim queueSimulation;

	/** 
	 * These are agents which are being teleported right now.
	 * This always has to be maintained, even if "show teleported agents" is off. Because the user might select an agent while
	 * it is teleporting and then of course it doesn't appear if this map doesn't have it.
	 * On the other hand, the interpolated coordinates are still only updated when we are interested in them, so the
	 * performance should be OK, I think.
	 * michaz feb 11
	 * 
	 */
	private final LinkedHashMap<Id, TeleportationVisData> teleportationData = new LinkedHashMap<Id, TeleportationVisData>();

	/**
	 * These are agents which should be visualised in addition to the agents which are visualised by the links themselves.
	 * This is used
	 * - for teleporting agents
	 * - for agents tracked by a query, because we want to always see them, no matter if the link decides that 
	 *   they should not be visualised (e.g. because "show parked cars" is switched off or something).
	 * michaz feb 11
	 */
	private final LinkedHashMap<Id, AgentSnapshotInfo> visData = new LinkedHashMap<Id, AgentSnapshotInfo>();

	private final LinkedHashMap<Id, MobsimAgent> agents = new LinkedHashMap<Id, MobsimAgent>();

	private final Set<Id> trackedAgents = new HashSet<Id>();

	public OTFVisMobsimFeature(OnTheFlyServer server, VisMobsim queueSimulation) {
		this.server = server;
		this.queueSimulation = queueSimulation;
		this.teleportationWriter = new OTFAgentsListHandler.Writer();
		this.teleportationWriter.setSrc(visData.values());
	}

	@Override
	public void notifySimulationInitialized(SimulationInitializedEvent ev) {
		log.info("receiving simulationInitializedEvent") ;
		for ( MobsimAgent mag : this.queueSimulation.getAgents() ) {
			if ( mag instanceof MobsimAgent ) {
				MobsimAgent pag = (MobsimAgent) mag ;
				agents.put( pag.getId(), pag) ;
			}
		}
	}

	@Override
	public void notifySimulationBeforeCleanup( SimulationBeforeCleanupEvent ev ) {
		// Nothing to do.
	}

	@Override
	public void handleEvent( AgentArrivalEvent ev ) {
		this.teleportationData.remove(ev.getPersonId());
	}

	@Override
	public void notifySimulationBeforeSimStep(SimulationBeforeSimStepEvent e) {
		this.server.blockUpdates();
	}
	
	@Override
	public void notifySimulationAfterSimStep(SimulationAfterSimStepEvent event) {
		double time = event.getSimulationTime() ;
		this.updateTeleportedAgents(time);
		this.visualizeTrackedAndTeleportingAgents();
		this.server.unblockUpdates();
		this.server.updateStatus(time);
	}

	private void visualizeTrackedAndTeleportingAgents() {
		visData.clear();
		for (TeleportationVisData agentInfo : teleportationData.values()) {
			if (this.doVisualizeTeleportedAgents || trackedAgents.contains(agentInfo.getId())) {
				this.visData.put(agentInfo.getId(), agentInfo);
			}
		}
		for (Id personId : trackedAgents) {
			Collection<AgentSnapshotInfo> positions = new ArrayList<AgentSnapshotInfo>();
			MobsimAgent agent = agents.get(personId);
			VisLink visLink = queueSimulation.getVisNetwork().getVisLinks().get(agent.getCurrentLinkId());
			visLink.getVisData().getVehiclePositions(positions);
			for (AgentSnapshotInfo position : positions) {
				if (position.getId().equals(personId)) {
					this.visData.put(position.getId(), position);
				}
			}
		}
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
		Link currLink = this.getVisMobsim().getScenario().getNetwork().getLinks().get( ev.getLinkId() ) ;
		Link destLink = this.getVisMobsim().getScenario().getNetwork().getLinks().get( ev.getDestinationLinkId() ) ;
		double travTime = ev.getTravelTime() ;
		TeleportationVisData agentInfo = new TeleportationVisData( now, agentId, currLink, destLink, travTime );
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

	public void removeTrackedAgent(Id id) {
		trackedAgents.remove(id);
	}

	@Override
	public VisMobsim getVisMobsim() {
		return queueSimulation;
	}

	public Plan findPlan(Id agentId) {
		MobsimAgent agent = agents.get(agentId);
		if (agent != null && agent instanceof PlanAgent ) {
			return ((PlanAgent) agent).getSelectedPlan();
		}
		return null;
	}

	public void addTrackedAgent(Id agentId) {
		trackedAgents.add(agentId);
	}

}