/* *********************************************************************** *
 * project: org.matsim.*                                                   *
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

package org.matsim.contrib.accessibility.run;

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.*;
import org.matsim.contrib.accessibility.*;
import org.matsim.contrib.accessibility.AccessibilityConfigGroup.AreaOfAccesssibilityComputation;
import org.matsim.contrib.drt.estimator.DrtEstimator;
import org.matsim.contrib.drt.estimator.impl.DirectTripBasedDrtEstimator;
import org.matsim.contrib.drt.estimator.impl.distribution.NoDistribution;
import org.matsim.contrib.drt.estimator.impl.trip_estimation.ConstantRideDurationEstimator;
import org.matsim.contrib.drt.estimator.impl.waiting_time_estimation.ConstantWaitingTimeEstimator;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.FacilitiesConfigGroup;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.testcases.MatsimTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;

/**
 * A small test that enables to easily compare results with hand-computed results.
 *
 * @author jakobrehmann
 */
public class EstimatedDrtAccessibilityTest {

	private static final Logger LOG = LogManager.getLogger(EstimatedDrtAccessibilityTest.class);

	@RegisterExtension
	private static MatsimTestUtils utils = new MatsimTestUtils();
	public static double gapBtwnNodes = 1000.;
	public double carSpeed;


	String emptyEventsFileName;

	String congestedEventsFileName;
	public int drtWaitTime;
	// Opportunity is at x = 2600m. We multiply distance by beeline factor (1.3).
	public int positionOfOpportunity = 2600;

