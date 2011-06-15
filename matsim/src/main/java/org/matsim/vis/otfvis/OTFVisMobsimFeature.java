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
import java.util.UUID;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.config.Config;
import org.matsim.core.events.AdditionalTeleportationDepartureEvent;
import org.matsim.core.mobsim.framework.HasPerson;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.events.SimulationAfterSimStepEvent;
import org.matsim.core.mobsim.framework.events.SimulationBeforeCleanupEvent;
import org.matsim.core.mobsim.framework.events.SimulationBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.events.SimulationInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.SimulationAfterSimStepListener;
import org.matsim.core.mobsim.framework.listeners.SimulationBeforeCleanupListener;
import org.matsim.core.mobsim.framework.listeners.SimulationBeforeSimStepListener;
import org.matsim.core.mobsim.framework.listeners.SimulationInitializedListener;
import org.matsim.core.scenario.ScenarioImpl;
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
import org.matsim.vis.otfvis.caching.SimpleSceneLayer;
import org.matsim.vis.otfvis.data.DefaultConnectionManagerFactory;
import org.matsim.vis.otfvis.data.OTFConnectionManager;
import org.matsim.vis.otfvis.handler.OTFAgentsListHandler;
import org.matsim.vis.otfvis.opengl.layer.AgentPointDrawer;
import org.matsim.vis.snapshots.writers.AgentSnapshotInfo;
import org.matsim.vis.snapshots.writers.TeleportationVisData;
import org.matsim.vis.snapshots.writers.VisLink;
import org.matsim.vis.snapshots.writers.VisMobsim;
import org.matsim.vis.snapshots.writers.VisMobsimFeature;

