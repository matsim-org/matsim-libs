/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * OTFVisModule.java
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
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalGroupsData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemsData;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.mobsim.framework.events.MobsimInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimInitializedListener;
import org.matsim.core.mobsim.qsim.AgentTracker;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.lanes.Lanes;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.vis.otfvis.OTFVisConfigGroup;
import org.matsim.vis.otfvis.OnTheFlyServer;
import org.matsim.vis.otfvis.handler.FacilityDrawer;
import org.matsim.vis.snapshotwriters.PositionInfo;
import org.matsim.vis.snapshotwriters.SnapshotLinkWidthCalculator;

import javax.inject.Inject;

public final class OTFVisWithSignalsLiveModule extends AbstractModule {

	@Override
	public void install() {
		this.addMobsimListenerBinding().to( OTFVisMobsimListener.class ) ;
	}

	private static class OTFVisMobsimListener implements MobsimInitializedListener{
		@Inject Scenario scenario ;
		@Inject EventsManager events ;
		@Override 
		public void notifyMobsimInitialized(MobsimInitializedEvent e) {
			QSim qsim = (QSim) e.getQueueSimulation() ; 
			OnTheFlyServer server = startServerAndRegisterWithQSim(scenario.getConfig(), scenario, events, qsim);
			OTFClientLiveWithSignals.run(scenario.getConfig(), server);
		}
	}
	
	static OnTheFlyServer startServerAndRegisterWithQSim(Config config, Scenario scenario, EventsManager events, QSim qSim) {
		OnTheFlyServer server = OnTheFlyServer.createInstance(scenario, events, qSim);
		Network network = scenario.getNetwork();
		TransitSchedule transitSchedule = scenario.getTransitSchedule();
//			TransitQSimEngine transitEngine = qSim.getTransitEngine();
//			TransitStopAgentTracker agentTracker = transitEngine.getAgentTracker();

//			AgentSnapshotInfoFactory snapshotInfoFactory = qSim.getVisNetwork().getAgentSnapshotInfoFactory();
		SnapshotLinkWidthCalculator linkWidthCalculator = new SnapshotLinkWidthCalculator();
		linkWidthCalculator.setLinkWidthForVis(config.qsim().getLinkWidthForVis());
		if (!Double.isNaN(network.getEffectiveLaneWidth())) {
			linkWidthCalculator.setLaneWidth(network.getEffectiveLaneWidth());
		}
		var snapshotInfoBuilder = new PositionInfo.LinkBasedBuilder().setLinkWidthCalculator(linkWidthCalculator);

		for (AgentTracker agentTracker : qSim.getAgentTrackers()) {
			FacilityDrawer.Writer facilityWriter = new FacilityDrawer.Writer(network, transitSchedule, agentTracker, snapshotInfoBuilder);
			server.addAdditionalElement(facilityWriter);
		}

		if ((config.qsim().isUseLanes() || config.network().getLaneDefinitionsFile() != null)
				&& (!(boolean) ConfigUtils.addOrGetModule(config, SignalSystemsConfigGroup.GROUP_NAME, SignalSystemsConfigGroup.class).isUseSignalSystems())) {
			ConfigUtils.addOrGetModule(config, OTFVisConfigGroup.GROUP_NAME, OTFVisConfigGroup.class).setScaleQuadTreeRect(true);
			OTFLaneWriter otfLaneWriter = new OTFLaneWriter(qSim.getVisNetwork(), (Lanes) scenario.getScenarioElement(Lanes.ELEMENT_NAME), scenario.getConfig());
			server.addAdditionalElement(otfLaneWriter);
		} else if (ConfigUtils.addOrGetModule(config, SignalSystemsConfigGroup.GROUP_NAME, SignalSystemsConfigGroup.class).isUseSignalSystems()) {
			ConfigUtils.addOrGetModule(config, OTFVisConfigGroup.GROUP_NAME, OTFVisConfigGroup.class).setScaleQuadTreeRect(true);
			SignalGroupStateChangeTracker signalTracker = new SignalGroupStateChangeTracker();
			events.addHandler(signalTracker);
			SignalsData signalsData = (SignalsData) scenario.getScenarioElement(SignalsData.ELEMENT_NAME);
			Lanes laneDefs = scenario.getLanes();
			SignalSystemsData systemsData = signalsData.getSignalSystemsData();
			SignalGroupsData groupsData = signalsData.getSignalGroupsData();
			OTFSignalWriter otfSignalWriter = new OTFSignalWriter(qSim.getVisNetwork(), laneDefs, scenario.getConfig(), systemsData, groupsData, signalTracker);
			server.addAdditionalElement(otfSignalWriter);
		}
		server.pause();
		return server;
	}
	
}
