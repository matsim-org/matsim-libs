/* *********************************************************************** *
 * project: org.matsim.*
 * InvertedNetworkRoutingSignalsFixture
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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
package org.matsim.contrib.signals.integration.invertednetworks;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.signals.data.SignalsDataImpl;
import org.matsim.contrib.signals.model.DefaultPlanbasedSignalSystemController;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalControlData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalControlDataFactory;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalGroupSettingsData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalPlanData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalSystemControllerData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalGroupData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalGroupsData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalGroupsDataFactory;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemsData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemsDataFactory;
import org.matsim.contrib.signals.model.Signal;
import org.matsim.contrib.signals.model.SignalGroup;
import org.matsim.contrib.signals.model.SignalPlan;
import org.matsim.contrib.signals.model.SignalSystem;
import org.matsim.core.config.ConfigUtils;
import org.matsim.contrib.signals.SignalSystemsConfigGroup;

/**
 * @author dgrether
 *
 */
public class InvertedNetworkRoutingSignalsFixture extends InvertedNetworkRoutingTestFixture {

	public InvertedNetworkRoutingSignalsFixture(boolean doCreateModes,
			boolean doCreateLanes, boolean doCreateSignals) {
		super(doCreateModes, doCreateLanes, doCreateSignals);
		if (doCreateSignals){
			scenario.getConfig().scenario().setUseSignalSystems(true);
			createSignals();
		}
	}
	
	private void createSignals() {
		SignalsData signalsData = new SignalsDataImpl(ConfigUtils.addOrGetModule(scenario.getConfig(), SignalSystemsConfigGroup.GROUPNAME, SignalSystemsConfigGroup.class));
		this.scenario.addScenarioElement(SignalsData.ELEMENT_NAME, signalsData);
		SignalSystemsData ssd = signalsData.getSignalSystemsData();
		SignalSystemsDataFactory f = ssd.getFactory();
		SignalSystemData system = f.createSignalSystemData(Id.create(2, SignalSystem.class));
		ssd.addSignalSystemData(system);
		SignalData signal = f.createSignalData(Id.create(1, Signal.class));
		signal.setLinkId(Id.create(12, Link.class));
		signal.addTurningMoveRestriction(Id.create(25, Link.class));
		system.addSignalData(signal);
		SignalGroupsData sgd = signalsData.getSignalGroupsData();
		SignalGroupsDataFactory fsg = sgd.getFactory();
		SignalGroupData sg = fsg.createSignalGroupData(Id.create(2, SignalSystem.class), Id.create(2, SignalGroup.class));
		sg.addSignalId(Id.create(1, Signal.class));
		sgd.addSignalGroupData(sg);
		SignalControlData scd = signalsData.getSignalControlData();
		SignalControlDataFactory fsc = scd.getFactory();
		SignalSystemControllerData controller = fsc.createSignalSystemControllerData(Id.create(2, SignalSystem.class));
		controller.setControllerIdentifier(DefaultPlanbasedSignalSystemController.IDENTIFIER);
		scd.addSignalSystemControllerData(controller);
		SignalPlanData plan = fsc.createSignalPlanData(Id.create(1, SignalPlan.class));
		plan.setStartTime(0.0);
		plan.setEndTime(23 * 3600.0);
		plan.setCycleTime(100);
		controller.addSignalPlanData(plan);
		SignalGroupSettingsData group = fsc.createSignalGroupSettingsData(Id.create(2, SignalGroup.class));
		group.setOnset(0);
		group.setDropping(100);
		plan.addSignalGroupSettings(group);
	}


}
