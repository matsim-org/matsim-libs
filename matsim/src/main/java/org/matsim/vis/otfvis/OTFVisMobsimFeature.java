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
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.config.Config;
import org.matsim.core.events.AdditionalTeleportationDepartureEvent;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.PersonAgent;
import org.matsim.core.mobsim.framework.events.SimulationAfterSimStepEvent;
import org.matsim.core.mobsim.framework.events.SimulationBeforeCleanupEvent;
import org.matsim.core.mobsim.framework.events.SimulationInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.SimulationAfterSimStepListener;
import org.matsim.core.mobsim.framework.listeners.SimulationBeforeCleanupListener;
import org.matsim.core.mobsim.framework.listeners.SimulationInitializedListener;
import org.matsim.lanes.LaneDefinitions;
import org.matsim.lanes.otfvis.drawer.OTFLaneSignalDrawer;
import org.matsim.lanes.otfvis.io.OTFLaneReader;
import org.matsim.lanes.otfvis.io.OTFLaneWriter;
import org.matsim.pt.otfvis.FacilityDrawer;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.signalsystems.data.SignalsData;
import org.matsim.signalsystems.data.signalgroups.v20.SignalGroupsData;
import org.matsim.signalsystems.data.signalsystems.v20.SignalSystemsData;
import org.matsim.signalsystems.otfvis.io.OTFSignalReader;
import org.matsim.signalsystems.otfvis.io.OTFSignalWriter;
import org.matsim.signalsystems.otfvis.io.SignalGroupStateChangeTracker;
import org.matsim.vis.otfvis.data.DefaultConnectionManagerFactory;
import org.matsim.vis.otfvis.data.OTFConnectionManager;
import org.matsim.vis.otfvis.data.teleportation.OTFTeleportAgentsDataReader;
import org.matsim.vis.otfvis.data.teleportation.OTFTeleportAgentsDataWriter;
import org.matsim.vis.otfvis.data.teleportation.OTFTeleportAgentsDrawer;
import org.matsim.vis.otfvis.data.teleportation.OTFTeleportAgentsLayer;
import org.matsim.vis.otfvis.data.teleportation.TeleportationVisData;
import org.matsim.vis.snapshots.writers.VisMobsim;
import org.matsim.vis.snapshots.writers.VisMobsimFeature;