	@BeforeEach
	void prepare(){
		emptyEventsFileName = utils.getClassInputDirectory() + "output_events.xml.gz";
		congestedEventsFileName = utils.getClassInputDirectory() + "output_events_congested.xml.gz";

	}
	/**
	 * Tests offline accessibility, based on events file. In this case, the events file is empty.
	 */
	@Test
	void runFromEvents() {


		// Set Parameters
		drtWaitTime = 300;
		carSpeed = 50 / 3.6;

		// Create Network
		Network network = createLineNetwork(carSpeed);

		// Create  & Update Config
		final Config config = createTestConfig();

		AccessibilityConfigGroup acg = ConfigUtils.addOrGetModule(config, AccessibilityConfigGroup.class) ;
		acg.setAreaOfAccessibilityComputation(AreaOfAccesssibilityComputation.fromBoundingBox);
		acg.setBoundingBoxBottom(-250).setBoundingBoxTop(250).setBoundingBoxLeft(0).setBoundingBoxRight(4000);
		acg.setTileSize_m(500);

		for (Modes4Accessibility mode : Modes4Accessibility.values()) {
			acg.setComputingAccessibilityForMode(mode, mode.equals(Modes4Accessibility.estimatedDrt) || mode.equals(Modes4Accessibility.teleportedWalk));
		}

		acg.setUseParallelization(false);

		addDrtParamsToConfig(config, network);



		// Create  Scenario
		final Scenario scenario = createTestScenario(config, network);

		// Set up DRT estimator

		DrtEstimator drtEstimator = new DirectTripBasedDrtEstimator.Builder()
			.setWaitingTimeEstimator(new ConstantWaitingTimeEstimator(drtWaitTime))
			.setWaitingTimeDistributionGenerator(new NoDistribution())
			.setRideDurationEstimator(new ConstantRideDurationEstimator(1, 0))
			.setRideDurationDistributionGenerator(new NoDistribution())
			.build();

		// Run Accessibility Analysis
		AccessibilityFromEvents.Builder builder = new AccessibilityFromEvents.Builder( scenario , emptyEventsFileName );
		TinyAccessibilityTest.ResultsComparator dataListener = new TinyAccessibilityTest.ResultsComparator();
		builder.addDataListener(dataListener);
		builder.addDrtEstimator(drtEstimator);

		builder.build().run() ;


		// ------------
		// Check results
		// ------------

		// A) First we will check the accessibility for teleportedWalk. This is important, because teleportedWalk is the fallback
		// mode for DRT, so we need to check these calculations first.
		Map<Tuple<ActivityFacility, Double>, Map<String, Double>> accessibilitiesMap = dataListener.getAccessibilitiesMap();
		Double walkBeelineFactor = config.routing().getBeelineDistanceFactors().get("walk");
		Double walkSpeed = config.routing().getTeleportedModeSpeeds().get("walk");
		for (Map.Entry<Tuple<ActivityFacility, Double>, Map<String, Double>> tupleMapEntry : accessibilitiesMap.entrySet()) {
			double dist = Math.abs(positionOfOpportunity - tupleMapEntry.getKey().getFirst().getCoord().getX()) * walkBeelineFactor;
			// time converted into hours
			double time = dist / walkSpeed / 3600;
			// accessibility for walk is just time x -12
			double accessibilityShould = time * (config.scoring().getModes().get("walk").getMarginalUtilityOfTraveling() - config.scoring().getPerforming_utils_hr());
			// we compare this "hand-calculated" value with the result from the accessibility analysis
			double accessibilityActual = tupleMapEntry.getValue().get(Modes4Accessibility.teleportedWalk.name());
			Assertions.assertEquals(accessibilityShould, accessibilityActual, MatsimTestUtils.EPSILON, "Unexpected accessibility for walk at " + tupleMapEntry.getKey().getFirst().getCoord().getX());
		}


		// @ Measuring point (250,0) we expect following:
		// Access Walk: we expect agent to walk 250 to closest DRT stop (back to node 0)
		// DRT trip is 3000m @ 2.7m/s + waiting time of 300 seconds. We assume -12 (=-6 -6) utils/hr.
		// Egress walk should be 400m x 1.3 (beeline factor) and walk speed of 0.83 m/s.



		{
			int x = 250;
			Map<String, Double> accMap = accessibilitiesMap.entrySet().stream().filter(entry -> entry.getKey().getFirst().getCoord().equals(new Coord(x, 0))).map(Map.Entry::getValue).findFirst().get();
			double accessWalkUtility = 	250 * walkBeelineFactor / walkSpeed / 3600 * -12;;
			// Note Method "addLink" in SpeedyGraphBuilder also rounds two 2 decimal places; if we don't do that, the difference exceeds our EPSILON
			double drtTripUtility = (Math.round(3000 / carSpeed * 100.) / 100. + drtWaitTime) / 3600 * -12;
			double egressWalkUtility = 400 * walkBeelineFactor / walkSpeed / 3600 * -12;

			double drtUtilityShould = accessWalkUtility + drtTripUtility + egressWalkUtility;
			double drtUtilityActual = accMap.get(Modes4Accessibility.estimatedDrt.name());
			double walkUtilityActual = accMap.get(Modes4Accessibility.teleportedWalk.name());
			Assertions.assertEquals(drtUtilityShould, drtUtilityActual, MatsimTestUtils.EPSILON);
			Assertions.assertTrue(drtUtilityActual > walkUtilityActual );
		}

		// @ Measuring point (750,0) we expect following:
		// Access Walk: we expect agent to walk 250 to closest DRT stop (forward to node 1)
		// DRT trip is 2000m @ 2.7m/s + waiting time of 300 seconds. We assume -12 (=-6 -6) utils/hr.
		// Egress walk should be 400m x 1.3 (beeline factor) and walk speed of 0.83 m/s.
		{
			int x = 750;
			Map<String, Double> accMap = accessibilitiesMap.entrySet().stream().filter(entry -> entry.getKey().getFirst().getCoord().equals(new Coord(x, 0))).map(Map.Entry::getValue).findFirst().get();
			double accessWalkUtility = 250 * walkBeelineFactor / walkSpeed / 3600 * -12;
			// Note Method "addLink" in SpeedyGraphBuilder also rounds two 2 decimal places; if we don't do that, the difference exceeds our EPSILON
			double drtTripUtility = (Math.round(2000 / carSpeed * 100.) / 100. + drtWaitTime) / 3600 * -12;
			double egressWalkUtility = 400 * walkBeelineFactor / walkSpeed / 3600 * -12;

			double drtUtilityShould = accessWalkUtility + drtTripUtility + egressWalkUtility;
			double drtUtilityActual = accMap.get(Modes4Accessibility.estimatedDrt.name());
			double walkUtilityActual = accMap.get(Modes4Accessibility.teleportedWalk.name());
			Assertions.assertEquals(drtUtilityShould, drtUtilityActual, MatsimTestUtils.EPSILON);
			Assertions.assertTrue(drtUtilityActual > walkUtilityActual );
		}

		// @ Measuring point (1250,0) we expect following:
		// Access Walk: we expect agent to walk 250 to closest DRT stop (backward to node 1)
		// DRT trip is 2000m @ 2.7m/s + waiting time of 300 seconds. We assume -12 (=-6 -6) utils/hr.
		// Egress walk should be 400m x 1.3 (beeline factor) and walk speed of 0.83 m/s.
		{
			int x = 1250;
			Map<String, Double> accMap = accessibilitiesMap.entrySet().stream().filter(entry -> entry.getKey().getFirst().getCoord().equals(new Coord(x, 0))).map(Map.Entry::getValue).findFirst().get();
			double accessWalkUtility = 250 * walkBeelineFactor / walkSpeed / 3600 * -12;
			// Note Method "addLink" in SpeedyGraphBuilder also rounds two 2 decimal places; if we don't do that, the difference exceeds our EPSILON
			double drtTripUtility = (Math.round(2000 / carSpeed * 100.) / 100. + drtWaitTime) / 3600 * -12;
			double egressWalkUtility = 400 * walkBeelineFactor / walkSpeed / 3600 * -12;

			double drtUtilityShould = accessWalkUtility + drtTripUtility + egressWalkUtility;
			double drtUtilityActual = accMap.get(Modes4Accessibility.estimatedDrt.name());
			double walkUtilityActual = accMap.get(Modes4Accessibility.teleportedWalk.name());
			Assertions.assertEquals(drtUtilityShould, drtUtilityActual, MatsimTestUtils.EPSILON);
			Assertions.assertTrue(drtUtilityActual > walkUtilityActual );
		}

		// @ Measuring point (1750,0) we expect following:
		// Access Walk: we expect agent to walk 250 to closest DRT stop (backward to node 1)
		// DRT trip is 1000 @ 2.7m/s + waiting time of 300 seconds. We assume -12 (=-6 -6) utils/hr.
		// Egress walk should be 400m x 1.3 (beeline factor) and walk speed of 0.83 m/s.
		{
			int x = 1750;
			Map<String, Double> accMap = accessibilitiesMap.entrySet().stream().filter(entry -> entry.getKey().getFirst().getCoord().equals(new Coord(x, 0))).map(Map.Entry::getValue).findFirst().get();
			double accessWalkUtility = 250 * walkBeelineFactor / walkSpeed / 3600 * -12;
			// Note Method "addLink" in SpeedyGraphBuilder also rounds two 2 decimal places; if we don't do that, the difference exceeds our EPSILON
			double drtTripUtility = (Math.round(1000 / carSpeed * 100.) / 100. + drtWaitTime) / 3600 * -12;
			double egressWalkUtility = 400 * walkBeelineFactor / walkSpeed / 3600 * -12;

			double drtUtilityShould = accessWalkUtility + drtTripUtility + egressWalkUtility;
			double drtUtilityActual = accMap.get(Modes4Accessibility.estimatedDrt.name());
			double walkUtilityActual = accMap.get(Modes4Accessibility.teleportedWalk.name());
			Assertions.assertTrue(drtUtilityShould < walkUtilityActual );
			Assertions.assertEquals(walkUtilityActual, drtUtilityActual, MatsimTestUtils.EPSILON);
		}

	}

