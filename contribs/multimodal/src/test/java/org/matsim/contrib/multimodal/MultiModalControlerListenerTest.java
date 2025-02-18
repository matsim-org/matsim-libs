/* *********************************************************************** *
 * project: org.matsim.*
 * MultiModalControlerListenerTest.java
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

package org.matsim.contrib.multimodal;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.contrib.multimodal.config.MultiModalConfigGroup;
import org.matsim.contrib.multimodal.tools.PrepareMultiModalScenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ScoringConfigGroup.ActivityParams;
import org.matsim.core.config.groups.RoutingConfigGroup;
import org.matsim.core.config.groups.PlansConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.CollectionUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.Vehicle;

public class MultiModalControlerListenerTest {

	private static final Logger log = LogManager.getLogger(MultiModalControlerListenerTest.class);

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();

	@SuppressWarnings("static-method")
	@Test
	void testSimpleScenario() {
		log.info("Run test single threaded...");
		runSimpleScenario(1);

		log.info("Run test multi threaded...");
		runSimpleScenario(2);
		runSimpleScenario(4);
	}

	static void runSimpleScenario(int numberOfThreads) {

		Config config = ConfigUtils.createConfig();
		config.routing().setNetworkRouteConsistencyCheck(RoutingConfigGroup.NetworkRouteConsistencyCheck.disable);
		config.qsim().setEndTime(24 * 3600);

		config.controller().setLastIteration(0);
		// doesn't matter - MultiModalModule sets the mobsim unconditionally. it just can't be something
		// which the ControlerDefaultsModule knows about. Try it, you will get an error. Quite safe.
		config.controller().setMobsim("myMobsim");

		MultiModalConfigGroup multiModalConfigGroup = new MultiModalConfigGroup();
		multiModalConfigGroup.setMultiModalSimulationEnabled(true);
		multiModalConfigGroup.setSimulatedModes("walk,bike,other");
		multiModalConfigGroup.setNumberOfThreads(numberOfThreads);
		config.addModule(multiModalConfigGroup);

		ActivityParams homeParams = new ActivityParams("home");
		homeParams.setTypicalDuration(16*3600);
		config.scoring().addActivityParams(homeParams);

		// set default walk speed; according to Weidmann 1.34 [m/s]
		double defaultWalkSpeed = 1.34;
		config.routing().setTeleportedModeSpeed(TransportMode.walk, defaultWalkSpeed);

		// set default bike speed; Parkin and Rotheram according to 6.01 [m/s]
		double defaultBikeSpeed = 6.01;
		config.routing().setTeleportedModeSpeed(TransportMode.bike, defaultBikeSpeed);

		// set unkown mode speed
		double unknownModeSpeed = 2.0;
		config.routing().setTeleportedModeSpeed("other", unknownModeSpeed);

        config.travelTimeCalculator().setFilterModes(true);

		Scenario scenario = ScenarioUtils.createScenario(config);

		final NetworkFactory nf = scenario.getNetwork().getFactory();
		Node node0 = nf.createNode(Id.create("n0", Node.class), new Coord(0.0, 0.0));
		Node node1 = nf.createNode(Id.create("n1", Node.class), new Coord(1.0, 0.0));
		Node node2 = nf.createNode(Id.create("n2", Node.class), new Coord(2.0, 0.0));
		Node node3 = nf.createNode(Id.create("n3", Node.class), new Coord(3.0, 0.0));

		Link link0 = nf.createLink(Id.create("l0", Link.class), node0, node1);
		Link link1 = nf.createLink(Id.create("l1", Link.class), node1, node2);
		Link link2 = nf.createLink(Id.create("l2", Link.class), node1, node2);
		Link link3 = nf.createLink(Id.create("l3", Link.class), node1, node2);
		Link link4 = nf.createLink(Id.create("l4", Link.class), node1, node2);
		Link link5 = nf.createLink(Id.create("l5", Link.class), node2, node3);

		link0.setLength(1.0);
		link1.setLength(1.0);
		link2.setLength(10.0);
		link3.setLength(100.0);
		link4.setLength(1000.0);
		link5.setLength(1.0);

		link0.setAllowedModes(CollectionUtils.stringToSet("car,bike,walk,other"));
		link1.setAllowedModes(CollectionUtils.stringToSet("car"));
		link2.setAllowedModes(CollectionUtils.stringToSet("bike"));
		link3.setAllowedModes(CollectionUtils.stringToSet("walk"));
		link4.setAllowedModes(CollectionUtils.stringToSet("other"));
		link5.setAllowedModes(CollectionUtils.stringToSet("car,bike,walk,other"));

		scenario.getNetwork().addNode(node0);
		scenario.getNetwork().addNode(node1);
		scenario.getNetwork().addNode(node2);
		scenario.getNetwork().addNode(node3);
		scenario.getNetwork().addLink(link0);
		scenario.getNetwork().addLink(link1);
		scenario.getNetwork().addLink(link2);
		scenario.getNetwork().addLink(link3);
		scenario.getNetwork().addLink(link4);
		scenario.getNetwork().addLink(link5);

		scenario.getPopulation().addPerson(createPerson(scenario, "p0", "car"));
		scenario.getPopulation().addPerson(createPerson(scenario, "p1", "bike"));
		scenario.getPopulation().addPerson(createPerson(scenario, "p2", "walk"));
		scenario.getPopulation().addPerson(createPerson(scenario, "p3", "other"));

		Controler controler = new Controler(scenario);
        controler.getConfig().controller().setCreateGraphs(false);
		controler.getConfig().controller().setDumpDataAtEnd(false);
		controler.getConfig().controller().setWriteEventsInterval(0);
		controler.getConfig().controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);

		// controler listener that initializes the multi-modal simulation
        controler.addOverridingModule(new MultiModalModule());

        LinkModeChecker linkModeChecker = new LinkModeChecker(scenario.getNetwork());
		controler.getEvents().addHandler(linkModeChecker);

		controler.run();

		// assume that the number of arrival events is correct
		Assertions.assertEquals(4, linkModeChecker.arrivalCount);

		// assume that the number of link left events is correct
		Assertions.assertEquals(8, linkModeChecker.linkLeftCount);
	}

	@Disabled("Due to bugfixes in slow flowCap accumulation in QueueWithBuffer")//by michalm
	@Test
	void testBerlinScenario_singleThreaded() {
		log.info("Run test single threaded...");
		runBerlinScenario(1);
	}

	@Disabled("Due to bugfixes in slow flowCap accumulation in QueueWithBuffer")//by michalm
	@Test
	void testBerlinScenario_multiThreaded_2() {
		log.info("Run test multi threaded with 2 threads...");
		runBerlinScenario(2);
	}

	@Disabled("Due to bugfixes in slow flowCap accumulation in QueueWithBuffer")//by michalm
	@Test
	void testBerlinScenario_multiThreaded_4() {
		log.info("Run test multi threaded with 4 threads...");
		runBerlinScenario(4);
	}

	void runBerlinScenario(int numberOfThreads) {

		String inputDir = this.utils.getClassInputDirectory();
		Config config = ConfigUtils.loadConfig(IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("berlin"), "config.xml"));
		ConfigUtils.loadConfig(config, inputDir + "config_berlin_multimodal.xml");
		config.addModule(new MultiModalConfigGroup());
		config.controller().setOutputDirectory(this.utils.getOutputDirectory());

		// doesn't matter - MultiModalModule sets the mobsim unconditionally. it just can't be something
		// which the ControlerDefaultsModule knows about. Try it, you will get an error. Quite safe.
		config.controller().setMobsim("myMobsim");

//		config.qsim().setRemoveStuckVehicles(true); // but why?  kai, feb'16
		config.qsim().setRemoveStuckVehicles(false);
		config.qsim().setStuckTime(100.0);

		config.plans().setActivityDurationInterpretation( PlansConfigGroup.ActivityDurationInterpretation.minOfDurationAndEndTime );
		// added by me to fix the test.  If you normally run with the default setting (now tryEndTimeThenDuration), I would suggest to remove
		// the above line and adapt the test outcome.  Kai, feb'14

        config.travelTimeCalculator().setFilterModes(true);

		Scenario scenario = ScenarioUtils.loadScenario(config);

		MultiModalConfigGroup multiModalConfigGroup = (MultiModalConfigGroup) scenario.getConfig().getModule(MultiModalConfigGroup.GROUP_NAME);
		multiModalConfigGroup.setNumberOfThreads(numberOfThreads);

		/*
		 * Create some bike trips since there are non present in the population.
		 */
		int i = 0;
		for (Person person : scenario.getPopulation().getPersons().values()) {

			Plan plan = person.getSelectedPlan();
			for (PlanElement planElement : plan.getPlanElements()) {
				if (planElement instanceof Leg) {
					((Leg) planElement).setMode(TransportMode.bike);
				}
			}
			i++;
			if (i >= 50) break;
		}

		PrepareMultiModalScenario.run(scenario);

		Controler controler = new Controler(scenario);
        controler.getConfig().controller().setCreateGraphs(false);
		controler.getConfig().controller().setDumpDataAtEnd(false);
		controler.getConfig().controller().setWriteEventsInterval(0);
		controler.getConfig().controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);

		// controler listener that initializes the multi-modal simulation
        controler.addOverridingModule(new MultiModalModule());
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addTravelTimeBinding(TransportMode.ride).to( carTravelTime() );
        		addTravelDisutilityFactoryBinding(TransportMode.ride).to(carTravelDisutilityFactoryKey());
			}
		});

        LinkModeChecker linkModeChecker = new LinkModeChecker(controler.getScenario().getNetwork());
		controler.getEvents().addHandler(linkModeChecker);

		controler.run();

		/* NOTE: When I introduced access/egress legs, nearly everything in the following (besides bikeCount) changed.
		 * After setting removeStuckVehicles from true to false, the counts were stable.  So with access/egress legs, a
		 * different number of vehicles got lost ... which makes sense, because they enter/leave the traffic at different times.
		 *
		 * Also, after not losing vehicles vehicles any more, the travel times for the uncongested modes bike and walk were stable.
		 * Predictably, the travel time for the congested mode changes.
		 *
		 * kai, feb'16
		 */

		// check the number of link leave events
		int carCount = linkModeChecker.leftCountPerMode.get(TransportMode.car);
		int bikeCount = linkModeChecker.leftCountPerMode.get(TransportMode.bike);
		int walkCount = linkModeChecker.leftCountPerMode.get(TransportMode.walk);
		Assertions.assertEquals(
				692259, carCount, "unexpected number of link leave events for mode car with number of threads "+numberOfThreads);
		Assertions.assertEquals(
				4577, bikeCount, "unexpected number of link leave events for mode bike with number of threads "+numberOfThreads);
		Assertions.assertEquals(
				7970, walkCount, "unexpected number of link leave events for mode walk with number of threads "+numberOfThreads);

		// check the total number of link left events
		Assertions.assertEquals(
				704806, linkModeChecker.linkLeftCount, "unexpected total number of link leave events with number of threads "+numberOfThreads);

		// check the total mode travel times
		double carTravelTime = linkModeChecker.travelTimesPerMode.get(TransportMode.car);
		double bikeTravelTime = linkModeChecker.travelTimesPerMode.get(TransportMode.bike);
		double walkTravelTime = linkModeChecker.travelTimesPerMode.get(TransportMode.walk);
		LogManager.getLogger( this.getClass() ).warn( "carTravelTime: " + carTravelTime ) ;
		LogManager.getLogger( this.getClass() ).warn( "bikeTravelTime: " + bikeTravelTime ) ;
		LogManager.getLogger( this.getClass() ).warn( "walkTravelTime: " + walkTravelTime ) ;
		if ( !config.routing().getAccessEgressType().equals(RoutingConfigGroup.AccessEgressType.none) ) {
			Assertions.assertEquals(
					1.1186864E8, carTravelTime, MatsimTestUtils.EPSILON, "unexpected total travel time for car mode with number of threads "+numberOfThreads);
		} else {
			Assertions.assertEquals(
				1.11881636E8, carTravelTime, MatsimTestUtils.EPSILON, "unexpected total travel time for car mode with number of threads "+numberOfThreads);
		}
		Assertions.assertEquals(
				480275.0, bikeTravelTime, MatsimTestUtils.EPSILON, "unexpected total travel time for bike mode with number of threads "+numberOfThreads);
		Assertions.assertEquals(
				3885025.0, walkTravelTime, MatsimTestUtils.EPSILON, "unexpected total travel time for walk mode with number of threads "+numberOfThreads);
	}

	private static Person createPerson(Scenario scenario, String id, String mode) {
		final PopulationFactory pf = scenario.getPopulation().getFactory();
		Person person = pf.createPerson(Id.create(id, Person.class));

		Activity from = pf.createActivityFromLinkId("home", Id.create("l0", Link.class));
		Leg leg = pf.createLeg(mode);
		Activity to = pf.createActivityFromLinkId("home", Id.create("l5", Link.class));

		from.setEndTime(8*3600);
		leg.setDepartureTime(8*3600);

		Plan plan = pf.createPlan();
		plan.addActivity(from);
		plan.addLeg(leg);
		plan.addActivity(to);

		person.addPlan(plan);

		return person;
	}

	private static class LinkModeChecker implements LinkLeaveEventHandler, PersonDepartureEventHandler,
	PersonArrivalEventHandler, VehicleEntersTrafficEventHandler {

		int arrivalCount = 0;
		int linkLeftCount = 0;

		private final Network network;

		// contains only modes for vehicles with wait2link events (needed to count link leave events)
		private final Map<Id<Vehicle>, String> vehModes = new HashMap<>();

		// contains also modes for teleported agents (needed to calculate travel times of all modes)
		private final Map<Id<Person>, String> agModes = new HashMap<>();

		private final Map<Id<Person>, Double> departures = new HashMap<>();
		final Map<String, Integer> leftCountPerMode = new HashMap<>();
		final Map<String, Double> travelTimesPerMode = new HashMap<>();

		public LinkModeChecker(Network network) {
			this.network = network;

			leftCountPerMode.put(TransportMode.car, 0);
			leftCountPerMode.put(TransportMode.bike, 0);
			leftCountPerMode.put(TransportMode.walk, 0);
			leftCountPerMode.put(TransportMode.ride, 0);
			leftCountPerMode.put(TransportMode.pt, 0);
			leftCountPerMode.put("other", 0);

			travelTimesPerMode.put(TransportMode.car, 0.0);
			travelTimesPerMode.put(TransportMode.bike, 0.0);
			travelTimesPerMode.put(TransportMode.walk, 0.0);
			travelTimesPerMode.put(TransportMode.ride, 0.0);
			travelTimesPerMode.put(TransportMode.pt, 0.0);
			travelTimesPerMode.put("other", 0.0);
		}

		@Override
		public void reset(int iteration) {
			// nothing to do here
		}

		@Override
		public void handleEvent(PersonDepartureEvent event) {
			this.departures.put(event.getPersonId(), event.getTime());
			this.agModes.put(event.getPersonId(), event.getLegMode());
		}

		@Override
		public void handleEvent(VehicleEntersTrafficEvent event) {
			this.vehModes.put(event.getVehicleId(), event.getNetworkMode());
		}

		@Override
		public void handleEvent(LinkLeaveEvent event) {
			Link link = this.network.getLinks().get(event.getLinkId());
			String mode = this.vehModes.get(event.getVehicleId());

			if (!link.getAllowedModes().contains(mode)) {
				log.error(mode);
			}

			// assume that the agent is allowed to travel on the link
			Assertions.assertEquals(true, link.getAllowedModes().contains(mode));

			if ( mode.contains(TransportMode.non_network_walk ) || mode.contains(TransportMode.non_network_walk ) ) {
				return ;
			}
			this.linkLeftCount++;

			int count = this.leftCountPerMode.get(mode);
			this.leftCountPerMode.put(mode, count + 1);
		}

		@Override
		public void handleEvent(PersonArrivalEvent event) {
			String mode = this.agModes.remove(event.getPersonId());
			this.arrivalCount++;

			double tripTravelTime = event.getTime() - this.departures.remove(event.getPersonId());
			if ( mode.contains(TransportMode.non_network_walk ) || mode.contains(TransportMode.non_network_walk ) ) {
				return ;
			}
			Double modeTravelTime = this.travelTimesPerMode.get(mode);
			if ( modeTravelTime==null ) {
				LogManager.getLogger(this.getClass()).warn( "mode:" + mode );
				LogManager.getLogger(this.getClass()).warn( "travelTimesPerMode:" + this.travelTimesPerMode );
			}
			this.travelTimesPerMode.put(mode, modeTravelTime + tripTravelTime);
		}
	}

}
