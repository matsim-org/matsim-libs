/* *********************************************************************** *
 * project: org.matsim.*
 * InvertedNetworkRoutingTest
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package org.matsim.integration.invertednetworks;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import junit.framework.Assert;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.lanes.data.v20.LaneDataV2;
import org.matsim.lanes.data.v20.LaneDefinitionsFactoryV2;
import org.matsim.lanes.data.v20.LaneDefinitionsV2;
import org.matsim.lanes.data.v20.LanesToLinkAssignmentV2;
import org.matsim.signalsystems.data.SignalsData;
import org.matsim.signalsystems.data.signalcontrol.v20.SignalControlData;
import org.matsim.signalsystems.data.signalcontrol.v20.SignalControlDataFactory;
import org.matsim.signalsystems.data.signalcontrol.v20.SignalGroupSettingsData;
import org.matsim.signalsystems.data.signalcontrol.v20.SignalPlanData;
import org.matsim.signalsystems.data.signalcontrol.v20.SignalSystemControllerData;
import org.matsim.signalsystems.data.signalgroups.v20.SignalGroupData;
import org.matsim.signalsystems.data.signalgroups.v20.SignalGroupsData;
import org.matsim.signalsystems.data.signalgroups.v20.SignalGroupsDataFactory;
import org.matsim.signalsystems.data.signalsystems.v20.SignalData;
import org.matsim.signalsystems.data.signalsystems.v20.SignalSystemData;
import org.matsim.signalsystems.data.signalsystems.v20.SignalSystemsData;
import org.matsim.signalsystems.data.signalsystems.v20.SignalSystemsDataFactory;
import org.matsim.signalsystems.model.DefaultPlanbasedSignalSystemController;
import org.matsim.testcases.MatsimTestUtils;

public class InvertedNetworkRoutingTest {

	@Rule
	public MatsimTestUtils testUtils = new MatsimTestUtils();
	
	@Test
	public final void testLanesInvertedNetworkRouting() {
		Config config = ConfigUtils.createConfig();
		config.controler().setOutputDirectory(testUtils.getOutputDirectory());
		config.scenario().setUseLanes(true);
		Fixture f = new Fixture(config, false);
		Controler c = new Controler(f.scenario);
		c.setDumpDataAtEnd(false);
		c.setCreateGraphs(false);
		final TestEventHandler testHandler = new TestEventHandler();
		c.addControlerListener(new StartupListener(){
			@Override
			public void notifyStartup(StartupEvent event) {
				event.getControler().getEvents().addHandler(testHandler);
			}
		});
		c.run();
		Assert.assertTrue("No traffic on link", testHandler.hadTrafficOnLink25);
	}

	@Test
	public final void testSignalsInvertedNetworkRouting() {
		Config config = ConfigUtils.createConfig();
		config.controler().setOutputDirectory(testUtils.getOutputDirectory());
		config.scenario().setUseSignalSystems(true);
		Fixture f = new Fixture(config, false);
		Controler c = new Controler(f.scenario);
		c.setDumpDataAtEnd(false);
		c.setCreateGraphs(false);
		final TestEventHandler testHandler = new TestEventHandler();
		c.addControlerListener(new StartupListener(){
			@Override
			public void notifyStartup(StartupEvent event) {
				event.getControler().getEvents().addHandler(testHandler);
			}
		});
		c.run();
		Assert.assertTrue("No traffic on link", testHandler.hadTrafficOnLink25);
	}

	@Test
	@Ignore
	public final void testModesInvertedNetworkRouting() {
		Config config = ConfigUtils.createConfig();
		config.controler().setOutputDirectory(testUtils.getOutputDirectory());
		Fixture f = new Fixture(config, true);
		Controler c = new Controler(f.scenario);
		c.setDumpDataAtEnd(false);
		c.setCreateGraphs(false);
		final TestEventHandler testHandler = new TestEventHandler();
		c.addControlerListener(new StartupListener(){
			@Override
			public void notifyStartup(StartupEvent event) {
				event.getControler().getEvents().addHandler(testHandler);
			}
		});
		c.run();
		Assert.assertTrue("No traffic on link", testHandler.hadTrafficOnLink25);
	}

	@Test
	@Ignore
	public final void testModesNotInvertedNetworkRouting() {
		Config config = ConfigUtils.createConfig();
		config.controler().setOutputDirectory(testUtils.getOutputDirectory());
		Fixture f = new Fixture(config, true);
		config.controler().setLinkToLinkRoutingEnabled(false);
		config.travelTimeCalculator().setCalculateLinkToLinkTravelTimes(false);
		Controler c = new Controler(f.scenario);
		c.setDumpDataAtEnd(false);
		c.setCreateGraphs(false);
		final TestEventHandler testHandler = new TestEventHandler();
		c.addControlerListener(new StartupListener(){
			@Override
			public void notifyStartup(StartupEvent event) {
				event.getControler().getEvents().addHandler(testHandler);
			}
		});
		c.run();
		Assert.assertTrue("No traffic on link", testHandler.hadTrafficOnLink25);
	}
	
	private static class TestEventHandler implements LinkEnterEventHandler {

		private boolean hadTrafficOnLink25 = false;
		
		@Override
		public void reset(int iteration) {
			this.hadTrafficOnLink25 = false;
		}

		@Override
		public void handleEvent(LinkEnterEvent event) {
			if (event.getLinkId().equals(new IdImpl("25"))) {
				this.hadTrafficOnLink25 = true;
			}
		}
	}
	
	
	/**
	 * Creates a simple test network, properties:
	 * <ul>
	 * 	<li>The link (2)->(3) may have only one mode pt, while the rest also allows car.</li>
	 * 	<li>A lane with turning move restrictions to Link (2)->(5) only can be attached to link (1)->(2).</li>
	 * 	<li>A signal with turning move restrictions to Link only can be attached to link (1)->(2).</li>
	 * </ul>
	 *
	 * <pre>
	 *				(4)
	 *    			^
	 *    			|
	 *				(3)<-(6)
	 *    			^			^
	 *    			|				|
	 *				(2)->(5)
	 *    			^
	 *    			|
	 *				(1)
	 * </pre>
	 *
	 * @author dgrether
	 */
	private static class Fixture {
		public final ScenarioImpl scenario;
		private Map<Integer, Id> ids = new HashMap<Integer, Id>();

		public Fixture(Config config, boolean doCreateModes) {
			config.controler().setLastIteration(0);
			config.controler().setLinkToLinkRoutingEnabled(true);
			config.travelTimeCalculator().setCalculateLinkToLinkTravelTimes(true);
			config.controler().setMobsim("qsim");
			config.global().setNumberOfThreads(1);
			config.addQSimConfigGroup(new QSimConfigGroup());
			ActivityParams params = new ActivityParams("home");
			params.setTypicalDuration(24.0 * 3600.0);
			config.planCalcScore().addActivityParams(params);
			this.scenario = (ScenarioImpl) ScenarioUtils.createScenario(config);
			createNetwork();
			if (config.scenario().isUseLanes()){
				createLanes();
			}
			if (config.scenario().isUseSignalSystems()){
				createSignals();
			}
			if (doCreateModes){
				createModes();
			}
			createPopulation();
		}

		private void createSignals() {
			SignalsData signalsData = this.scenario.getScenarioElement(SignalsData.class);
			SignalSystemsData ssd = signalsData.getSignalSystemsData();
			SignalSystemsDataFactory f = ssd.getFactory();
			SignalSystemData system = f.createSignalSystemData(getId(2));
			ssd.addSignalSystemData(system);
			SignalData signal = f.createSignalData(getId(1));
			signal.setLinkId(getId(12));
			signal.addTurningMoveRestriction(getId(25));
			system.addSignalData(signal);
			SignalGroupsData sgd = signalsData.getSignalGroupsData();
			SignalGroupsDataFactory fsg = sgd.getFactory();
			SignalGroupData sg = fsg.createSignalGroupData(getId(2), getId(2));
			sgd.addSignalGroupData(sg);
			SignalControlData scd = signalsData.getSignalControlData();
			SignalControlDataFactory fsc = scd.getFactory();
			SignalSystemControllerData controller = fsc.createSignalSystemControllerData(getId(2));
			controller.setControllerIdentifier(DefaultPlanbasedSignalSystemController.IDENTIFIER);
			scd.addSignalSystemControllerData(controller);
			SignalPlanData plan = fsc.createSignalPlanData(getId(1));
			plan.setStartTime(0.0);
			plan.setEndTime(23 * 3600.0);
			plan.setCycleTime(100);
			controller.addSignalPlanData(plan);
			SignalGroupSettingsData group = fsc.createSignalGroupSettingsData(getId(2));
			group.setOnset(0);
			group.setDropping(100);
			plan.addSignalGroupSettings(group);
		}

		private void createLanes() {
			LaneDefinitionsV2 ld = this.scenario.getScenarioElement(LaneDefinitionsV2.class);
			LaneDefinitionsFactoryV2 f = ld.getFactory();
			LanesToLinkAssignmentV2 l2l = f.createLanesToLinkAssignment(getId(12));
			ld.addLanesToLinkAssignment(l2l);
			LaneDataV2 l = f.createLane(getId(121));
			l.setStartsAtMeterFromLinkEnd(300);
			l.addToLaneId(getId(122));
			l2l.addLane(l);
			l = f.createLane(getId(122));
			l.setStartsAtMeterFromLinkEnd(150);
			l.addToLinkId(getId(25));
			l2l.addLane(l);
		}

		private void createModes() {
			Network network = this.scenario.getNetwork();
			Set<String> ptOnly = new HashSet<String>();
			ptOnly.add(TransportMode.pt);
			Set<String> carPt = new HashSet<String>();
			carPt.add(TransportMode.car);
			carPt.add(TransportMode.pt);
			network.getLinks().get(getId(12)).setAllowedModes(carPt);
			network.getLinks().get(getId(23)).setAllowedModes(ptOnly);
			network.getLinks().get(getId(34)).setAllowedModes(carPt);
			network.getLinks().get(getId(25)).setAllowedModes(carPt);
			network.getLinks().get(getId(56)).setAllowedModes(carPt);
			network.getLinks().get(getId(63)).setAllowedModes(carPt);
		}

		private void createNetwork() {
			Network network = this.scenario.getNetwork();
			NetworkFactory f = network.getFactory();
			Node n;
			Link l;
			n = f.createNode(getId(1), scenario.createCoord(0, 0));
			network.addNode(n);
			n = f.createNode(getId(2), scenario.createCoord(0, 300));
			network.addNode(n);
			n = f.createNode(getId(3), scenario.createCoord(0, 600));
			network.addNode(n);
			n = f.createNode(getId(4), scenario.createCoord(0, 900));
			network.addNode(n);
			n = f.createNode(getId(5), scenario.createCoord(0, 300));
			network.addNode(n);
			n = f.createNode(getId(6), scenario.createCoord(0, 600));
			network.addNode(n);
			l = f.createLink(getId(12), network.getNodes().get(getId(1)), network.getNodes().get(getId(2)));
			l.setLength(300.0);
			l.setFreespeed(10.0);
			l.setCapacity(3600.0);
			network.addLink(l);
			l = f.createLink(getId(23), network.getNodes().get(getId(2)), network.getNodes().get(getId(3)));
			l.setLength(300.0);
			l.setFreespeed(10.0);
			l.setCapacity(3600.0);
			network.addLink(l);
			l = f.createLink(getId(34), network.getNodes().get(getId(3)), network.getNodes().get(getId(4)));
			l.setLength(300.0);
			l.setFreespeed(10.0);
			l.setCapacity(3600.0);
			network.addLink(l);
			l = f.createLink(getId(25), network.getNodes().get(getId(2)), network.getNodes().get(getId(5)));
			l.setLength(300.0);
			l.setFreespeed(10.0);
			l.setCapacity(3600.0);
			network.addLink(l);
			l = f.createLink(getId(56), network.getNodes().get(getId(5)), network.getNodes().get(getId(6)));
			l.setLength(300.0);
			l.setFreespeed(10.0);
			l.setCapacity(3600.0);
			network.addLink(l);
			l = f.createLink(getId(63), network.getNodes().get(getId(6)), network.getNodes().get(getId(3)));
			l.setLength(300.0);
			l.setFreespeed(10.0);
			l.setCapacity(3600.0);
			network.addLink(l);
		}
		
		private void createPopulation() {
			Population pop = this.scenario.getPopulation();
			PopulationFactory f = pop.getFactory();
			Person p = f.createPerson(getId(1));
			pop.addPerson(p);
			Plan plan = f.createPlan();
			p.addPlan(plan);
			Activity act = f.createActivityFromLinkId("home", getId(12));
			act.setEndTime(0.0);
			plan.addActivity(act);
			Leg leg = f.createLeg(TransportMode.car);
			plan.addLeg(leg);
			act = f.createActivityFromLinkId("home", getId(34));
			plan.addActivity(act);
		}


		public Id getId(int i){
			if (this.ids.containsKey(i)){
				return this.ids.get(i);
			}
			Id id = this.scenario.createId(Integer.toString(i));			
			this.ids.put(i, id);
			return id;
		}


	}
}