	private void addDrtParamsToConfig(Config config, Network network)  {
		String stopsInputFileName = utils.getClassInputDirectory() + "drtStops.xml";

		try {
			createDrtStopsFile(stopsInputFileName, network);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}


		ScoringConfigGroup.ModeParams drtParams = new ScoringConfigGroup.ModeParams(TransportMode.drt);
//		drtParams.setMarginalUtilityOfTraveling(0);
		config.scoring().addModeParams(drtParams);

		ConfigUtils.addOrGetModule(config, DvrpConfigGroup.class );

		DrtConfigGroup drtConfigGroup = new DrtConfigGroup();
		drtConfigGroup.setOperationalScheme(DrtConfigGroup.OperationalScheme.stopbased);
		drtConfigGroup.setTransitStopFile( stopsInputFileName);

		drtConfigGroup.addOrGetDrtOptimizationConstraintsParams().addOrGetDefaultDrtOptimizationConstraintsSet().maxWalkDistance = 200;


		MultiModeDrtConfigGroup multiModeDrtConfigGroup = new MultiModeDrtConfigGroup();
		multiModeDrtConfigGroup.addParameterSet(drtConfigGroup);
		config.addModule(multiModeDrtConfigGroup);
		config.addModule(drtConfigGroup);
	}



	private void createDrtStopsFile(String stopsInputFileName, Network network) throws IOException {
		Scenario dummyScenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		TransitScheduleFactory tf = dummyScenario.getTransitSchedule().getFactory();


		for (Node node : network.getNodes().values()) {

//			if(node.getId().toString().contains("-")) continue;

			int nodeId = Integer.parseInt(node.getId().toString());

			TransitStopFacility stop = tf.createTransitStopFacility(Id.create(nodeId, TransitStopFacility.class),node.getCoord(), false);

			stop.setLinkId(Id.create(nodeId, Link.class));
//
//			Link link = network.getLinks().get(Id.createLinkId(nodeId + "-" + (nodeId + 1)));
//			if(link == null) {
//				link = network.getLinks().get(Id.createLinkId((nodeId - 1) + "-" + nodeId));
//			}
//			stop.setLinkId(link.getId());
			dummyScenario.getTransitSchedule().addStopFacility(stop);
		}

		if (!Files.exists(Path.of(utils.getInputDirectory()))) {
			Files.createDirectories(Path.of(utils.getInputDirectory()));
		}

		new TransitScheduleWriter(dummyScenario.getTransitSchedule()).writeFile(stopsInputFileName);
	}



