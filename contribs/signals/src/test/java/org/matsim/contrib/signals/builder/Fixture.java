/* *********************************************************************** *
 * project: org.matsim.*
 * Fixture
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package org.matsim.contrib.signals.builder;

import java.lang.reflect.Method;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.signals.SignalSystemsConfigGroup;
import org.matsim.contrib.signals.SignalSystemsConfigGroup.ActionOnSignalSpecsViolation;
import org.matsim.contrib.signals.SignalSystemsConfigGroup.IntersectionLogic;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.SignalsDataLoader;
import org.matsim.contrib.signals.data.conflicts.Direction;
import org.matsim.contrib.signals.data.conflicts.IntersectionDirections;
import org.matsim.contrib.signals.data.signalcontrol.v20.SignalGroupSettingsData;
import org.matsim.contrib.signals.data.signalcontrol.v20.SignalPlanData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalGroupData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemControllerData;
import org.matsim.contrib.signals.model.Signal;
import org.matsim.contrib.signals.model.SignalGroup;
import org.matsim.contrib.signals.model.SignalPlan;
import org.matsim.contrib.signals.model.SignalSystem;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.RoutingConfigGroup;
import org.matsim.core.config.groups.ScoringConfigGroup.ActivityParams;
import org.matsim.core.config.groups.ReplanningConfigGroup.StrategySettings;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.lanes.Lane;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author dgrether
 */
public class Fixture {

	final Id<Link> linkId1 = Id.create(1, Link.class);
	final Id<Link> linkId2 = Id.create(2, Link.class);
	final Id<Node> nodeId2 = Id.create(2, Node.class);
	final Id<SignalPlan> signalPlanId2 = Id.create(2, SignalPlan.class);
	final Id<SignalSystem> signalSystemId2 = Id.create(2, SignalSystem.class);
	final Id<SignalGroup> signalGroupId100 = Id.create(100, SignalGroup.class);

	// only available if 'TwoSignals'-Method is used
	final Id<SignalGroup> signalGroupId200 = Id.create(200, SignalGroup.class);
	final Id<Link> linkId6 = Id.create(6, Link.class);
	final Id<Node> nodeId6 = Id.create(6, Node.class);

	public Scenario createAndLoadTestScenarioOneSignal(Boolean useIntergreens) {
		Config conf = createConfigOneSignal(useIntergreens);
		Scenario scenario = ScenarioUtils.loadScenario(conf);
		scenario.addScenarioElement(SignalsData.ELEMENT_NAME, new SignalsDataLoader(conf).loadSignalsData());

		return scenario;
	}

	private Config createConfigOneSignal(Boolean useIntergreens) {
		MatsimTestUtils testUtils = new MatsimTestUtils();
		try {
			Method m = this.getClass().getMethod("createAndLoadTestScenarioOneSignal", Boolean.class);
			testUtils.initWithoutJUnitForFixture(this.getClass(), m);
		} catch (NoSuchMethodException | SecurityException e) {
			e.printStackTrace();
		}
		Config conf = ConfigUtils.createConfig(testUtils.classInputResourcePath());
		conf.controller().setOverwriteFileSetting(OverwriteFileSetting.overwriteExistingFiles);
		conf.routing().setNetworkRouteConsistencyCheck(RoutingConfigGroup.NetworkRouteConsistencyCheck.disable);
		ActivityParams params = new ActivityParams("h");
		params.setTypicalDuration(24.0 * 3600.0);
		conf.scoring().addActivityParams(params);

		StrategySettings settings = new StrategySettings(Id.create("1", StrategySettings.class));
		settings.setStrategyName("ChangeExpBeta");
		settings.setWeight(1.0);
		conf.replanning().addStrategySettings(settings);
		conf.network().setInputFile("network.xml.gz");
		conf.network().setLaneDefinitionsFile("testLaneDefinitions_v2.0.xml");
		conf.plans().setInputFile("plans1Agent.xml");
		conf.qsim().setUseLanes(true);
		conf.qsim().setStuckTime(1000);
		conf.qsim().setStartTime(0.0);
		conf.qsim().setUsingFastCapacityUpdate(false);
		SignalSystemsConfigGroup signalsConfig = ConfigUtils.addOrGetModule(conf, SignalSystemsConfigGroup.GROUP_NAME,
				SignalSystemsConfigGroup.class);
		signalsConfig.setUseSignalSystems(true);

		if (useIntergreens) {
			signalsConfig.setIntergreenTimesFile("testIntergreenTimes_v1.0.xml");
			signalsConfig.setUseIntergreenTimes(true);
			signalsConfig.setActionOnIntergreenViolation(ActionOnSignalSpecsViolation.EXCEPTION);
		}

		this.setSignalSystemConfigValues(signalsConfig, testUtils);
		return conf;
	}

