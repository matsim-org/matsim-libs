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

import java.rmi.RemoteException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.mobsim.framework.PersonAgent;
import org.matsim.core.mobsim.framework.events.SimulationAfterSimStepEvent;
import org.matsim.core.mobsim.framework.events.SimulationBeforeCleanupEvent;
import org.matsim.core.mobsim.framework.events.SimulationInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.SimulationAfterSimStepListener;
import org.matsim.core.mobsim.framework.listeners.SimulationBeforeCleanupListener;
import org.matsim.core.mobsim.framework.listeners.SimulationInitializedListener;
import org.matsim.lanes.otfvis.drawer.OTFLaneSignalDrawer;
import org.matsim.lanes.otfvis.io.OTFLaneReader;
import org.matsim.lanes.otfvis.io.OTFLaneWriter;
import org.matsim.lanes.otfvis.layer.OTFLaneLayer;
import org.matsim.pt.otfvis.FacilityDrawer;
import org.matsim.pt.qsim.TransitQSimulation;
import org.matsim.ptproject.qsim.interfaces.MobsimFeature;
import org.matsim.signalsystems.otfvis.io.OTFSignalReader;
import org.matsim.signalsystems.otfvis.io.OTFSignalWriter;
import org.matsim.signalsystems.otfvis.layer.OTFSignalLayer;
import org.matsim.vis.otfvis.data.DefaultConnectionManagerFactory;
import org.matsim.vis.otfvis.data.OTFConnectionManager;
import org.matsim.vis.otfvis.data.teleportation.OTFTeleportAgentsDataReader;
import org.matsim.vis.otfvis.data.teleportation.OTFTeleportAgentsDataWriter;
import org.matsim.vis.otfvis.data.teleportation.OTFTeleportAgentsDrawer;
import org.matsim.vis.otfvis.data.teleportation.OTFTeleportAgentsLayer;
import org.matsim.vis.otfvis.data.teleportation.TeleportationVisData;
import org.matsim.vis.otfvis.server.OnTheFlyServer;
import org.matsim.vis.snapshots.writers.VisMobsim;
import org.matsim.vis.snapshots.writers.VisMobsimFeature;

public class OTFVisMobsimFeature implements MobsimFeature, VisMobsimFeature, SimulationInitializedListener, SimulationAfterSimStepListener, SimulationBeforeCleanupListener  {

	private static final Logger log = Logger.getLogger("noname");
	
	protected OnTheFlyServer otfServer = null;

	private boolean ownServer = true;

	private boolean doVisualizeTeleportedAgents = false;

	private OTFConnectionManager connectionManager = new DefaultConnectionManagerFactory().createConnectionManager();

	private OTFTeleportAgentsDataWriter teleportationWriter;

	private VisMobsim queueSimulation;

	private final LinkedHashMap<Id, TeleportationVisData> visTeleportationData = 
		new LinkedHashMap<Id, TeleportationVisData>();

//	private final LinkedHashMap<Id, Integer> currentActivityNumbers = new LinkedHashMap<Id, Integer>();

	private final LinkedHashMap<Id, PersonAgent> agents = new LinkedHashMap<Id, PersonAgent>();

	public OTFVisMobsimFeature(VisMobsim queueSimulation) {
		this.queueSimulation = queueSimulation;
	}

	/*package*/ void setServer(OnTheFlyServer server) {
		this.otfServer = server;
		ownServer = false;
	}

