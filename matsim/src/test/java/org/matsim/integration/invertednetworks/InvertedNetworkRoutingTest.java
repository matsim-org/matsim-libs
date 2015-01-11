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

import junit.framework.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.replanning.DefaultPlanStrategiesModule;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.lanes.data.v20.*;
import org.matsim.signalsystems.data.SignalsData;
import org.matsim.signalsystems.data.SignalsDataImpl;
import org.matsim.signalsystems.data.signalcontrol.v20.*;
import org.matsim.signalsystems.data.signalgroups.v20.SignalGroupData;
import org.matsim.signalsystems.data.signalgroups.v20.SignalGroupsData;
import org.matsim.signalsystems.data.signalgroups.v20.SignalGroupsDataFactory;
import org.matsim.signalsystems.data.signalsystems.v20.SignalData;
import org.matsim.signalsystems.data.signalsystems.v20.SignalSystemData;
import org.matsim.signalsystems.data.signalsystems.v20.SignalSystemsData;
import org.matsim.signalsystems.data.signalsystems.v20.SignalSystemsDataFactory;
import org.matsim.signalsystems.model.*;
import org.matsim.testcases.MatsimTestUtils;

import java.util.HashSet;
import java.util.Set;

public class InvertedNetworkRoutingTest {

	@Rule
	public MatsimTestUtils testUtils = new MatsimTestUtils();
	