	Config createTestConfig() {
		final Config config = ConfigUtils.createConfig();

		config.controller().setLastIteration(0);
		config.controller().setOutputDirectory(utils.getOutputDirectory());
		config.controller().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);

		config.routing().setRoutingRandomness(0.);

		return config;
	}


	Scenario createTestScenario(final Config config, Network network) {
		MutableScenario scenario = (MutableScenario) ScenarioUtils.loadScenario(config);
		scenario.setNetwork(network);

		final ActivityFacilities opportunities = scenario.getActivityFacilities();
		ActivityFacility facility1 = opportunities.getFactory().createActivityFacility(Id.create("1", ActivityFacility.class), new Coord(positionOfOpportunity, 0));
		opportunities.addActivityFacility(facility1);
		scenario.getConfig().facilities().setFacilitiesSource(FacilitiesConfigGroup.FacilitiesSource.setInScenario);
		return scenario;
	}


	/**
	 * This method creates a simple lin-based network used for EstimatedDrt accessibility tests
	 *
	 * @return the created test network
	 *
	 */
	public Network createLineNetwork(double carSpeed) {
		/*
		 * (0)----------(1)----------(2)----------(3)------x----(4)
		 * each "-" denotes 1000m
		 */
		double freespeed = carSpeed;
		double capacity = 500.;
		double numLanes = 1.;

		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());

		Network network = scenario.getNetwork();


		Set<String> modes = new HashSet<>();
		modes.add("car");

		// Nodes & Links
		Int2ObjectArrayMap<Node> nodeMap = new Int2ObjectArrayMap<>();
		Node prevNode = null;
		for (int i = 0; i < 5; i++) {
			Node node = NetworkUtils.createAndAddNode(network, Id.createNodeId(i), new Coord(i * gapBtwnNodes, 0));

			Link tinyLink = NetworkUtils.createAndAddLink(network, Id.createLinkId(i), node, node, 0, freespeed, capacity, numLanes);
			tinyLink.setAllowedModes(modes);

			if(prevNode != null) {

				Link linkForward = NetworkUtils.createAndAddLink(network, Id.createLinkId(prevNode.getId().toString() + "-" + i), prevNode, node, 1000., freespeed, capacity, numLanes);
				linkForward.setAllowedModes(modes);
				Link linkBackward = NetworkUtils.createAndAddLink(network, Id.createLinkId(i + "-" + prevNode.getId().toString()), node, prevNode, 1000., freespeed, capacity, numLanes);
				linkBackward.setAllowedModes(modes);
			}

			prevNode = node;
			nodeMap.put(i, node);
		}

		return network;
	}


	static class ResultsComparator implements FacilityDataExchangeInterface{
		private final Map<Tuple<ActivityFacility, Double>, Map<String,Double>> accessibilitiesMap = new HashMap<>() ;


		public Map<Tuple<ActivityFacility, Double>, Map<String, Double>> getAccessibilitiesMap() {
			return accessibilitiesMap;
		}

		@Override
		public void setFacilityAccessibilities(ActivityFacility measurePoint, Double timeOfDay, String mode, double accessibility) {
			Tuple<ActivityFacility, Double> key = new Tuple<>(measurePoint, timeOfDay);
			if (!accessibilitiesMap.containsKey(key)) {
				Map<String,Double> accessibilitiesByMode = new HashMap<>();
				accessibilitiesMap.put(key, accessibilitiesByMode);
			}
			accessibilitiesMap.get(key).put(mode, accessibility);
		}

		@Override
		public void finish() {

		}
	}

