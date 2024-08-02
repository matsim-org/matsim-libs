/* *********************************************************************** *
 * project: org.matsim.*
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

/**
 *
 */
package playground.vsp.congestion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.inject.Inject;
import jakarta.inject.Provider;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.RoutingConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.PrepareForSimUtils;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.ControlerListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimBuilder;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.LinkNetworkRouteFactory;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.costcalculators.RandomizingTimeDistanceTravelDisutilityFactory;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;

import playground.vsp.congestion.controler.MarginalCongestionPricingContolerListener;
import playground.vsp.congestion.events.CongestionEvent;
import playground.vsp.congestion.handlers.CongestionEventHandler;
import playground.vsp.congestion.handlers.CongestionHandlerImplV10;
import playground.vsp.congestion.handlers.CongestionHandlerImplV3;
import playground.vsp.congestion.handlers.CongestionHandlerImplV8;
import playground.vsp.congestion.handlers.CongestionHandlerImplV9;
import playground.vsp.congestion.handlers.TollHandler;
import playground.vsp.congestion.routing.CongestionTollTimeDistanceTravelDisutilityFactory;

/**
 *
 * Simple scenario:
 * - The bottleneck on link 3 (1 car every 10 seconds) is activated by the first agent.
 * - Then, the storage capacity on link 3 is reached.
 * - Finally, the last agent is delayed.
 *
 * @author ikaddoura , lkroeger
 *
 */

public class MarginalCongestionHandlerFlowSpillbackQueueQsimTest {

	@RegisterExtension
	private MatsimTestUtils testUtils = new MatsimTestUtils();

	private EventsManager events;

	private Id<Person> testAgent1 = Id.create("testAgent1", Person.class);
	private Id<Person> testAgent2 = Id.create("testAgent2", Person.class);
	private Id<Person> testAgent3 = Id.create("testAgent3", Person.class);

	private Id<Link> linkId1 = Id.create("link1", Link.class);
	private Id<Link> linkId2 = Id.create("link2", Link.class);
	private Id<Link> linkId3 = Id.create("link3", Link.class);
	private Id<Link> linkId4 = Id.create("link4", Link.class);
	private Id<Link> linkId5 = Id.create("link5", Link.class);

	private Id<Link> linkId2_ = Id.create("linkId2_", Link.class);

	double avgValue1 = 0.0;
	double avgValue2 = 0.0;
	double avgOldValue1 = 0.0;
	double avgOldValue2 = 0.0;
	double avgValue3 = 0.0;
	double avgValue4 = 0.0;
	double avgOldValue3 = 0.0;
	double avgOldValue4 = 0.0;

	//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

	/**
	 * V3
	 *
	 */
	@Disabled("Temporarily ignoring")//TODO for Amit
	@Test
	final void testFlowAndStorageCongestion_3agents(){

		Scenario sc = loadScenario1();
		sc.getConfig().routing().setNetworkRouteConsistencyCheck(RoutingConfigGroup.NetworkRouteConsistencyCheck.disable);
		setPopulation1(sc);

		final List<CongestionEvent> congestionEvents = new ArrayList<CongestionEvent>();

		events.addHandler( new CongestionEventHandler() {

			@Override
			public void reset(int iteration) {
			}

			@Override
			public void handleEvent(CongestionEvent event) {
				congestionEvents.add(event);
			}
		});

		events.addHandler(new CongestionHandlerImplV3(events, (MutableScenario) sc));

		QSim sim = createQSim(sc, events);
		sim.run();

		for (CongestionEvent event : congestionEvents) {

			System.out.println(event.toString());

			if (event.getCausingAgentId().toString().equals(this.testAgent1.toString()) && event.getAffectedAgentId().toString().equals(this.testAgent2.toString())) {
				Assertions.assertEquals(10.0, event.getDelay(), MatsimTestUtils.EPSILON, "wrong delay.");
			} else if ((event.getCausingAgentId().toString().equals(this.testAgent2.toString())) && (event.getAffectedAgentId().toString().equals(this.testAgent3.toString())) && (event.getTime() == 116.0)) {
				Assertions.assertEquals(10.0, event.getDelay(), MatsimTestUtils.EPSILON, "wrong delay.");
			} else if ((event.getCausingAgentId().toString().equals(this.testAgent1.toString())) && (event.getAffectedAgentId().toString().equals(this.testAgent3.toString())) && (event.getTime() == 126.0)) {
				Assertions.assertEquals(9.0, event.getDelay(), MatsimTestUtils.EPSILON, "wrong delay.");
			}

		}

	}

	/**
	 * V9
	 *
	 */
	@Test
	final void testFlowAndStorageCongestion_3agents_V9() {

		Scenario sc = loadScenario1();
		sc.getConfig().routing().setNetworkRouteConsistencyCheck(RoutingConfigGroup.NetworkRouteConsistencyCheck.disable);
		setPopulation1(sc);

		final List<CongestionEvent> congestionEvents = new ArrayList<CongestionEvent>();

		events.addHandler(new CongestionEventHandler() {

			@Override
			public void reset(int iteration) {
			}

			@Override
			public void handleEvent(CongestionEvent event) {
				congestionEvents.add(event);
			}
		});

		events.addHandler(new CongestionHandlerImplV9(events, (MutableScenario) sc));

		PrepareForSimUtils.createDefaultPrepareForSim(sc).run();
		QSim sim = createQSim(sc, events);
		sim.run();

		double totalDelay = 0.;

		for (CongestionEvent event : congestionEvents) {

			System.out.println(event.toString());
			totalDelay += event.getDelay();
		}
		Assertions.assertEquals(50.0, totalDelay, MatsimTestUtils.EPSILON, "wrong total delay.");

	}

