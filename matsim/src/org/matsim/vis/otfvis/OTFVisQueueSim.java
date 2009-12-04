/* *********************************************************************** *
 * project: org.matsim.*
 * OnTheFlyQueueSimQuad.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

import java.rmi.RemoteException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.matsim.api.basic.v01.Id;
import org.matsim.api.basic.v01.TransportMode;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.AgentDepartureEventImpl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.queuesim.DriverAgent;
import org.matsim.core.mobsim.queuesim.QueueNetwork;
import org.matsim.core.mobsim.queuesim.QueueSimEngine;
import org.matsim.core.mobsim.queuesim.QueueSimulation;
import org.matsim.vis.otfvis.data.DefaultConnectionManagerFactory;
import org.matsim.vis.otfvis.data.OTFConnectionManager;
import org.matsim.vis.otfvis.data.teleportation.OTFTeleportAgentsDataReader;
import org.matsim.vis.otfvis.data.teleportation.OTFTeleportAgentsDataWriter;
import org.matsim.vis.otfvis.data.teleportation.OTFTeleportAgentsDrawer;
import org.matsim.vis.otfvis.data.teleportation.OTFTeleportAgentsLayer;
import org.matsim.vis.otfvis.data.teleportation.TeleportationVisData;
import org.matsim.vis.otfvis.server.OnTheFlyServer;


/**
 * This class starts OTFVis in live mode, i.e. with a running QueueSimulation.
 * @author DS
 */
public class OTFVisQueueSim extends QueueSimulation{
	protected OnTheFlyServer myOTFServer = null;
	private boolean ownServer = true;
	private boolean doVisualizeTeleportedAgents = false;
	private OTFConnectionManager connectionManager = new DefaultConnectionManagerFactory().createConnectionManager();
	private OTFTeleportAgentsDataWriter teleportationWriter;
	private Map<Id, TeleportationVisData> visTeleportationData;
	
	public OTFVisQueueSim(final Scenario scenario, final EventsManager events) {
		super(scenario, events);
	}
	
	public void setServer(OnTheFlyServer server) {
		this.myOTFServer = server;
		ownServer = false;
	}
	@Override
	protected void prepareSim() {
		super.prepareSim();

		if(ownServer) {
			UUID idOne = UUID.randomUUID();
			this.myOTFServer = OnTheFlyServer.createInstance("OTFServer_" + idOne.toString(), this.network, this.population, getEvents(), false);

			if (this.doVisualizeTeleportedAgents){
				this.teleportationWriter = new OTFTeleportAgentsDataWriter();
				this.visTeleportationData = new LinkedHashMap<Id, TeleportationVisData>();
				this.myOTFServer.addAdditionalElement(this.teleportationWriter);
				this.connectionManager.add(OTFTeleportAgentsDataWriter.class, OTFTeleportAgentsDataReader.class);
				this.connectionManager.add(OTFTeleportAgentsDataReader.class, OTFTeleportAgentsDrawer.class);
				this.connectionManager.add(OTFTeleportAgentsDrawer.class, OTFTeleportAgentsLayer.class);
			}
			OTFClient client = null;
			client = new OTFClient("rmi:127.0.0.1:4019:OTFServer_" + idOne.toString(), this.connectionManager);
			client.start();

			try {
				this.myOTFServer.pause();
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	protected void cleanupSim() {
		if(ownServer) {
			this.myOTFServer.cleanup();
		}
		this.myOTFServer = null;
		super.cleanupSim();
	}
	
	@Override
	protected void handleAgentArrival(final double now, DriverAgent agent){
		if (this.doVisualizeTeleportedAgents){
			this.visTeleportationData.remove(agent.getPerson().getId());
			
		}
		super.handleAgentArrival(now, agent);
	}
	
	@Override
	protected void beforeSimStep(final double time) {
		super.beforeSimStep(time);
//		if (doVisualizeTeleportedAgents) {
//			this.visualizeTeleportedAgents(time);
//		}
	}

	@Override
	protected void afterSimStep(final double time) {
		super.afterSimStep(time);
		if (doVisualizeTeleportedAgents) {
			this.visualizeTeleportedAgents(time);
		}
		this.myOTFServer.updateStatus(time);
	}
	
	@Override
	protected void agentDeparts(double now, final DriverAgent agent, final Link link) {
		Leg leg = agent.getCurrentLeg();
		TransportMode mode = leg.getMode();
		getEvents().processEvent(new AgentDepartureEventImpl(now, agent.getPerson(), link, leg));
		if (this.getNotTeleportedModes().contains(mode)){
			this.handleKnownLegModeDeparture(now, agent, link, mode);
		}
		else if (this.doVisualizeTeleportedAgents){
			this.visAndHandleUnknownLegMode(now, agent, link);
		}
		else {
			this.handleUnknownLegMode(now, agent);
		}
	}
	
	protected void visAndHandleUnknownLegMode(double now, final DriverAgent agent, Link link){
		this.visTeleportationData.put(agent.getPerson().getId() , new TeleportationVisData(now, agent, link));
		super.handleUnknownLegMode(now, agent);
	}

	private void visualizeTeleportedAgents(double time) {
		this.teleportationWriter.setSrc(this.visTeleportationData);
		this.teleportationWriter.setTime(time);
	}

	public void setQueueNetwork(QueueNetwork net) {
		this.network = net;
		this.simEngine = new QueueSimEngine(this.network, MatsimRandom.getRandom());
	}
	
	public OTFConnectionManager getConnectionManager() {
		return this.connectionManager;
	}
	
	public void setConnectionManager(OTFConnectionManager connectionManager) {
		this.connectionManager = connectionManager;
	}
	
	public void setVisualizeTeleportedAgents(boolean active) {
		this.doVisualizeTeleportedAgents = active;
	}
}