	@Override
//	public void afterPrepareSim() {
	public void notifySimulationInitialized(SimulationInitializedEvent ev) {
		log.warn("receiving simulationInitializedEvent") ;
		if (ownServer) {
			UUID idOne = UUID.randomUUID();
			this.otfServer = OnTheFlyServer.createInstance("OTFServer_" + idOne.toString(), queueSimulation.getEventsManager());
			this.otfServer.setSimulation(this);
			if (this.doVisualizeTeleportedAgents) {
				this.teleportationWriter = new OTFTeleportAgentsDataWriter();
				this.otfServer.addAdditionalElement(this.teleportationWriter);
				this.connectionManager.connectWriterToReader(
						OTFTeleportAgentsDataWriter.class,
						OTFTeleportAgentsDataReader.class);
				this.connectionManager.connectReaderToReceiver(
						OTFTeleportAgentsDataReader.class,
						OTFTeleportAgentsDrawer.class);
				this.connectionManager.connectReceiverToLayer(
						OTFTeleportAgentsDrawer.class,
						OTFTeleportAgentsLayer.class);

			}
			if (queueSimulation instanceof TransitQSimulation) {
				this.otfServer
						.addAdditionalElement(new FacilityDrawer.DataWriter_v1_0(
								queueSimulation.getVisNetwork().getNetwork(),
								((ScenarioImpl) queueSimulation.getScenario())
										.getTransitSchedule(),
								((TransitQSimulation) queueSimulation)
										.getAgentTracker()));
				this.connectionManager.connectWriterToReader(
						FacilityDrawer.DataWriter_v1_0.class,
						FacilityDrawer.DataReader_v1_0.class);
				this.connectionManager.connectReaderToReceiver(
						FacilityDrawer.DataReader_v1_0.class,
						FacilityDrawer.DataDrawer.class);
			}
			if (this.queueSimulation.getScenario().getConfig().scenario()
					.isUseLanes()
					&& (!this.queueSimulation.getScenario().getConfig()
							.scenario().isUseSignalSystems())) {
				this.connectionManager.connectQLinkToWriter(OTFLaneWriter.class);
				this.connectionManager.connectWriterToReader(OTFLaneWriter.class,
						OTFLaneReader.class);
				this.connectionManager.connectReaderToReceiver(OTFLaneReader.class,
						OTFLaneSignalDrawer.class);
				this.connectionManager.connectReceiverToLayer(OTFLaneSignalDrawer.class,
						OTFLaneLayer.class);
				this.queueSimulation.getScenario().getConfig().otfVis().setScaleQuadTreeRect(true);
			} else if (this.queueSimulation.getScenario().getConfig()
					.scenario().isUseLanes()
					&& (this.queueSimulation.getScenario().getConfig()
							.scenario().isUseSignalSystems())) {
				// data source to writer
				this.connectionManager.connectQLinkToWriter(OTFSignalWriter.class);
				// writer -> reader: from server to client
				this.connectionManager.connectWriterToReader(OTFSignalWriter.class,
						OTFSignalReader.class);
				// reader to drawer (or provider to receiver)
				this.connectionManager.connectReaderToReceiver(OTFSignalReader.class,
						OTFLaneSignalDrawer.class);
				// drawer -> layer
				this.connectionManager.connectReceiverToLayer(OTFLaneSignalDrawer.class,
						OTFSignalLayer.class);
	       this.queueSimulation.getScenario().getConfig().otfVis().setScaleQuadTreeRect(true);
			}

			OTFClientLive client = null;
			client = new OTFClientLive("rmi:127.0.0.1:4019:OTFServer_" + idOne.toString(), this.connectionManager);
			new Thread(client).start();

			try {
				this.otfServer.pause();
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
//	public void beforeCleanupSim() {
	public void notifySimulationBeforeCleanup( SimulationBeforeCleanupEvent ev ) {
		if(ownServer) {
			this.otfServer.cleanup();
		}
		this.otfServer = null;
	}

	@Override
	public void beforeHandleAgentArrival(PersonAgent agent) {
		this.visTeleportationData.remove(agent.getPerson().getId());
	}

//	@Override
//	public void afterAfterSimStep(final double time) {
//		this.visualizeTeleportedAgents(time);
//		this.otfServer.updateStatus(time);
//	}
	
	@Override
	public void notifySimulationAfterSimStep(SimulationAfterSimStepEvent event) {
		double time = event.getSimulationTime() ;
		this.visualizeTeleportedAgents(time);
		this.otfServer.updateStatus(time);
	}


	@Override
	public void beforeHandleUnknownLegMode(double now, final PersonAgent agent, Link link) {
		this.visTeleportationData.put(agent.getPerson().getId() , 
				new TeleportationVisData(now, agent, link, this.queueSimulation.getVisNetwork().getNetwork().getLinks().get(agent.getDestinationLinkId())));
	}

	private void visualizeTeleportedAgents(double time) {
		if (this.doVisualizeTeleportedAgents) {
			this.teleportationWriter.setSrc(this.visTeleportationData);
			for (TeleportationVisData teleportationVisData : visTeleportationData.values()) {
				teleportationVisData.calculatePosition(time);
			}
		}
	}

	void setConnectionManager(OTFConnectionManager connectionManager) {
		this.connectionManager = connectionManager;
	}

	public void setVisualizeTeleportedAgents(boolean active) {
		this.doVisualizeTeleportedAgents = active;
	}

//	@Override
//	public void afterActivityBegins(PersonAgent agent) {
////		currentActivityNumbers.put(agent.getPerson().getId(), planElementIndex);
//	}

//	public LinkedHashMap<Id, Integer> getCurrentActivityNumbers() {
//		return currentActivityNumbers;
//	}

//	@Override
//	public void afterActivityEnds(PersonAgent agent, double time) {
////		currentActivityNumbers.remove(agent.getPerson().getId());
//	}

	@Override
	public VisMobsim getVisMobsim() {
		return queueSimulation;
	}

	public Map<Id, TeleportationVisData> getVisTeleportationData() {
		return visTeleportationData;
	}

	public Person findPersonAgent(Id agentId) {
		PersonAgent personAgentI = agents.get(agentId);
		if (personAgentI != null) {
			return personAgentI.getPerson();
		}
		return null;
	}

	@Override
	public void agentCreated(PersonAgent agent) {
		agents.put(agent.getPerson().getId(), agent);
	}

}