	/**
	 * V8 (the same as V9 but without charging for spill-back delays)
	 *
	 */
	@Test
	final void testFlowAndStorageCongestion_3agents_V8() {

		Scenario sc = loadScenario1();
		sc.getConfig().routing().setNetworkRouteConsistencyCheck(RoutingConfigGroup.NetworkRouteConsistencyCheck.disable);
		setPopulation1(sc);

		final List<CongestionEvent> congestionEvents = new ArrayList<CongestionEvent>();

		events.addHandler(new CongestionEventHandler() {

			@Override
			public void reset(int iteration) {
			}

			@Override
			public void handleEvent(CongestionEvent event) {
				congestionEvents.add(event);
			}
		});

		events.addHandler(new CongestionHandlerImplV8(events, (MutableScenario) sc));

		PrepareForSimUtils.createDefaultPrepareForSim(sc).run();
		QSim sim = createQSim(sc, events);
		sim.run();

		double totalDelay = 0.;

		for (CongestionEvent event : congestionEvents) {

			System.out.println(event.toString());
			totalDelay += event.getDelay();
		}
		Assertions.assertEquals(30.0, totalDelay, MatsimTestUtils.EPSILON, "wrong total delay.");

	}

	/**
	 * V10
	 *
	 */
	@Disabled("Temporarily ignoring")//TODO for Amit
	@Test
	final void testFlowAndStorageCongestion_3agents_V10() {

		Scenario sc = loadScenario1();
		sc.getConfig().routing().setNetworkRouteConsistencyCheck(RoutingConfigGroup.NetworkRouteConsistencyCheck.disable);
		setPopulation1(sc);

		final List<CongestionEvent> congestionEvents = new ArrayList<CongestionEvent>();

		events.addHandler(new CongestionEventHandler() {

			@Override
			public void reset(int iteration) {
			}

			@Override
			public void handleEvent(CongestionEvent event) {
				congestionEvents.add(event);
			}
		});

		events.addHandler(new CongestionHandlerImplV10(events, (MutableScenario) sc));

		QSim sim = createQSim(sc, events);
		sim.run();

		double totalDelay = 0.;

		for (CongestionEvent event : congestionEvents) {

			System.out.println(event.toString());
			totalDelay += event.getDelay();
		}
		Assertions.assertEquals(32.0, totalDelay, MatsimTestUtils.EPSILON, "wrong total delay.");

	}