//	void createCongestedEventsFile(String congestedFileName, double congestionTime){
//		final Config config = createTestConfig();
//		config.controller().setLastIteration(1);
//		config.replanning().addStrategySettings(new ReplanningConfigGroup.StrategySettings().setStrategyName("ChangeExpBeta").setWeight(1.));
//		config.scoring().addActivityParams(new ScoringConfigGroup.ActivityParams("dummy").setTypicalDuration(60));
//
//
//		final Scenario scenario = createTestScenario(config,);
//
//		// ---
//
//		Random rnd = new Random();
//		PopulationFactory pf = scenario.getPopulation().getFactory();
//		for (int i = 0; i < 1000; i++) {
//			Person person = pf.createPerson(Id.createPersonId(i));
//			Plan plan = pf.createPlan();
//			Activity home = pf.createActivityFromCoord("dummy", new Coord(rnd.nextInt(200), rnd.nextInt(200)));
//			home.setEndTime(congestionTime);
//			Leg leg = pf.createLeg(TransportMode.car);
//			Activity work = pf.createActivityFromCoord("dummy", new Coord(rnd.nextInt(200), rnd.nextInt(200)));
//			plan.addActivity(home);
//			plan.addLeg(leg);
//			plan.addActivity(work);
//			person.addPlan(plan);
//			scenario.getPopulation().addPerson(person);
//		}
//
//
//		Controler controler = new Controler(scenario);
//		controler.run();
//
//		try {
//			Files.copy(Path.of(utils.getOutputDirectory() + "output_events.xml.gz"),Path.of(congestedFileName), StandardCopyOption.REPLACE_EXISTING);
//		} catch (IOException e) {
//			throw new RuntimeException(e);
//		}
//
//	}
}
