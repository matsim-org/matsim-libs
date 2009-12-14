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
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.ptproject.qsim.DriverAgent;
import org.matsim.ptproject.qsim.QueueSimulation;
import org.matsim.ptproject.qsim.QueueSimulationFeature;
import org.matsim.vis.otfvis.data.DefaultConnectionManagerFactory;
import org.matsim.vis.otfvis.data.OTFConnectionManager;
import org.matsim.vis.otfvis.data.teleportation.OTFTeleportAgentsDataReader;
import org.matsim.vis.otfvis.data.teleportation.OTFTeleportAgentsDataWriter;
import org.matsim.vis.otfvis.data.teleportation.OTFTeleportAgentsDrawer;
import org.matsim.vis.otfvis.data.teleportation.OTFTeleportAgentsLayer;
import org.matsim.vis.otfvis.data.teleportation.TeleportationVisData;
import org.matsim.vis.otfvis.server.OnTheFlyServer;


public class OTFVisQueueSim extends QueueSimulation {

	private OTFVisQueueSimFeature queueSimulationFeature;

	public OTFVisQueueSim(Network network, Population plans,
			EventsManager events) {
		super(network, plans, events);
		installFeature();
	}

	public OTFVisQueueSim(Scenario scenario, EventsManager events) {
		super(scenario, events);
		installFeature();
	}

	private void installFeature() {
		queueSimulationFeature = new OTFVisQueueSimFeature(this);
		super.addFeature(queueSimulationFeature);
	}

	public void setServer(OnTheFlyServer server) {
		queueSimulationFeature.setServer(server);
	}

	public void setVisualizeTeleportedAgents(boolean active) {
		queueSimulationFeature.setVisualizeTeleportedAgents(active);
	}

	public void setConnectionManager(OTFConnectionManager connectionManager) {
		queueSimulationFeature.setConnectionManager(connectionManager);
	}



	/**
	 * This class starts OTFVis in live mode, i.e. with a running QueueSimulation.
	 * @author DS
	 */
	public class OTFVisQueueSimFeature implements QueueSimulationFeature {
		
		protected OnTheFlyServer otfServer = null;
		private boolean ownServer = true;
		private boolean doVisualizeTeleportedAgents = false;
		private OTFConnectionManager connectionManager = new DefaultConnectionManagerFactory().createConnectionManager();
		private OTFTeleportAgentsDataWriter teleportationWriter;
		private Map<Id, TeleportationVisData> visTeleportationData;
		private QueueSimulation queueSimulation;
		
		public OTFVisQueueSimFeature(QueueSimulation queueSimulation) {
			this.queueSimulation = queueSimulation;
		}
		
		public void setServer(OnTheFlyServer server) {
			this.otfServer = server;
			ownServer = false;
		}
	
		public void afterPrepareSim() {
			if(ownServer) {
				UUID idOne = UUID.randomUUID();
				this.otfServer = OnTheFlyServer.createInstance("OTFServer_" + idOne.toString(), queueSimulation.getNetwork(), queueSimulation.getPopulation(), queueSimulation.getEvents(), false);
	
				if (this.doVisualizeTeleportedAgents){
					this.teleportationWriter = new OTFTeleportAgentsDataWriter();
					this.visTeleportationData = new LinkedHashMap<Id, TeleportationVisData>();
					this.otfServer.addAdditionalElement(this.teleportationWriter);
					this.connectionManager.add(OTFTeleportAgentsDataWriter.class, OTFTeleportAgentsDataReader.class);
					this.connectionManager.add(OTFTeleportAgentsDataReader.class, OTFTeleportAgentsDrawer.class);
					this.connectionManager.add(OTFTeleportAgentsDrawer.class, OTFTeleportAgentsLayer.class);
				}
				OTFClientLive client = null;
				client = new OTFClientLive("rmi:127.0.0.1:4019:OTFServer_" + idOne.toString(), this.connectionManager);
				client.start();
	
				try {
					this.otfServer.pause();
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
		}
	
		public void beforeCleanupSim() {
			if(ownServer) {
				this.otfServer.cleanup();
			}
			this.otfServer = null;
		}
	
		public void beforeHandleAgentArrival(DriverAgent agent) {
			if (this.doVisualizeTeleportedAgents){
				this.visTeleportationData.remove(agent.getPerson().getId());
			}
		}
	
		public void afterAfterSimStep(final double time) {
			if (doVisualizeTeleportedAgents) {
				this.visualizeTeleportedAgents(time);
			}
			this.otfServer.updateStatus(time);
		}
		
		public void beforeHandleUnknownLegMode(double now, final DriverAgent agent, Link link) {
			if (this.doVisualizeTeleportedAgents) {
				this.visTeleportationData.put(agent.getPerson().getId() , new TeleportationVisData(now, agent, link));
			}
		}
	
		private void visualizeTeleportedAgents(double time) {
			this.teleportationWriter.setSrc(this.visTeleportationData);
			this.teleportationWriter.setTime(time);
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
	
		public void afterCreateAgents() {
			
		}
		
	}
	
}