	// testing the routing
	// link 2 has a capacity of 60 vehicles / hour (= 1 vehicle / 60 seconds)
	// 3 agents enter the relevant link at 8:14 (in the time-bin 8:00-8:15), the second and third agent  leave the link after 8:15:00 (hence in the next time-bin 8:15-8:30)
	// some minutes later 3 agents enter and leave the relevant link in the same time-bin (8:15-8:30)
	// in both cases delays of about 1 + 2 minutes are caused
	// in both cases the router should consider the time-bin of the LinkEnterEvent (not that one of the Link-LeaveEvent)
	// this applies for the calculation of the average toll-costs, too.
	//
	// Moreover the "toll" and "tollOldValue" are compared here for the last iteration
	// Due to no route- and mode-alternatives and no stucking agents
	// in both iterations the "toll" and the "tollOldValue" should be the same
	//
	// 3 iterations are necessary to check the equality of the "toll" and the "tollOldValue"
	@Disabled("Temporarily ignoring")//TODO for Amit
	@Test
	final void testRouting(){

		String configFile = testUtils.getPackageInputDirectory()+"MarginalCongestionHandlerV3QsimTest/configTestRouting.xml";

		Config config = ConfigUtils.loadConfig( configFile ) ;

		config.routing().setAccessEgressType(RoutingConfigGroup.AccessEgressType.none);
		config.routing().setNetworkRouteConsistencyCheck(RoutingConfigGroup.NetworkRouteConsistencyCheck.disable);

		final Scenario scenario = ScenarioUtils.loadScenario( config );
		Controler controler = new Controler( scenario );

		final TollHandler tollHandler = new TollHandler(controler.getScenario());

		final CongestionTollTimeDistanceTravelDisutilityFactory tollDisutilityCalculatorFactory = new CongestionTollTimeDistanceTravelDisutilityFactory(
				new RandomizingTimeDistanceTravelDisutilityFactory(TransportMode.car, config),
				tollHandler, controler.getConfig().scoring());
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addControlerListenerBinding().toProvider(new Provider<ControlerListener>() {
					@Inject Scenario scenario;
					@Inject EventsManager eventsManager;
					@Override
					public ControlerListener get() {
						return new MarginalCongestionPricingContolerListener(scenario, tollHandler, new CongestionHandlerImplV3(eventsManager, scenario));
					}
				});
				bindCarTravelDisutilityFactory().toInstance(tollDisutilityCalculatorFactory);
			}
		});

		final String timeBin1 = "08:00-08:15";
		final String timeBin2 = "08:15-08:30";

		final Map<String,Integer> enterCounter = new HashMap<String,Integer>();
		final Map<String,Integer> leaveCounter = new HashMap<String,Integer>();

		enterCounter.put(timeBin1,0);
		enterCounter.put(timeBin2,0);
		leaveCounter.put(timeBin1,0);
		leaveCounter.put(timeBin2,0);

		controler.getEvents().addHandler( new LinkEnterEventHandler() {

			@Override
			public void reset(int iteration) {
				enterCounter.put(timeBin1,0);
				enterCounter.put(timeBin2,0);
				leaveCounter.put(timeBin1,0);
				leaveCounter.put(timeBin2,0);
			}

			@Override
			public void handleEvent(LinkEnterEvent event) {
				if(event.getLinkId().toString().equals("linkId2_")){
					if(event.getTime()>=28800 && event.getTime()<=29700){
						enterCounter.put(timeBin1, (enterCounter.get(timeBin1)+1));
					}else if(event.getTime()>=29700 && event.getTime()<=30600){
						enterCounter.put(timeBin2, (enterCounter.get(timeBin2)+1));
					}
				}else{}
			}
		});

		controler.getEvents().addHandler( new LinkLeaveEventHandler() {

			@Override
			public void reset(int iteration) {
				enterCounter.put(timeBin1,0);
				enterCounter.put(timeBin2,0);
				leaveCounter.put(timeBin1,0);
				leaveCounter.put(timeBin2,0);
			}

			@Override
			public void handleEvent(LinkLeaveEvent event) {
				if(event.getLinkId().toString().equals("linkId2_")){
					if(event.getTime()>=28800 && event.getTime()<=29700){
						leaveCounter.put(timeBin1, (leaveCounter.get(timeBin1)+1));
					}else if(event.getTime()>=29700 && event.getTime()<=30600){
						leaveCounter.put(timeBin2, (leaveCounter.get(timeBin2)+1));
					}
				}else{}
			}
		});

		controler.addControlerListener(new IterationStartsListener() {

			@Override
			public void notifyIterationStarts(IterationStartsEvent event) {
				// last but one iteration
				if(((event.getServices().getConfig().controller().getLastIteration())-(event.getIteration()))==1){
					avgValue1 = tollHandler.getAvgToll(linkId2_, 28800);
					avgValue2 = tollHandler.getAvgToll(linkId2_, 29700);
					avgOldValue1 = tollHandler.getAvgTollOldValue(linkId2_, 28800);
					avgOldValue2 = tollHandler.getAvgTollOldValue(linkId2_, 28800);
				}
				// last iteration
				else if(((event.getServices().getConfig().controller().getLastIteration())-(event.getIteration()))==0){
					avgValue3 = tollHandler.getAvgToll(linkId2_, 28800);
					avgValue4 = tollHandler.getAvgToll(linkId2_, 29700);
					avgOldValue3 = tollHandler.getAvgTollOldValue(linkId2_, 28800);
					avgOldValue4 = tollHandler.getAvgTollOldValue(linkId2_, 28800);
				}
			}

		});

		controler.getConfig().controller().setOverwriteFileSetting(
				OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles );
		controler.run();

		{ // controlling and showing the conditions of the test:
			Assertions.assertEquals(3.0, enterCounter.get(timeBin1), MatsimTestUtils.EPSILON, "entering agents in timeBin1.");
			Assertions.assertEquals(3.0, enterCounter.get(timeBin2), MatsimTestUtils.EPSILON, "entering agents in timeBin2.");
			Assertions.assertEquals(1.0, leaveCounter.get(timeBin1), MatsimTestUtils.EPSILON, "leaving agents in timeBin1.");
			Assertions.assertEquals(5.0, leaveCounter.get(timeBin2), MatsimTestUtils.EPSILON, "leaving agents in timeBin1.");
		}

		// for both time-bins ("28800-29700" and "29700-30600")
		// the expectedTollDisutility should be the same
		// because in both time-bins 3 agents enter the link,
		// the congestion effects of each 3 cars are the same,
		// the fact that 2 of the first 3 cars leave the link in the next time-bin should be irrelevant

		Assertions.assertEquals(avgValue1, avgValue2, MatsimTestUtils.EPSILON, "avgValue1 == avgValue2");
		Assertions.assertEquals(avgValue3, avgValue4, MatsimTestUtils.EPSILON, "avgValue3 == avgValue3");
		Assertions.assertEquals(avgValue1, avgValue3, MatsimTestUtils.EPSILON, "avgValue1 == avgValue3");

		Assertions.assertEquals(avgOldValue1, avgOldValue2, MatsimTestUtils.EPSILON, "avgOldValue1 == avgOldValue2");
		Assertions.assertEquals(avgOldValue3, avgOldValue4, MatsimTestUtils.EPSILON, "avgOldValue3 == avgOldValue3");
		Assertions.assertEquals(avgOldValue1, avgOldValue3, MatsimTestUtils.EPSILON, "avgOldValue1 == avgOldValue3");

		Assertions.assertEquals(avgValue1, avgOldValue1, MatsimTestUtils.EPSILON, "avgValue1 == avgOldValue1");

	 }

	// setInsertingWaitingVehiclesBeforeDrivingVehicles = false
	@Disabled("Temporarily ignoring")//TODO for Amit
	@Test
	final void testInsertingWaitingVehicles_01(){

		Scenario sc = loadScenario4();
		sc.getConfig().routing().setNetworkRouteConsistencyCheck(RoutingConfigGroup.NetworkRouteConsistencyCheck.disable);
		setPopulation4(sc);

		final List<CongestionEvent> congestionEvents = new ArrayList<CongestionEvent>();

		events.addHandler( new CongestionEventHandler() {

			@Override
			public void reset(int iteration) {
			}

			@Override
			public void handleEvent(CongestionEvent event) {
				congestionEvents.add(event);
			}
		});

		events.addHandler( new LinkLeaveEventHandler() {

			@Override
			public void reset(int iteration) {
			}

			@Override
			public void handleEvent(LinkLeaveEvent event) {
				System.out.println(event.toString());
			}
		});

		events.addHandler(new CongestionHandlerImplV3(events, (MutableScenario) sc));

		QSim sim = createQSim(sc, events);
		sim.run();

		for (CongestionEvent event : congestionEvents) {

			System.out.println(event.toString());

		}

		Assertions.assertEquals(congestionEvents.size(), 3, MatsimTestUtils.EPSILON, "numberOfCongestionEvents");

		for(CongestionEvent mce : congestionEvents){
			if((mce.getCausingAgentId().equals(testAgent1))&&(mce.getAffectedAgentId().equals(testAgent2))){
				Assertions.assertEquals(10., mce.getDelay(), MatsimTestUtils.EPSILON, "delay");
			}else if((mce.getCausingAgentId().equals(testAgent2))&&(mce.getAffectedAgentId().equals(testAgent3))){
				Assertions.assertEquals(10., mce.getDelay(), MatsimTestUtils.EPSILON, "delay");
			}else if((mce.getCausingAgentId().equals(testAgent1))&&(mce.getAffectedAgentId().equals(testAgent3))){
				Assertions.assertEquals(4., mce.getDelay(), MatsimTestUtils.EPSILON, "delay");
			}
		}
	}

	// setInsertingWaitingVehiclesBeforeDrivingVehicles = true
	// to compare
	@Disabled("Temporarily ignoring")//TODO for Amit
	@Test
	final void testInsertingWaitingVehicles_02(){

		Scenario sc = loadScenario5();
		sc.getConfig().routing().setNetworkRouteConsistencyCheck(RoutingConfigGroup.NetworkRouteConsistencyCheck.disable);
		setPopulation5(sc);

		final List<CongestionEvent> congestionEvents = new ArrayList<CongestionEvent>();

		events.addHandler( new CongestionEventHandler() {

			@Override
			public void reset(int iteration) {
			}

			@Override
			public void handleEvent(CongestionEvent event) {
				congestionEvents.add(event);
			}
		});

		events.addHandler( new LinkLeaveEventHandler() {

			@Override
			public void reset(int iteration) {
			}

			@Override
			public void handleEvent(LinkLeaveEvent event) {
				System.out.println(event.toString());
			}
		});

		events.addHandler(new CongestionHandlerImplV3(events, (MutableScenario) sc));

		QSim sim = createQSim(sc, events);
		sim.run();

		for (CongestionEvent event : congestionEvents) {

			System.out.println(event.toString());

		}

		Assertions.assertEquals(congestionEvents.size(), 3, MatsimTestUtils.EPSILON, "numberOfCongestionEvents");

		for(CongestionEvent mce : congestionEvents){
			if((mce.getCausingAgentId().equals(testAgent1))&&(mce.getAffectedAgentId().equals(testAgent3))){
				Assertions.assertEquals(4., mce.getDelay(), MatsimTestUtils.EPSILON, "delay");
			}else if((mce.getCausingAgentId().equals(testAgent3))&&(mce.getAffectedAgentId().equals(testAgent2))){
				Assertions.assertEquals(10., mce.getDelay(), MatsimTestUtils.EPSILON, "delay");
			}else if((mce.getCausingAgentId().equals(testAgent1))&&(mce.getAffectedAgentId().equals(testAgent2))){
				Assertions.assertEquals(10., mce.getDelay(), MatsimTestUtils.EPSILON, "delay");
			}
		}

	}

	// setInsertingWaitingVehiclesBeforeDrivingVehicles = false
	// agent 2 is already on link 2 when agent 3 ends his activity,
	// therefore agent 3 has to wait until agent 2 has left the link
	@Disabled("Temporarily ignoring")//TODO for Amit
	@Test
	final void testInsertingWaitingVehicles_03(){

		Scenario sc = loadScenario4();
		sc.getConfig().routing().setNetworkRouteConsistencyCheck(RoutingConfigGroup.NetworkRouteConsistencyCheck.disable);
		setPopulation6(sc);

		final List<CongestionEvent> congestionEvents = new ArrayList<CongestionEvent>();

		events.addHandler( new CongestionEventHandler() {

			@Override
			public void reset(int iteration) {
			}

			@Override
			public void handleEvent(CongestionEvent event) {
				congestionEvents.add(event);
			}
		});

		events.addHandler( new LinkLeaveEventHandler() {

			@Override
			public void reset(int iteration) {
			}

			@Override
			public void handleEvent(LinkLeaveEvent event) {
				System.out.println(event.toString());
			}
		});

		events.addHandler(new CongestionHandlerImplV3(events, (MutableScenario) sc));

		QSim sim = createQSim(sc, events);
		sim.run();

		for (CongestionEvent event : congestionEvents) {

			System.out.println(event.toString());

		}

		Assertions.assertEquals(congestionEvents.size(), 3, MatsimTestUtils.EPSILON, "numberOfCongestionEvents");

		for(CongestionEvent mce : congestionEvents){
			if((mce.getCausingAgentId().equals(testAgent1))&&(mce.getAffectedAgentId().equals(testAgent2))){
				Assertions.assertEquals(5., mce.getDelay(), MatsimTestUtils.EPSILON, "delay");
			}else if((mce.getCausingAgentId().equals(testAgent2))&&(mce.getAffectedAgentId().equals(testAgent3))){
				Assertions.assertEquals(10., mce.getDelay(), MatsimTestUtils.EPSILON, "delay");
			}else if((mce.getCausingAgentId().equals(testAgent1))&&(mce.getAffectedAgentId().equals(testAgent3))){
				Assertions.assertEquals(8., mce.getDelay(), MatsimTestUtils.EPSILON, "delay");
			}
		}

	}

	@Disabled("Temporarily ignoring")//TODO for Amit
	@Test
	final void testStuckTimePeriod(){

		Scenario sc = loadScenario1b();
		setPopulation1(sc);

		final List<CongestionEvent> congestionEvents = new ArrayList<CongestionEvent>();

		events.addHandler( new CongestionEventHandler() {

			@Override
			public void reset(int iteration) {
			}

			@Override
			public void handleEvent(CongestionEvent event) {
				congestionEvents.add(event);
			}
		});

		events.addHandler( new LinkLeaveEventHandler() {

			@Override
			public void reset(int iteration) {
			}

			@Override
			public void handleEvent(LinkLeaveEvent event) {
				System.out.println(event.toString());
			}
		});

		events.addHandler( new LinkEnterEventHandler() {

			@Override
			public void reset(int iteration) {
			}

			@Override
			public void handleEvent(LinkEnterEvent event) {
				System.out.println(event.toString());
			}
		});

		events.addHandler(new CongestionHandlerImplV3(events, (MutableScenario) sc));

		QSim sim = createQSim(sc, events);
		sim.run();

		for (CongestionEvent event : congestionEvents) {

			System.out.println(event.toString());

			if (event.getCausingAgentId().toString().equals(this.testAgent1.toString()) && event.getAffectedAgentId().toString().equals(this.testAgent2.toString())) {
				Assertions.assertEquals(10.0, event.getDelay(), MatsimTestUtils.EPSILON, "wrong delay.");
			} else if ((event.getCausingAgentId().toString().equals(this.testAgent2.toString())) && (event.getAffectedAgentId().toString().equals(this.testAgent3.toString())) && (event.getTime() == 116.0)) {
				Assertions.assertEquals(10.0, event.getDelay(), MatsimTestUtils.EPSILON, "wrong delay.");
			} else if ((event.getCausingAgentId().toString().equals(this.testAgent1.toString())) && (event.getAffectedAgentId().toString().equals(this.testAgent3.toString())) && (event.getTime() == 126.0)) {
				Assertions.assertEquals(9.0, event.getDelay(), MatsimTestUtils.EPSILON, "wrong delay.");
			}
		}

	}

	// ################################################################################################################################

	private void setPopulation1(Scenario scenario) {

		Population population = scenario.getPopulation();
        PopulationFactory popFactory = (PopulationFactory) scenario.getPopulation().getFactory();
		LinkNetworkRouteFactory routeFactory = new LinkNetworkRouteFactory();

		Activity workActLink5 = popFactory.createActivityFromLinkId("work", linkId5);

		// leg: 1,2,3,4,5
		Leg leg_1_5 = popFactory.createLeg("car");
		List<Id<Link>> linkIds234 = new ArrayList<Id<Link>>();
		linkIds234.add(linkId2);
		linkIds234.add(linkId3);
		linkIds234.add(linkId4);
		NetworkRoute route1_5 = (NetworkRoute) routeFactory.createRoute(linkId1, linkId5);
		route1_5.setLinkIds(linkId1, linkIds234, linkId5);
		leg_1_5.setRoute(route1_5);

		// ################################################################
		// first agent activating the flow capacity on link3
		Person person1 = popFactory.createPerson(testAgent1);
		Plan plan1 = popFactory.createPlan();
		Activity homeActLink1_1 = popFactory.createActivityFromLinkId("home", linkId1);
		homeActLink1_1.setEndTime(100);
		plan1.addActivity(homeActLink1_1);
		plan1.addLeg(leg_1_5);
		plan1.addActivity(workActLink5);
		person1.addPlan(plan1);
		population.addPerson(person1);

		// ################################################################
		// second agent delayed on link3; blocking link3
		Person person2 = popFactory.createPerson(testAgent2);
		Plan plan2 = popFactory.createPlan();
		Activity homeActLink1_2 = popFactory.createActivityFromLinkId("home", linkId1);
		homeActLink1_2.setEndTime(101);
		plan2.addActivity(homeActLink1_2);
		{
			Leg leg = popFactory.createLeg(leg_1_5.getMode());
			PopulationUtils.copyFromTo(leg_1_5, leg);
			plan2.addLeg(leg);
		}
		plan2.addActivity(workActLink5);
		person2.addPlan(plan2);
		population.addPerson(person2);

		// ################################################################
		// third agent delayed on link2 (spill-back)
		Person person3 = popFactory.createPerson(testAgent3);
		Plan plan3 = popFactory.createPlan();
		Activity homeActLink1_3 = popFactory.createActivityFromLinkId("home", linkId1);
		homeActLink1_3.setEndTime(102);
		plan3.addActivity(homeActLink1_3);
		{
			Leg leg = popFactory.createLeg(leg_1_5.getMode());
			PopulationUtils.copyFromTo(leg_1_5, leg);
			plan3.addLeg(leg);
		}
		plan3.addActivity(workActLink5);
		person3.addPlan(plan3);
		population.addPerson(person3);

	}

	private void setPopulation4(Scenario scenario) {

		Population population = scenario.getPopulation();
        PopulationFactory popFactory = (PopulationFactory) scenario.getPopulation().getFactory();
		LinkNetworkRouteFactory routeFactory = new LinkNetworkRouteFactory();

		Activity workActLink5 = popFactory.createActivityFromLinkId("work", linkId5);

		// leg: 1,2,3,4,5
		Leg leg_1_5 = popFactory.createLeg("car");
		List<Id<Link>> linkIds234 = new ArrayList<Id<Link>>();
		linkIds234.add(linkId2);
		linkIds234.add(linkId3);
		linkIds234.add(linkId4);
		NetworkRoute route1_5 = (NetworkRoute) routeFactory.createRoute(linkId1, linkId5);
		route1_5.setLinkIds(linkId1, linkIds234, linkId5);
		leg_1_5.setRoute(route1_5);

		Person person1 = popFactory.createPerson(testAgent1);
		Plan plan1 = popFactory.createPlan();
		Activity homeActLink1_1 = popFactory.createActivityFromLinkId("home", linkId1);
		homeActLink1_1.setEndTime(100);
		plan1.addActivity(homeActLink1_1);
		plan1.addLeg(leg_1_5);
		plan1.addActivity(workActLink5);
		person1.addPlan(plan1);
		population.addPerson(person1);

		Person person2 = popFactory.createPerson(testAgent2);
		Plan plan2 = popFactory.createPlan();
		Activity homeActLink1_2 = popFactory.createActivityFromLinkId("home", linkId1);
		homeActLink1_2.setEndTime(101);
		plan2.addActivity(homeActLink1_2);
		{
			Leg leg = popFactory.createLeg(leg_1_5.getMode());
			PopulationUtils.copyFromTo(leg_1_5, leg);
			plan2.addLeg(leg);
		}
		plan2.addActivity(workActLink5);
		person2.addPlan(plan2);
		population.addPerson(person2);

		// leg: 1,2,3,4,5
		Leg leg_2_5 = popFactory.createLeg("car");
		List<Id<Link>> linkIds34 = new ArrayList<Id<Link>>();
		linkIds34.add(linkId3);
		linkIds34.add(linkId4);
		NetworkRoute route2_5 = (NetworkRoute) routeFactory.createRoute(linkId2, linkId5);
		route2_5.setLinkIds(linkId2, linkIds34, linkId5);
		leg_2_5.setRoute(route2_5);

		Person person3 = popFactory.createPerson(testAgent3);
		Plan plan3 = popFactory.createPlan();
		Activity homeActLink2_3 = popFactory.createActivityFromLinkId("home", linkId2);
		homeActLink2_3.setEndTime(158);
		plan3.addActivity(homeActLink2_3);
		plan3.addLeg(leg_2_5);
		plan3.addActivity(workActLink5);
		person3.addPlan(plan3);
		population.addPerson(person3);

	}

	private void setPopulation5(Scenario scenario) {

		Population population = scenario.getPopulation();
        PopulationFactory popFactory = (PopulationFactory) scenario.getPopulation().getFactory();
		LinkNetworkRouteFactory routeFactory = new LinkNetworkRouteFactory();

		Activity workActLink5 = popFactory.createActivityFromLinkId("work", linkId5);

		// leg: 1,2,3,4,5
		Leg leg_1_5 = popFactory.createLeg("car");
		List<Id<Link>> linkIds234 = new ArrayList<Id<Link>>();
		linkIds234.add(linkId2);
		linkIds234.add(linkId3);
		linkIds234.add(linkId4);
		NetworkRoute route1_5 = (NetworkRoute) routeFactory.createRoute(linkId1, linkId5);
		route1_5.setLinkIds(linkId1, linkIds234, linkId5);
		leg_1_5.setRoute(route1_5);

		Person person1 = popFactory.createPerson(testAgent1);
		Plan plan1 = popFactory.createPlan();
		Activity homeActLink1_1 = popFactory.createActivityFromLinkId("home", linkId1);
		homeActLink1_1.setEndTime(100);
		plan1.addActivity(homeActLink1_1);
		plan1.addLeg(leg_1_5);
		plan1.addActivity(workActLink5);
		person1.addPlan(plan1);
		population.addPerson(person1);

		Person person2 = popFactory.createPerson(testAgent2);
		Plan plan2 = popFactory.createPlan();
		Activity homeActLink1_2 = popFactory.createActivityFromLinkId("home", linkId1);
		homeActLink1_2.setEndTime(101);
		plan2.addActivity(homeActLink1_2);
		{
			Leg leg = popFactory.createLeg(leg_1_5.getMode());
			PopulationUtils.copyFromTo(leg_1_5, leg);
			plan2.addLeg(leg);
		}
		plan2.addActivity(workActLink5);
		person2.addPlan(plan2);
		population.addPerson(person2);

		// leg: 1,2,3,4,5
		Leg leg_2_5 = popFactory.createLeg("car");
		List<Id<Link>> linkIds34 = new ArrayList<Id<Link>>();
		linkIds34.add(linkId3);
		linkIds34.add(linkId4);
		NetworkRoute route2_5 = (NetworkRoute) routeFactory.createRoute(linkId2, linkId5);
		route2_5.setLinkIds(linkId2, linkIds34, linkId5);
		leg_2_5.setRoute(route2_5);

		Person person3 = popFactory.createPerson(testAgent3);
		Plan plan3 = popFactory.createPlan();
		Activity homeActLink2_3 = popFactory.createActivityFromLinkId("home", linkId2);
		homeActLink2_3.setEndTime(158);
		plan3.addActivity(homeActLink2_3);
		plan3.addLeg(leg_2_5);
		plan3.addActivity(workActLink5);
		person3.addPlan(plan3);
		population.addPerson(person3);

	}

	private void setPopulation6(Scenario scenario) {

		Population population = scenario.getPopulation();
		PopulationFactory popFactory = (PopulationFactory) scenario.getPopulation().getFactory();
		LinkNetworkRouteFactory routeFactory = new LinkNetworkRouteFactory();

		Activity workActLink5 = popFactory.createActivityFromLinkId("work", linkId5);

		// leg: 1,2,3,4,5
		Leg leg_1_5 = popFactory.createLeg("car");
		List<Id<Link>> linkIds234 = new ArrayList<Id<Link>>();
		linkIds234.add(linkId2);
		linkIds234.add(linkId3);
		linkIds234.add(linkId4);
		NetworkRoute route1_5 = (NetworkRoute) routeFactory.createRoute(linkId1, linkId5);
		route1_5.setLinkIds(linkId1, linkIds234, linkId5);
		leg_1_5.setRoute(route1_5);

		Person person1 = popFactory.createPerson(testAgent1);
		Plan plan1 = popFactory.createPlan();
		Activity homeActLink1_1 = popFactory.createActivityFromLinkId("home", linkId1);
		homeActLink1_1.setEndTime(100);
		plan1.addActivity(homeActLink1_1);
		plan1.addLeg(leg_1_5);
		plan1.addActivity(workActLink5);
		person1.addPlan(plan1);
		population.addPerson(person1);

		Person person2 = popFactory.createPerson(testAgent2);
		Plan plan2 = popFactory.createPlan();
		Activity homeActLink1_2 = popFactory.createActivityFromLinkId("home", linkId1);
		homeActLink1_2.setEndTime(106);
		plan2.addActivity(homeActLink1_2);
		{
			Leg leg = popFactory.createLeg(leg_1_5.getMode());
			PopulationUtils.copyFromTo(leg_1_5, leg);
			plan2.addLeg(leg);
		}
		plan2.addActivity(workActLink5);
		person2.addPlan(plan2);
		population.addPerson(person2);

		// leg: 1,2,3,4,5
		Leg leg_2_5 = popFactory.createLeg("car");
		List<Id<Link>> linkIds34 = new ArrayList<Id<Link>>();
		linkIds34.add(linkId3);
		linkIds34.add(linkId4);
		NetworkRoute route2_5 = (NetworkRoute) routeFactory.createRoute(linkId2, linkId5);
		route2_5.setLinkIds(linkId2, linkIds34, linkId5);
		leg_2_5.setRoute(route2_5);

		Person person3 = popFactory.createPerson(testAgent3);
		Plan plan3 = popFactory.createPlan();
		Activity homeActLink2_3 = popFactory.createActivityFromLinkId("home", linkId2);
		homeActLink2_3.setEndTime(154);
		plan3.addActivity(homeActLink2_3);
		plan3.addLeg(leg_2_5);
		plan3.addActivity(workActLink5);
		person3.addPlan(plan3);
		population.addPerson(person3);
	}

	private Scenario loadScenario1() {

		// (0)				(1)				(2)				(3)				(4)				(5)
		//    -----link1----   ----link2----   ----link3----   ----link4----   ----link5----

		Config config = testUtils.createConfig();
		QSimConfigGroup qSimConfigGroup = config.qsim();
		qSimConfigGroup.setFlowCapFactor(1.0);
		qSimConfigGroup.setStorageCapFactor(1.0);
		qSimConfigGroup.setInsertingWaitingVehiclesBeforeDrivingVehicles(true);
		qSimConfigGroup.setRemoveStuckVehicles(true);
		qSimConfigGroup.setStuckTime(3600.0);
		Scenario scenario = (ScenarioUtils.createScenario(config));

		Network network = (Network) scenario.getNetwork();
		network.setEffectiveCellSize(7.5);
		network.setCapacityPeriod(3600.);

		Node node0 = network.getFactory().createNode(Id.create("0", Node.class), new Coord(0., 0.));
		Node node1 = network.getFactory().createNode(Id.create("1", Node.class), new Coord(100., 0.));
		Node node2 = network.getFactory().createNode(Id.create("2", Node.class), new Coord(200., 0.));
		Node node3 = network.getFactory().createNode(Id.create("3", Node.class), new Coord(300., 0.));
		Node node4 = network.getFactory().createNode(Id.create("4", Node.class), new Coord(400., 0.));
		Node node5 = network.getFactory().createNode(Id.create("5", Node.class), new Coord(500., 0.));

		Link link1 = network.getFactory().createLink(this.linkId1, node0, node1);
		Link link2 = network.getFactory().createLink(this.linkId2, node1, node2);
		Link link3 = network.getFactory().createLink(this.linkId3, node2, node3);
		Link link4 = network.getFactory().createLink(this.linkId4, node3, node4);
		Link link5 = network.getFactory().createLink(this.linkId5, node4, node5);

		Set<String> modes = new HashSet<String>();
		modes.add("car");

		// link without capacity restrictions
		link1.setAllowedModes(modes);
		link1.setCapacity(999999);
		link1.setFreespeed(250); // one time step
		link1.setNumberOfLanes(100);
		link1.setLength(500);

		// link without capacity restrictions
		link2.setAllowedModes(modes);
		link2.setCapacity(999999);
		link2.setFreespeed(166.66666667); // two time steps
		link2.setNumberOfLanes(100);
		link2.setLength(500);

		// link meant to reach storage capacity: space for one car, flow capacity: one car every 10 sec
		link3.setAllowedModes(modes);
		link3.setCapacity(360);
		link3.setFreespeed(250); // one time step
		link3.setNumberOfLanes(1);
		link3.setLength(7.5);

		// link without capacity restrictions
		link4.setAllowedModes(modes);
		link4.setCapacity(999999);
		link4.setFreespeed(166.66666667); // two time steps
		link4.setNumberOfLanes(100);
		link4.setLength(500);

		// link without capacity restrictions
		link5.setAllowedModes(modes);
		link5.setCapacity(999999);
		link5.setFreespeed(250); // one time step
		link5.setNumberOfLanes(100);
		link5.setLength(500);

		network.addNode(node0);
		network.addNode(node1);
		network.addNode(node2);
		network.addNode(node3);
		network.addNode(node4);
		network.addNode(node5);

		network.addLink(link1);
		network.addLink(link2);
		network.addLink(link3);
		network.addLink(link4);
		network.addLink(link5);

		this.events = EventsUtils.createEventsManager();
		return scenario;
	}

	private Scenario loadScenario1b() {

		// only the stuckTimePeriod has be changed to a small value
		// (0)				(1)				(2)				(3)				(4)				(5)
		//    -----link1----   ----link2----   ----link3----   ----link4----   ----link5----

		Config config = testUtils.createConfig();
		QSimConfigGroup qSimConfigGroup = config.qsim();
		qSimConfigGroup.setFlowCapFactor(1.0);
		qSimConfigGroup.setStorageCapFactor(1.0);
		qSimConfigGroup.setInsertingWaitingVehiclesBeforeDrivingVehicles(true);
		qSimConfigGroup.setRemoveStuckVehicles(false);
		qSimConfigGroup.setStuckTime(6.0);
		Scenario scenario = (ScenarioUtils.createScenario(config));

		Network network = (Network) scenario.getNetwork();
		network.setEffectiveCellSize(7.5);
		network.setCapacityPeriod(3600.);

		Node node0 = network.getFactory().createNode(Id.create("0", Node.class), new Coord(0., 0.));
		Node node1 = network.getFactory().createNode(Id.create("1", Node.class), new Coord(100., 0.));
		Node node2 = network.getFactory().createNode(Id.create("2", Node.class), new Coord(200., 0.));
		Node node3 = network.getFactory().createNode(Id.create("3", Node.class), new Coord(300., 0.));
		Node node4 = network.getFactory().createNode(Id.create("4", Node.class), new Coord(400., 0.));
		Node node5 = network.getFactory().createNode(Id.create("5", Node.class), new Coord(500., 0.));

		Link link1 = network.getFactory().createLink(this.linkId1, node0, node1);
		Link link2 = network.getFactory().createLink(this.linkId2, node1, node2);
		Link link3 = network.getFactory().createLink(this.linkId3, node2, node3);
		Link link4 = network.getFactory().createLink(this.linkId4, node3, node4);
		Link link5 = network.getFactory().createLink(this.linkId5, node4, node5);

		Set<String> modes = new HashSet<String>();
		modes.add("car");

		// link without capacity restrictions
		link1.setAllowedModes(modes);
		link1.setCapacity(999999);
		link1.setFreespeed(250); // one time step
		link1.setNumberOfLanes(100);
		link1.setLength(500);

		// link without capacity restrictions
		link2.setAllowedModes(modes);
		link2.setCapacity(999999);
		link2.setFreespeed(166.66666667); // two time steps
		link2.setNumberOfLanes(100);
		link2.setLength(500);

		// link meant to reach storage capacity: space for one car, flow capacity: one car every 10 sec
		link3.setAllowedModes(modes);
		link3.setCapacity(360);
		link3.setFreespeed(250); // one time step
		link3.setNumberOfLanes(1);
		link3.setLength(7.5);

		// link without capacity restrictions
		link4.setAllowedModes(modes);
		link4.setCapacity(999999);
		link4.setFreespeed(166.66666667); // two time steps
		link4.setNumberOfLanes(100);
		link4.setLength(500);

		// link without capacity restrictions
		link5.setAllowedModes(modes);
		link5.setCapacity(999999);
		link5.setFreespeed(250); // one time step
		link5.setNumberOfLanes(100);
		link5.setLength(500);

		network.addNode(node0);
		network.addNode(node1);
		network.addNode(node2);
		network.addNode(node3);
		network.addNode(node4);
		network.addNode(node5);

		network.addLink(link1);
		network.addLink(link2);
		network.addLink(link3);
		network.addLink(link4);
		network.addLink(link5);

		this.events = EventsUtils.createEventsManager();
		return scenario;
	}

	private Scenario loadScenario4() {

		// (0)				(1)				(2)				(3)				(4)				(5)
		//    -----link1----   ----link2----   ----link3----   ----link4----   ----link5----

		Config config = testUtils.createConfig();
		QSimConfigGroup qSimConfigGroup = config.qsim();
		qSimConfigGroup.setFlowCapFactor(1.0);
		qSimConfigGroup.setStorageCapFactor(1.0);
		qSimConfigGroup.setInsertingWaitingVehiclesBeforeDrivingVehicles(false);
		qSimConfigGroup.setRemoveStuckVehicles(true);
		qSimConfigGroup.setStuckTime(3600.0);
		Scenario scenario = (ScenarioUtils.createScenario(config));

		Network network = (Network) scenario.getNetwork();
		network.setEffectiveCellSize(7.5);
		network.setCapacityPeriod(3600.);

		Node node0 = network.getFactory().createNode(Id.create("0", Node.class), new Coord(0., 0.));
		Node node1 = network.getFactory().createNode(Id.create("1", Node.class), new Coord(100., 0.));
		Node node2 = network.getFactory().createNode(Id.create("2", Node.class), new Coord(200., 0.));
		Node node3 = network.getFactory().createNode(Id.create("3", Node.class), new Coord(300., 0.));
		Node node4 = network.getFactory().createNode(Id.create("4", Node.class), new Coord(400., 0.));
		Node node5 = network.getFactory().createNode(Id.create("5", Node.class), new Coord(500., 0.));

		Link link1 = network.getFactory().createLink(this.linkId1, node0, node1);
		Link link2 = network.getFactory().createLink(this.linkId2, node1, node2);
		Link link3 = network.getFactory().createLink(this.linkId3, node2, node3);
		Link link4 = network.getFactory().createLink(this.linkId4, node3, node4);
		Link link5 = network.getFactory().createLink(this.linkId5, node4, node5);

		Set<String> modes = new HashSet<String>();
		modes.add("car");

		// link without capacity restrictions
		link1.setAllowedModes(modes);
		link1.setCapacity(999999);
		link1.setFreespeed(10);
		link1.setNumberOfLanes(100);
		link1.setLength(500);

		// link with capacity restrictions
		link2.setAllowedModes(modes);
		link2.setCapacity(360);
		link2.setFreespeed(10);
		link2.setNumberOfLanes(100);
		link2.setLength(500);

		// link without capacity restrictions
		link3.setAllowedModes(modes);
		link3.setCapacity(999999);
		link3.setFreespeed(10);
		link3.setNumberOfLanes(100);
		link3.setLength(500);

		// link without capacity restrictions
		link4.setAllowedModes(modes);
		link4.setCapacity(999999);
		link4.setFreespeed(10);
		link4.setNumberOfLanes(100);
		link4.setLength(500);

		// link without capacity restrictions
		link5.setAllowedModes(modes);
		link5.setCapacity(999999);
		link5.setFreespeed(10);
		link5.setNumberOfLanes(100);
		link5.setLength(500);

		network.addNode(node0);
		network.addNode(node1);
		network.addNode(node2);
		network.addNode(node3);
		network.addNode(node4);
		network.addNode(node5);

		network.addLink(link1);
		network.addLink(link2);
		network.addLink(link3);
		network.addLink(link4);
		network.addLink(link5);

		this.events = EventsUtils.createEventsManager();
		return scenario;
	}

	private Scenario loadScenario5() {

		// (0)				(1)				(2)				(3)				(4)				(5)
		//    -----link1----   ----link2----   ----link3----   ----link4----   ----link5----

		Config config =  testUtils.createConfig();
		QSimConfigGroup qSimConfigGroup = config.qsim();
		qSimConfigGroup.setFlowCapFactor(1.0);
		qSimConfigGroup.setStorageCapFactor(1.0);
		qSimConfigGroup.setInsertingWaitingVehiclesBeforeDrivingVehicles(true);
		qSimConfigGroup.setRemoveStuckVehicles(true);
		qSimConfigGroup.setStuckTime(3600.0);
		Scenario scenario = (ScenarioUtils.createScenario(config));

		Network network = (Network) scenario.getNetwork();
		network.setEffectiveCellSize(7.5);
		network.setCapacityPeriod(3600.);

		Node node0 = network.getFactory().createNode(Id.create("0", Node.class), new Coord(0., 0.));
		Node node1 = network.getFactory().createNode(Id.create("1", Node.class), new Coord(100., 0.));
		Node node2 = network.getFactory().createNode(Id.create("2", Node.class), new Coord(200., 0.));
		Node node3 = network.getFactory().createNode(Id.create("3", Node.class), new Coord(300., 0.));
		Node node4 = network.getFactory().createNode(Id.create("4", Node.class), new Coord(400., 0.));
		Node node5 = network.getFactory().createNode(Id.create("5", Node.class), new Coord(500., 0.));

		Link link1 = network.getFactory().createLink(this.linkId1, node0, node1);
		Link link2 = network.getFactory().createLink(this.linkId2, node1, node2);
		Link link3 = network.getFactory().createLink(this.linkId3, node2, node3);
		Link link4 = network.getFactory().createLink(this.linkId4, node3, node4);
		Link link5 = network.getFactory().createLink(this.linkId5, node4, node5);

		Set<String> modes = new HashSet<String>();
		modes.add("car");

		// link without capacity restrictions
		link1.setAllowedModes(modes);
		link1.setCapacity(999999);
		link1.setFreespeed(10);
		link1.setNumberOfLanes(100);
		link1.setLength(500);

		// link with capacity restrictions
		link2.setAllowedModes(modes);
		link2.setCapacity(360);
		link2.setFreespeed(10);
		link2.setNumberOfLanes(100);
		link2.setLength(500);

		// link without capacity restrictions
		link3.setAllowedModes(modes);
		link3.setCapacity(999999);
		link3.setFreespeed(10);
		link3.setNumberOfLanes(100);
		link3.setLength(500);

		// link without capacity restrictions
		link4.setAllowedModes(modes);
		link4.setCapacity(999999);
		link4.setFreespeed(10);
		link4.setNumberOfLanes(100);
		link4.setLength(500);

		// link without capacity restrictions
		link5.setAllowedModes(modes);
		link5.setCapacity(999999);
		link5.setFreespeed(10);
		link5.setNumberOfLanes(100);
		link5.setLength(500);

		network.addNode(node0);
		network.addNode(node1);
		network.addNode(node2);
		network.addNode(node3);
		network.addNode(node4);
		network.addNode(node5);

		network.addLink(link1);
		network.addLink(link2);
		network.addLink(link3);
		network.addLink(link4);
		network.addLink(link5);

		this.events = EventsUtils.createEventsManager();
		return scenario;
	}

	private QSim createQSim(Scenario sc, EventsManager events) {
		return new QSimBuilder(sc.getConfig()).useDefaults().build(sc, events);
	}

}