public class OTFVisMobsimFeature implements VisMobsimFeature,
SimulationInitializedListener, SimulationAfterSimStepListener, SimulationBeforeCleanupListener {

	private static final Logger log = Logger.getLogger(OTFVisMobsimFeature.class);

	protected OnTheFlyServer otfServer = null;

	private boolean ownServer = true;

	private boolean doVisualizeTeleportedAgents = false;

	private OTFConnectionManager connectionManager = new DefaultConnectionManagerFactory().createConnectionManager();

	private OTFTeleportAgentsDataWriter teleportationWriter;

	private VisMobsim queueSimulation;

	private final LinkedHashMap<Id, TeleportationVisData> visTeleportationData = new LinkedHashMap<Id, TeleportationVisData>();

	private final LinkedHashMap<Id, Person> agents = new LinkedHashMap<Id, Person>();

	public OTFVisMobsimFeature(VisMobsim queueSimulation) {
		this.queueSimulation = queueSimulation;
	}

	/*package*/ void setServer(OnTheFlyServer server) {
		this.otfServer = server;
		ownServer = false;
	}

	@Override
	public void notifySimulationInitialized(SimulationInitializedEvent ev) {
		log.info("receiving simulationInitializedEvent") ;
		for ( MobsimAgent mag : this.queueSimulation.getAgents() ) {
			if ( mag instanceof PersonAgent ) {
				PersonAgent pag = (PersonAgent) mag ;
				agents.put( pag.getPerson().getId(), pag.getPerson() ) ;
			}
		}

		if (ownServer) {
			Config config = this.queueSimulation.getScenario().getConfig();
			UUID idOne = UUID.randomUUID();
			this.otfServer = OnTheFlyServer.createInstance("OTFServer_" + idOne.toString(), queueSimulation.getEventsManager());
			this.otfServer.setSimulation(this);

			// the "connect" statements for the regular links are called by
			// new DefaultConnectionManagerFactory().createConnectionManager() above.  kai, aug'10

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
			if (config.scenario().isUseTransit()) {
				this.otfServer.addAdditionalElement(new FacilityDrawer.DataWriter_v1_0(
						queueSimulation.getVisNetwork().getNetwork(),
						((ScenarioImpl) queueSimulation.getScenario()).getTransitSchedule(),
						((QSim) queueSimulation).getQSimTransitEngine().getAgentTracker()
				));
				this.connectionManager.connectWriterToReader(
						FacilityDrawer.DataWriter_v1_0.class,
						FacilityDrawer.DataReader_v1_0.class);
				this.connectionManager.connectReaderToReceiver(
						FacilityDrawer.DataReader_v1_0.class,
						FacilityDrawer.DataDrawer.class);
			}
			if (config.scenario().isUseLanes() && (!config.scenario().isUseSignalSystems())) {
				this.otfServer.addAdditionalElement(new OTFLaneWriter(this.queueSimulation.getVisNetwork(), ((ScenarioImpl) this.queueSimulation.getScenario()).getLaneDefinitions()));
				this.connectionManager.connectWriterToReader(OTFLaneWriter.class, OTFLaneReader.class);
				this.connectionManager.connectReaderToReceiver(OTFLaneReader.class, OTFLaneSignalDrawer.class);
				config.otfVis().setScaleQuadTreeRect(true);
			} 
			else if (config.scenario().isUseSignalSystems()) {
				SignalGroupStateChangeTracker signalTracker = new SignalGroupStateChangeTracker();
				this.queueSimulation.getEventsManager().addHandler(signalTracker);
				SignalsData signalsData = this.queueSimulation.getScenario().getScenarioElement(SignalsData.class);
				LaneDefinitions laneDefs = ((ScenarioImpl)this.queueSimulation.getScenario()).getLaneDefinitions();
				SignalSystemsData systemsData = signalsData.getSignalSystemsData();
				SignalGroupsData groupsData = signalsData.getSignalGroupsData();
				this.otfServer.addAdditionalElement(new OTFSignalWriter(this.queueSimulation.getVisNetwork(), laneDefs, systemsData, groupsData , signalTracker));
				this.connectionManager.connectWriterToReader(OTFSignalWriter.class, OTFSignalReader.class);
				this.connectionManager.connectReaderToReceiver(OTFSignalReader.class, OTFLaneSignalDrawer.class);
				config.otfVis().setScaleQuadTreeRect(true);
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
	public void notifySimulationBeforeCleanup( SimulationBeforeCleanupEvent ev ) {
		if(ownServer) {
			this.otfServer.cleanup();
		}
		this.otfServer = null;
	}

	@Override
	public void handleEvent( AgentArrivalEvent ev ) {
		this.visTeleportationData.remove( ev.getPersonId() ) ;
	}

	@Override
	public void notifySimulationAfterSimStep(SimulationAfterSimStepEvent event) {
		double time = event.getSimulationTime() ;
		this.visualizeTeleportedAgents(time);
		this.otfServer.updateStatus(time);
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
		this.visTeleportationData.put( agentId , new TeleportationVisData( now, agentId, currLink, destLink, travTime ) );
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

	@Override
	public VisMobsim getVisMobsim() {
		return queueSimulation;
	}

	public Map<Id, TeleportationVisData> getVisTeleportationData() {
		return visTeleportationData;
	}

	public Person findPersonAgent(Id agentId) {
		Person person = agents.get(agentId);
		if (person != null) {
			return person ;
		}
		return null;
	}

}