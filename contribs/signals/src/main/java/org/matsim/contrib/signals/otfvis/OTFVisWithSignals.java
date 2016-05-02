/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * OTFVisWithSignals.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2015 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */

package org.matsim.contrib.signals.otfvis;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.signals.SignalSystemsConfigGroup;
import org.matsim.contrib.signals.builder.FromDataBuilder;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalGroupsData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemsData;
import org.matsim.contrib.signals.mobsim.QSimSignalEngine;
import org.matsim.contrib.signals.mobsim.SignalEngine;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimUtils;
import org.matsim.core.mobsim.qsim.pt.TransitQSimEngine;
import org.matsim.core.mobsim.qsim.pt.TransitStopAgentTracker;
import org.matsim.lanes.data.v20.Lanes;
import org.matsim.vis.otfvis.handler.FacilityDrawer;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.vis.otfvis.*;
import org.matsim.vis.snapshotwriters.AgentSnapshotInfoFactory;
import org.matsim.vis.snapshotwriters.SnapshotLinkWidthCalculator;

public class OTFVisWithSignals {

	public static void playScenario(Scenario scenario){
		EventsManager events = EventsUtils.createEventsManager();
		QSim qSim = QSimUtils.createDefaultQSim(scenario, events);
		if ((boolean) ConfigUtils.addOrGetModule(scenario.getConfig(), SignalSystemsConfigGroup.GROUPNAME, SignalSystemsConfigGroup.class).isUseSignalSystems()){
			SignalEngine engine = new QSimSignalEngine(new FromDataBuilder(scenario, events).createAndInitializeSignalSystemsManager());
			qSim.addQueueSimulationListeners(engine);
		}

		OnTheFlyServer server = startServerAndRegisterWithQSim(scenario.getConfig(), scenario, events, qSim);
		OTFClientLiveWithSignals.run(scenario.getConfig(), server);

		qSim.run();
	}

	public static OnTheFlyServer startServerAndRegisterWithQSim(Config config, Scenario scenario, EventsManager events, QSim qSim) {
		OnTheFlyServer server = OnTheFlyServer.createInstance(scenario, events);

		// this is the start/stop facility, which may be used outside of otfvis:
		PlayPauseMobsimListener playPauseMobsimListener = new PlayPauseMobsimListener();
		server.setNotificationListener( playPauseMobsimListener ) ;
		qSim.addQueueSimulationListeners(playPauseMobsimListener);

		server.setSimulation(qSim);

		if (config.transit().isUseTransit()) {

			Network network = scenario.getNetwork();
			TransitSchedule transitSchedule = scenario.getTransitSchedule();
			TransitQSimEngine transitEngine = qSim.getTransitEngine();
			TransitStopAgentTracker agentTracker = transitEngine.getAgentTracker();

//			AgentSnapshotInfoFactory snapshotInfoFactory = qSim.getVisNetwork().getAgentSnapshotInfoFactory();
			SnapshotLinkWidthCalculator linkWidthCalculator = new SnapshotLinkWidthCalculator();
			linkWidthCalculator.setLinkWidthForVis( config.qsim().getLinkWidthForVis() );
			if (! Double.isNaN(network.getEffectiveLaneWidth())){
				linkWidthCalculator.setLaneWidth( network.getEffectiveLaneWidth() );
			}
			AgentSnapshotInfoFactory snapshotInfoFactory = new AgentSnapshotInfoFactory(linkWidthCalculator);

			FacilityDrawer.Writer facilityWriter = new FacilityDrawer.Writer(network, transitSchedule, agentTracker, snapshotInfoFactory);
			server.addAdditionalElement(facilityWriter);
		}

		if ( (config.qsim().isUseLanes() || config.network().getLaneDefinitionsFile()!=null ) 
				&& (!(boolean) ConfigUtils.addOrGetModule(config, SignalSystemsConfigGroup.GROUPNAME, SignalSystemsConfigGroup.class).isUseSignalSystems())) {
			ConfigUtils.addOrGetModule(config, OTFVisConfigGroup.GROUP_NAME, OTFVisConfigGroup.class).setScaleQuadTreeRect(true);
			OTFLaneWriter otfLaneWriter = new OTFLaneWriter(qSim.getVisNetwork(), (Lanes) scenario.getScenarioElement(Lanes.ELEMENT_NAME), scenario.getConfig());
			server.addAdditionalElement(otfLaneWriter);
		} else if ((boolean) ConfigUtils.addOrGetModule(config, SignalSystemsConfigGroup.GROUPNAME, SignalSystemsConfigGroup.class).isUseSignalSystems()) {
			ConfigUtils.addOrGetModule(config, OTFVisConfigGroup.GROUP_NAME, OTFVisConfigGroup.class).setScaleQuadTreeRect(true);
			SignalGroupStateChangeTracker signalTracker = new SignalGroupStateChangeTracker();
			events.addHandler(signalTracker);
			SignalsData signalsData = (SignalsData) scenario.getScenarioElement(SignalsData.ELEMENT_NAME);
			Lanes laneDefs = scenario.getLanes();
			SignalSystemsData systemsData = signalsData.getSignalSystemsData();
			SignalGroupsData groupsData = signalsData.getSignalGroupsData();
			OTFSignalWriter otfSignalWriter = new OTFSignalWriter(qSim.getVisNetwork(), laneDefs, scenario.getConfig(), systemsData, groupsData , signalTracker);
			server.addAdditionalElement(otfSignalWriter);
		}
		server.pause();
		return server;
	}


}