public class OTFVisMobsimFeature implements VisMobsimFeature,
SimulationInitializedListener, SimulationBeforeSimStepListener, SimulationAfterSimStepListener, SimulationBeforeCleanupListener {

	private static final Logger log = Logger.getLogger(OTFVisMobsimFeature.class);

	protected OnTheFlyServer otfServer = null;

	private boolean doVisualizeTeleportedAgents = false;

	private OTFConnectionManager connectionManager = new DefaultConnectionManagerFactory().createConnectionManager();

	private OTFAgentsListHandler.Writer teleportationWriter;

	private VisMobsim queueSimulation;

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

	public OTFVisMobsimFeature(VisMobsim queueSimulation) {
		this.queueSimulation = queueSimulation;
	}

	/*
	 * TODO: Erzeugung des Servers hier rausschmeissen. In eine statische Factory in OnTheFlyServer
	 * verlegen. Der Server muss damit extern erzeugt und dieser Klasse im Konstruktor 
	 * uebergeben werden. Der Client muss ebenfalls extern erzeugt und gestartet werden.
	 * Und zwar unabhaengig davon. Das ist gerade der Witz am Client-Server-Prinzip.
	 * michaz feb 11
	 */
	@Override
	public void notifySimulationInitialized(SimulationInitializedEvent ev) {
		log.info("receiving simulationInitializedEvent") ;
		for ( MobsimAgent mag : this.queueSimulation.getAgents() ) {
			if ( mag instanceof MobsimAgent ) {
				MobsimAgent pag = (MobsimAgent) mag ;
				agents.put( pag.getId(), pag) ;
			}
		}

		Config config = this.queueSimulation.getScenario().getConfig();
		UUID idOne = UUID.randomUUID();
		this.otfServer = OnTheFlyServer.createInstance("OTFServer_" + idOne.toString(), queueSimulation.getEventsManager());
		this.otfServer.setSimulation(this);

		// the "connect" statements for the regular links are called by
		// new DefaultConnectionManagerFactory().createConnectionManager() above.  kai, aug'10

		this.teleportationWriter = new OTFAgentsListHandler.Writer();
		this.teleportationWriter.setSrc(visData.values());
		this.otfServer.addAdditionalElement(this.teleportationWriter);
		this.connectionManager.connectWriterToReader(
				OTFAgentsListHandler.Writer.class,
				OTFAgentsListHandler.class);
		this.connectionManager.connectReaderToReceiver(
				OTFAgentsListHandler.class,
				AgentPointDrawer.class);

		if (config.scenario().isUseTransit()) {
			this.otfServer.addAdditionalElement(new FacilityDrawer.DataWriter_v1_0(
					queueSimulation.getVisNetwork().getNetwork(),
					((ScenarioImpl) queueSimulation.getScenario()).getTransitSchedule(),
					((QSim) queueSimulation).getTransitEngine().getAgentTracker()
			));
			this.connectionManager.connectWriterToReader(
					FacilityDrawer.DataWriter_v1_0.class,
					FacilityDrawer.DataReader_v1_0.class);
			this.connectionManager.connectReaderToReceiver(
					FacilityDrawer.DataReader_v1_0.class,
					FacilityDrawer.DataDrawer.class);
			this.connectionManager.connectReceiverToLayer(FacilityDrawer.DataDrawer.class, SimpleSceneLayer.class);
		}
		if (config.scenario().isUseLanes() && (!config.scenario().isUseSignalSystems())) {
			this.otfServer.addAdditionalElement(new OTFLaneWriter(this.queueSimulation.getVisNetwork(), ((ScenarioImpl) this.queueSimulation.getScenario()).getLaneDefinitions()));
			this.connectionManager.connectWriterToReader(OTFLaneWriter.class, OTFLaneReader.class);
			this.connectionManager.connectReaderToReceiver(OTFLaneReader.class, OTFLaneSignalDrawer.class);
			this.connectionManager.connectReceiverToLayer(OTFLaneSignalDrawer.class, SimpleSceneLayer.class);
			config.otfVis().setScaleQuadTreeRect(true);
		} else if (config.scenario().isUseSignalSystems()) {
			SignalGroupStateChangeTracker signalTracker = new SignalGroupStateChangeTracker();
			this.queueSimulation.getEventsManager().addHandler(signalTracker);
			SignalsData signalsData = this.queueSimulation.getScenario().getScenarioElement(SignalsData.class);
			LaneDefinitions laneDefs = ((ScenarioImpl)this.queueSimulation.getScenario()).getLaneDefinitions();
			SignalSystemsData systemsData = signalsData.getSignalSystemsData();
			SignalGroupsData groupsData = signalsData.getSignalGroupsData();
			this.otfServer.addAdditionalElement(new OTFSignalWriter(this.queueSimulation.getVisNetwork(), laneDefs, systemsData, groupsData , signalTracker));
			this.connectionManager.connectWriterToReader(OTFSignalWriter.class, OTFSignalReader.class);
			this.connectionManager.connectReaderToReceiver(OTFSignalReader.class, OTFLaneSignalDrawer.class);
			this.connectionManager.connectReceiverToLayer(OTFLaneSignalDrawer.class, SimpleSceneLayer.class);
			config.otfVis().setScaleQuadTreeRect(true);
		}

		OTFClientLive client = new OTFClientLive(this.otfServer, this.connectionManager);
		new Thread(client).start();

		this.otfServer.pause();
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
		this.otfServer.blockUpdates();
	}
	
	@Override
	public void notifySimulationAfterSimStep(SimulationAfterSimStepEvent event) {
		double time = event.getSimulationTime() ;
		this.updateTeleportedAgents(time);
		this.visualizeTrackedAndTeleportingAgents();
		this.otfServer.unblockUpdates();
		this.otfServer.updateStatus(time);
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

	void setConnectionManager(OTFConnectionManager connectionManager) {
		this.connectionManager = connectionManager;
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

	public Person findPersonAgent(Id agentId) {
		// yy should probably be called findPerson.  kai, jun'11 

		MobsimAgent personAgent = agents.get(agentId);
		if (personAgent != null && personAgent instanceof HasPerson ) {
			Person person = ((HasPerson)personAgent).getPerson();
			return person ;
		}
		return null;
	}

	public void addTrackedAgent(Id agentId) {
		trackedAgents.add(agentId);
	}

}