	@Test
	public final void testLanesInvertedNetworkRouting() {
		Fixture f = new Fixture(false, true, false);
		f.scenario.getConfig().controler().setOutputDirectory(testUtils.getOutputDirectory());
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
		Fixture f = new Fixture(false, false, true);
		f.scenario.getConfig().controler().setOutputDirectory(testUtils.getOutputDirectory());
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
	public final void testSignalsInvertedNetworkRoutingIterations() {
		Fixture f = new Fixture(false, false, true);
		f.scenario.getConfig().controler().setOutputDirectory(testUtils.getOutputDirectory());
		f.scenario.getConfig().controler().setLastIteration(1);
		SignalsData signalsData = (SignalsData) f.scenario.getScenarioElement(SignalsData.ELEMENT_NAME);
		SignalPlanData signalPlan = signalsData.getSignalControlData().getSignalSystemControllerDataBySystemId().get(Id.create(2, SignalSystem.class)).getSignalPlanData().get(Id.create(1, SignalPlan.class));
		signalPlan.setCycleTime(500);
		signalPlan.getSignalGroupSettingsDataByGroupId().get(Id.create(2, SignalGroup.class)).setOnset(0);
		signalPlan.getSignalGroupSettingsDataByGroupId().get(Id.create(2, SignalGroup.class)).setDropping(5);
		SignalData sd = signalsData.getSignalSystemsData().getSignalSystemData().get(Id.create(2, SignalSystem.class)).getSignalData().get(Id.create(1, Signal.class));
		sd.addTurningMoveRestriction(Id.create(23, Link.class));
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
	public final void testModesInvertedNetworkRouting() {
		Fixture f = new Fixture(true, false, false);
		f.scenario.getConfig().controler().setOutputDirectory(testUtils.getOutputDirectory());
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
//	@Ignore
	public final void testModesNotInvertedNetworkRouting() {
		Fixture f = new Fixture(true, false, false);
		f.scenario.getConfig().controler().setOutputDirectory(testUtils.getOutputDirectory());
		f.scenario.getConfig().controler().setLinkToLinkRoutingEnabled(false);
		f.scenario.getConfig().travelTimeCalculator().setCalculateLinkToLinkTravelTimes(false);
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
			if (event.getLinkId().equals(Id.create("25", Link.class))) {
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
	 *				  |
	 *				(0)
	 * </pre>
	 *
	 * @author dgrether
	 */
	private static class Fixture {
		public final ScenarioImpl scenario;

		public Fixture(boolean doCreateModes, boolean doCreateLanes, boolean doCreateSignals) {
			Config config = ConfigUtils.createConfig();
			config.controler().setLastIteration(0);
			config.controler().setLinkToLinkRoutingEnabled(true);
			config.travelTimeCalculator().setCalculateLinkToLinkTravelTimes(true);
			config.controler().setMobsim("qsim");
			config.global().setNumberOfThreads(1);
			config.qsim().setRemoveStuckVehicles(false);
			config.qsim().setStuckTime(10000.0);
			config.qsim().setStartTime(0.0);
			config.qsim().setSimStarttimeInterpretation(QSimConfigGroup.ONLY_USE_STARTTIME);
			StrategySettings stratSets = new StrategySettings(Id.create(1, StrategySettings.class));
			stratSets.setStrategyName(DefaultPlanStrategiesModule.Names.ReRoute.toString());
			stratSets.setWeight(1.0);
			config.strategy().addStrategySettings(stratSets);
			config.planCalcScore().setTraveling_utils_hr(-1200.0);
			ActivityParams params = new ActivityParams("home");
			params.setTypicalDuration(24.0 * 3600.0);
			config.planCalcScore().addActivityParams(params);
			config.scenario().setUseLanes(doCreateLanes);
			config.scenario().setUseSignalSystems(doCreateSignals);
			
			this.scenario = (ScenarioImpl) ScenarioUtils.createScenario(config);
			createNetwork();
			if (doCreateLanes){
				config.scenario().setUseLanes(true);
				createLanes();
			}
			if (doCreateSignals){
				config.scenario().setUseSignalSystems(true);
				createSignals();
			}
			if (doCreateModes){
				createModes();
			}
			createPopulation();
		}

		private void createSignals() {
			SignalsData signalsData = new SignalsDataImpl( scenario.getConfig().signalSystems() );
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

		private void createLanes() {
			LaneDefinitions20 ld = new LaneDefinitions20Impl();
			scenario.addScenarioElement(LaneDefinitions20.ELEMENT_NAME, ld);
			LaneDefinitionsFactory20 f = ld.getFactory();
			LanesToLinkAssignment20 l2l = f.createLanesToLinkAssignment(Id.create(12, Link.class));
			ld.addLanesToLinkAssignment(l2l);
			LaneData20 l = f.createLane(Id.create(121, Lane.class));
			l.setStartsAtMeterFromLinkEnd(300);
			l.addToLaneId(Id.create(122, Lane.class));
			l2l.addLane(l);
			l = f.createLane(Id.create(122, Lane.class));
			l.setStartsAtMeterFromLinkEnd(150);
			l.addToLinkId(Id.create(25, Link.class));
			l2l.addLane(l);
		}

		private void createModes() {
			Network network = this.scenario.getNetwork();
			Set<String> ptOnly = new HashSet<String>();
			ptOnly.add(TransportMode.pt);
			Set<String> carPt = new HashSet<String>();
			carPt.add(TransportMode.car);
			carPt.add(TransportMode.pt);
			network.getLinks().get(Id.create(12, Link.class)).setAllowedModes(carPt);
			network.getLinks().get(Id.create(23, Link.class)).setAllowedModes(ptOnly);
			network.getLinks().get(Id.create(34, Link.class)).setAllowedModes(carPt);
			network.getLinks().get(Id.create(25, Link.class)).setAllowedModes(carPt);
			network.getLinks().get(Id.create(56, Link.class)).setAllowedModes(carPt);
			network.getLinks().get(Id.create(63, Link.class)).setAllowedModes(carPt);
		}

		private void createNetwork() {
			Network network = this.scenario.getNetwork();
			NetworkFactory f = network.getFactory();
			Node n;
			Link l;
			n = f.createNode(Id.create(0, Node.class), scenario.createCoord(0, -300));
			network.addNode(n);
			n = f.createNode(Id.create(1, Node.class), scenario.createCoord(0, 0));
			network.addNode(n);
			n = f.createNode(Id.create(2, Node.class), scenario.createCoord(0, 300));
			network.addNode(n);
			n = f.createNode(Id.create(3, Node.class), scenario.createCoord(0, 600));
			network.addNode(n);
			n = f.createNode(Id.create(4, Node.class), scenario.createCoord(0, 900));
			network.addNode(n);
			n = f.createNode(Id.create(5, Node.class), scenario.createCoord(0, 300));
			network.addNode(n);
			n = f.createNode(Id.create(6, Node.class), scenario.createCoord(0, 600));
			network.addNode(n);
			l = f.createLink(Id.create(01, Link.class), network.getNodes().get(Id.create(0, Node.class)), network.getNodes().get(Id.create(1, Node.class)));
			l.setLength(300.0);
			l.setFreespeed(10.0);
			l.setCapacity(3600.0);
			network.addLink(l);
			l = f.createLink(Id.create(12, Link.class), network.getNodes().get(Id.create(1, Node.class)), network.getNodes().get(Id.create(2, Node.class)));
			l.setLength(300.0);
			l.setFreespeed(10.0);
			l.setCapacity(3600.0);
			network.addLink(l);
			l = f.createLink(Id.create(23, Link.class), network.getNodes().get(Id.create(2, Node.class)), network.getNodes().get(Id.create(3, Node.class)));
			l.setLength(300.0);
			l.setFreespeed(20.0);
			l.setCapacity(3600.0);
			network.addLink(l);
			l = f.createLink(Id.create(34, Link.class), network.getNodes().get(Id.create(3, Node.class)), network.getNodes().get(Id.create(4, Node.class)));
			l.setLength(300.0);
			l.setFreespeed(10.0);
			l.setCapacity(3600.0);
			network.addLink(l);
			l = f.createLink(Id.create(25, Link.class), network.getNodes().get(Id.create(2, Node.class)), network.getNodes().get(Id.create(5, Node.class)));
			l.setLength(300.0);
			l.setFreespeed(10.0);
			l.setCapacity(3600.0);
			network.addLink(l);
			l = f.createLink(Id.create(56, Link.class), network.getNodes().get(Id.create(5, Node.class)), network.getNodes().get(Id.create(6, Node.class)));
			l.setLength(300.0);
			l.setFreespeed(10.0);
			l.setCapacity(3600.0);
			network.addLink(l);
			l = f.createLink(Id.create(63, Link.class), network.getNodes().get(Id.create(6, Node.class)), network.getNodes().get(Id.create(3, Node.class)));
			l.setLength(300.0);
			l.setFreespeed(10.0);
			l.setCapacity(3600.0);
			network.addLink(l);
		}
		
		private void createPopulation() {
			Population pop = this.scenario.getPopulation();
			PopulationFactory f = pop.getFactory();
			Person p = f.createPerson(Id.create(1, Person.class));
			pop.addPerson(p);
			Plan plan = f.createPlan();
			p.addPlan(plan);
			Activity act = f.createActivityFromLinkId("home", Id.create(1, Link.class));
			act.setEndTime(2000.0);
			plan.addActivity(act);
			Leg leg = f.createLeg(TransportMode.car);
			plan.addLeg(leg);
			act = f.createActivityFromLinkId("home", Id.create(34, Link.class));
			plan.addActivity(act);
		}


	}
}