	private void setSignalSystemConfigValues(SignalSystemsConfigGroup signalsConfig, MatsimTestUtils testUtils) {
		signalsConfig.setSignalSystemFile("testSignalSystems_v2.0.xml");
		signalsConfig.setSignalGroupsFile("testSignalGroups_v2.0.xml");
		signalsConfig.setSignalControlFile("testSignalControl_v2.0.xml");
		signalsConfig.setAmberTimesFile("testAmberTimes_v1.0.xml");
	}

	public Scenario createAndLoadTestScenarioTwoSignals(boolean useConflictData) {
		Config config = createConfigOneSignal(false);
		SignalSystemsConfigGroup signalsConfig = ConfigUtils.addOrGetModule(config, SignalSystemsConfigGroup.GROUP_NAME,
				SignalSystemsConfigGroup.class);
		signalsConfig.setIntersectionLogic(IntersectionLogic.CONFLICTING_DIRECTIONS_NO_TURN_RESTRICTIONS);
		signalsConfig.setActionOnConflictingDirectionViolation(ActionOnSignalSpecsViolation.EXCEPTION);

		Scenario scenario = ScenarioUtils.loadScenario(config);
		scenario.addScenarioElement(SignalsData.ELEMENT_NAME, new SignalsDataLoader(config).loadSignalsData());

		// modify scenario
		Network net = scenario.getNetwork();
		net.addNode(net.getFactory().createNode(nodeId6, new Coord(0, 100)));
		net.addLink(net.getFactory().createLink(linkId6, net.getNodes().get(nodeId2), net.getNodes().get(nodeId6)));
		SignalsData signalsData = (SignalsData)scenario.getScenarioElement(SignalsData.ELEMENT_NAME);
		// add another lane, signal and group
		Lane lane16 = scenario.getLanes().getFactory().createLane(Id.create("1-6", Lane.class));
		// use default parameters for capacity, length ... and just specify the to-link:
		lane16.addToLinkId(linkId6);
		scenario.getLanes().getLanesToLinkAssignments().get(linkId1).addLane(lane16);
		scenario.getLanes()
				.getLanesToLinkAssignments()
				.get(linkId1)
				.getLanes()
				.get(Id.create("1.ol", Lane.class))
				.addToLaneId(lane16.getId());
		;
		SignalData secondSignal = signalsData.getSignalSystemsData()
				.getFactory()
				.createSignalData(Id.create(200, Signal.class));
		secondSignal.setLinkId(linkId1);
		secondSignal.addLaneId(lane16.getId());
		secondSignal.addTurningMoveRestriction(linkId6);
		signalsData.getSignalSystemsData().getSignalSystemData().get(signalSystemId2).addSignalData(secondSignal);
		SignalGroupData secondGroup = signalsData.getSignalGroupsData()
				.getFactory()
				.createSignalGroupData(signalSystemId2, signalGroupId200);
		secondGroup.addSignalId(secondSignal.getId());
		signalsData.getSignalGroupsData().addSignalGroupData(secondGroup);
		SignalSystemControllerData controllerData = signalsData.getSignalControlData()
				.getSignalSystemControllerDataBySystemId()
				.get(signalSystemId2);
		SignalPlanData planData = controllerData.getSignalPlanData().get(signalPlanId2);
		SignalGroupSettingsData groupPlanSettings = signalsData.getSignalControlData()
				.getFactory()
				.createSignalGroupSettingsData(signalGroupId200);
		groupPlanSettings.setOnset(5);
		groupPlanSettings.setDropping(10);
		planData.addSignalGroupSettings(groupPlanSettings);
		// add conflict data
		IntersectionDirections conflictsNode2 = signalsData.getConflictingDirectionsData()
				.getFactory()
				.createConflictingDirectionsContainerForIntersection(signalSystemId2, nodeId2);
		Direction direction1 = signalsData.getConflictingDirectionsData()
				.getFactory()
				.createDirection(signalSystemId2, nodeId2, linkId1, linkId2,
						Id.create(linkId1 + "-" + linkId2, Direction.class));
		Direction direction2 = signalsData.getConflictingDirectionsData()
				.getFactory()
				.createDirection(signalSystemId2, nodeId2, linkId1, linkId6,
						Id.create(linkId1 + "-" + linkId6, Direction.class));
		direction1.addConflictingDirection(direction2.getId());
		direction2.addConflictingDirection(direction1.getId());
		conflictsNode2.addDirection(direction1);
		conflictsNode2.addDirection(direction2);
		signalsData.getConflictingDirectionsData()
				.addConflictingDirectionsForIntersection(signalSystemId2, nodeId2, conflictsNode2);

		return scenario;
	}

}
