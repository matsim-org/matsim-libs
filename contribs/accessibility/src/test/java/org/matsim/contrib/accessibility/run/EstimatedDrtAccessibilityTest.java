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
import java.util.*;
/*
 * A small test to test the functionality of the EstimatedDrtContributionCalculator.java
 * We are comparing calculators outputs to hand-computed results. We will test the following:
 * - The required output files are created --> runFromEvents, part e
 * - Calculation of walk is correct --> runFromEvents, part a
 * - Calculation of DRT accessibility is correct --> runFromEvents, part b & c
 * - Agent will fall back to walk if walk provides higher accessibility --> runFromEvents, part d
 * - Agent will fall back to walk if origin and destination stops of the DRT trip are identical
 * - The wait time and detour factors are included in DRT calculation --> testDetourFactorsAndWaitTimeAndDistanceCosts
 * - The distance costs are included in DRT calculation --> testDetourFactorsAndWaitTimeAndDistanceCosts
 *
 *
 * Following Network will be used
 * (0)-----!----------!-----(1)-----!----------!-----(2)-----!----------!-----(3)-----!--------x--!-----(4)
 * (#) denotes nodes - distance between each node is 1000m (each "-" denotes 50m)
 * ! denotes location of DRT stops
 * x denotes our opportunity
 *
 * @author jakobrehmann
 */

public class EstimatedDrtAccessibilityTest {

	@RegisterExtension
	private static final MatsimTestUtils utils = new MatsimTestUtils();
	public static double gapBtwnNodes = 1000.;
	public double carSpeed = 50/3.6;


	String emptyEventsFileName;

	String congestedEventsFileName;

	// Set Parameters
	public int drtWaitTime = 300;
	public int positionOfOpportunity = 2600;

	@BeforeEach
	void prepare() {
		emptyEventsFileName = utils.getClassInputDirectory() + "output_events.xml.gz";
		congestedEventsFileName = utils.getClassInputDirectory() + "output_events_congested.xml.gz";

	}

	/**
	 * Tests walk and drt calculations; also tests that walk is used if walk provides higher accessibility. Finally, tests that output files are generated.
	 */
	@Test
	void runFromEvents() {
		// Create Network
		Network network = createLineNetwork();

		Config config = createTestConfig(network);

		final Scenario scenario = createTestScenario(config, network);

		// Set up DRT estimator

		DrtEstimator drtEstimator = new DirectTripBasedDrtEstimator.Builder()
			.setWaitingTimeEstimator(new ConstantWaitingTimeEstimator(drtWaitTime))
			.setWaitingTimeDistributionGenerator(new NoDistribution())
			.setRideDurationEstimator(new ConstantRideDurationEstimator(1, 0))
			.setRideDurationDistributionGenerator(new NoDistribution())
			.build();

		// Run Accessibility Analysis
		AccessibilityFromEvents.Builder builder = new AccessibilityFromEvents.Builder(scenario, emptyEventsFileName);
		TinyAccessibilityTest.ResultsComparator dataListener = new TinyAccessibilityTest.ResultsComparator();
		builder.addDataListener(dataListener);
		builder.setDrtEstimator(drtEstimator);

		builder.build().run();


		// ------------
		// Check results
		// ------------

		// A) First we will check the accessibility for teleportedWalk. This is important, because teleportedWalk is the fallback
		// mode for DRT, so we need to check these calculations first.
		Map<Tuple<ActivityFacility, Double>, Map<String, Double>> accessibilitiesMap = dataListener.getAccessibilitiesMap();
		Double walkBeelineFactor = config.routing().getBeelineDistanceFactors().get("walk");
		Double walkSpeed = config.routing().getTeleportedModeSpeeds().get("walk");
		int marginalUtilityOfTime_s = -12;
		for (Map.Entry<Tuple<ActivityFacility, Double>, Map<String, Double>> tupleMapEntry : accessibilitiesMap.entrySet()) {
			double dist = Math.abs(positionOfOpportunity - tupleMapEntry.getKey().getFirst().getCoord().getX()) * walkBeelineFactor;
			// time converted into hours x -12
			double accessibilityShould = dist / walkSpeed / 3600 * marginalUtilityOfTime_s;
			// we compare this "hand-calculated" value with the result from the accessibility analysis
			double accessibilityActual = tupleMapEntry.getValue().get(Modes4Accessibility.teleportedWalk.name());
			Assertions.assertEquals(accessibilityShould, accessibilityActual, MatsimTestUtils.EPSILON, "Unexpected accessibility for walk at " + tupleMapEntry.getKey().getFirst().getCoord().getX());
		}


		// B) Test that agent walks backwards (away from opportunity) to reach closest DRT stop. And check that a direct walk would be worse than DRT in this case.
		// @ Measuring point (250,0):
		// Access Walk: we expect agent to walk 250 to closest DRT stop (back to node 0)
		// DRT trip is 3000m @ 2.7m/s + waiting time of 300 seconds. We assume -12 (=-6 -6) utils/hr.
		// Egress walk should be 400m x 1.3 (beeline factor) and walk speed of 0.83 m/s.
		int egressWalkDist = 400;
		int accessWalkDist = 250;
		{
			int x = 250;
			Map<String, Double> accMap = accessibilitiesMap.entrySet().stream().filter(entry -> entry.getKey().getFirst().getCoord().equals(new Coord(x, 0))).map(Map.Entry::getValue).findFirst().get();
			double accessWalkUtility = accessWalkDist * walkBeelineFactor / walkSpeed / 3600 * marginalUtilityOfTime_s;
			// Note Method "addLink" in SpeedyGraphBuilder rounds two 2 decimal places; if we don't do that, the difference exceeds our EPSILON
			int drtTravelDist = 3000;
			double drtTripUtility = (Math.round(drtTravelDist / carSpeed * 100.) / 100. + drtWaitTime) / 3600 * marginalUtilityOfTime_s;
			double egressWalkUtility = egressWalkDist * walkBeelineFactor / walkSpeed / 3600 * marginalUtilityOfTime_s;

			double drtUtilityShould = accessWalkUtility + drtTripUtility + egressWalkUtility;
			double drtUtilityActual = accMap.get(Modes4Accessibility.estimatedDrt.name());
			double walkUtilityActual = accMap.get(Modes4Accessibility.teleportedWalk.name());
			Assertions.assertEquals(drtUtilityShould, drtUtilityActual, MatsimTestUtils.EPSILON);
			Assertions.assertTrue(drtUtilityActual > walkUtilityActual);
		}
		// C) Test that agent walks forwards (towards opportunity) to reach closest DRT stop. And check that a direct walk would be worse than DRT in this case.
		// @ Measuring point (750,0): we expect agent to walk forwards to reach closest DRT stop
		// Access Walk: we expect agent to walk 250 to closest DRT stop (forward to node 1)
		// DRT trip is 2000m @ 2.7m/s + waiting time of 300 seconds. We assume -12 (=-6 -6) utils/hr.
		// Egress walk should be 400m x 1.3 (beeline factor) and walk speed of 0.83 m/s.
		{
			int x = 750;
			Map<String, Double> accMap = accessibilitiesMap.entrySet().stream().filter(entry -> entry.getKey().getFirst().getCoord().equals(new Coord(x, 0))).map(Map.Entry::getValue).findFirst().get();
			double accessWalkUtility = accessWalkDist * walkBeelineFactor / walkSpeed / 3600 * marginalUtilityOfTime_s;
			int drtTravelDist = 2000;
			double drtTripUtility = (Math.round(drtTravelDist / carSpeed * 100.) / 100. + drtWaitTime) / 3600 * marginalUtilityOfTime_s;
			double egressWalkUtility = egressWalkDist * walkBeelineFactor / walkSpeed / 3600 * marginalUtilityOfTime_s;

			double drtUtilityShould = accessWalkUtility + drtTripUtility + egressWalkUtility;
			double drtUtilityActual = accMap.get(Modes4Accessibility.estimatedDrt.name());
			double walkUtilityActual = accMap.get(Modes4Accessibility.teleportedWalk.name());
			Assertions.assertEquals(drtUtilityShould, drtUtilityActual, MatsimTestUtils.EPSILON);
			Assertions.assertTrue(drtUtilityActual > walkUtilityActual);
		}

		// D) Test situation where agent falls back to walk because walk is better
		// @ Measuring point (2250,0): we expect agent to directly walk to the destination
		// Access Walk: we expect agent to walk 250 to closest DRT stop (forward to backward to node 2)
		// DRT trip is 1000m @ 2.7m/s + waiting time of 300 seconds. We assume -12 (=-6 -6) utils/hr.
		// Egress walk should be 400m x 1.3 (beeline factor) and walk speed of 0.83 m/s
		// Since direct walk accessibility is higher than DRT accessibility (only 350m direct walk), walk will be chosen

		{
			int x = 2250;
			Map<String, Double> accMap = accessibilitiesMap.entrySet().stream().filter(entry -> entry.getKey().getFirst().getCoord().equals(new Coord(x, 0))).map(Map.Entry::getValue).findFirst().get();
			double accessWalkUtility = accessWalkDist * walkBeelineFactor / walkSpeed / 3600 * marginalUtilityOfTime_s;
			int drtTravelDist = 1000;
			double drtTripUtility = (Math.round(drtTravelDist / carSpeed * 100.) / 100. + drtWaitTime) / 3600 * marginalUtilityOfTime_s;
			double egressWalkUtility = egressWalkDist * walkBeelineFactor / walkSpeed / 3600 * marginalUtilityOfTime_s;

			double drtUtilityShould = accessWalkUtility + drtTripUtility + egressWalkUtility;
			// I do not have to recalculate walk by hand, since we checked the calculation in part a of this test.
			double walkUtilityShould = accMap.get(Modes4Accessibility.teleportedWalk.name());
			Assertions.assertTrue(walkUtilityShould > drtUtilityShould);

			// Since walk is better, walk is chosen:
			double drtUtilityActual = accMap.get(Modes4Accessibility.estimatedDrt.name());
			double walkUtilityActual = accMap.get(Modes4Accessibility.teleportedWalk.name());
			Assertions.assertEquals(walkUtilityActual, drtUtilityActual, MatsimTestUtils.EPSILON);

		}

		// E) Test that output files are created:
		Path pathToFolder = Path.of(utils.getOutputDirectory() + "analysis/accessibility/null/");

		Assertions.assertTrue(Files.exists(pathToFolder), "Expected output folder was not created: " + pathToFolder);
		Assertions.assertTrue(Files.exists(pathToFolder.resolve("accessibilities.csv")), "Expected output file accessibilities.csv was not created in" + pathToFolder);
		Assertions.assertTrue(Files.exists(pathToFolder.resolve("pois.csv")), "Expected output file pois.csv was not created in" + pathToFolder);
		Assertions.assertTrue(Files.exists(pathToFolder.resolve("configUsedForAccessibilityComputation.xml")), "Expected output file configUsedForAccessibilityComputation.xml was not created in" + pathToFolder);

	}

	/**
	 * Tests that EstimatedDrtContributionCalculator is sensitive to detour factors (both intercept and slope), waiting time, and distance
	 * based disutility.
	 */
	@Test
	void testDetourFactorsAndWaitTimeAndDistanceCosts() {

		Network network = createLineNetwork();

		Config config = createTestConfig(network);

		config.scoring().getModes().get(TransportMode.drt).setMarginalUtilityOfDistance(-0.001);


		// Create  Scenario
		final Scenario scenario = createTestScenario(config, network);

		// Set up DRT estimator

		DrtEstimator drtEstimator = new DirectTripBasedDrtEstimator.Builder()
			.setWaitingTimeEstimator(new ConstantWaitingTimeEstimator(drtWaitTime))
			.setWaitingTimeDistributionGenerator(new NoDistribution())
			.setRideDurationEstimator(new ConstantRideDurationEstimator(1.5, 30))
			.setRideDurationDistributionGenerator(new NoDistribution())
			.build();

		// Run Accessibility Analysis
		AccessibilityFromEvents.Builder builder = new AccessibilityFromEvents.Builder(scenario, emptyEventsFileName);
		TinyAccessibilityTest.ResultsComparator dataListener = new TinyAccessibilityTest.ResultsComparator();
		builder.addDataListener(dataListener);
		builder.setDrtEstimator(drtEstimator);

		builder.build().run();


		// ------------
		// Check results
		// ------------

		Map<Tuple<ActivityFacility, Double>, Map<String, Double>> accessibilitiesMap = dataListener.getAccessibilitiesMap();
		Double walkBeelineFactor = config.routing().getBeelineDistanceFactors().get("walk");
		Double walkSpeed = config.routing().getTeleportedModeSpeeds().get("walk");

		// Test that wait time and detour factors are included in DRT Accessibility calculation.
		// @ Measuring point (250,0):
		// Access Walk: we expect agent to walk 250 to closest DRT stop (back to node 0)
		// DRT trip is (3000m @ 2.7m/s) * 1.5 for detour factor alpha+ 30 second for detour factor beta  + waiting time of 300 seconds. We assume -12 (=-6 -6) utils/hr.
		// Distance based disutility is 3000m x -.001 utils/m. (Note: for distance disutility we do not include detours)
		// Egress walk should be 400m x 1.3 (beeline factor) and walk speed of 0.83 m/s.
		{
			int x = 250;
			Map<String, Double> accMap = accessibilitiesMap.entrySet().stream().filter(entry -> entry.getKey().getFirst().getCoord().equals(new Coord(x, 0))).map(Map.Entry::getValue).findFirst().get();
			double accessWalkUtility = 250 * walkBeelineFactor / walkSpeed / 3600 * -12;
			// Note Method "addLink" in SpeedyGraphBuilder also rounds two 2 decimal places; if we don't do that, the difference exceeds our EPSILON
			double drtTripTimeUtility = (Math.round(3000 / carSpeed * 100.) * 1.5 / 100. + 30 + drtWaitTime) / 3600 * -12;
			double drtTripDistanceUtility = 3000 * -.001;
			double drtTripUtility = drtTripTimeUtility + drtTripDistanceUtility;
			double egressWalkUtility = 400 * walkBeelineFactor / walkSpeed / 3600 * -12;

			double drtUtilityShould = accessWalkUtility + drtTripUtility + egressWalkUtility;
			double drtUtilityActual = accMap.get(Modes4Accessibility.estimatedDrt.name());
			Assertions.assertEquals(drtUtilityShould, drtUtilityActual, MatsimTestUtils.EPSILON);
		}

	}

	/**
	 * Tests that EstimatedDrtContributionCalculator is sensitive to detour factors (both intercept and slope), waiting time, and distance
	 * based disutility. We test this by setting the ASC to postive 10. Thus, the DRT trip is guaranteed to provide a higher accessibilty.
	 * And then we examine a measuring point that shares the same DRT stop as the opportunity, to check that we fallback to walk.
	 */
	@Test
	void testFallbackWalkIfDrtStopsSame() {

		Network network = createLineNetwork();

		Config config = createTestConfig(network);

		config.scoring().getModes().get(TransportMode.drt).setConstant(10);


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
		AccessibilityFromEvents.Builder builder = new AccessibilityFromEvents.Builder(scenario, emptyEventsFileName);
		TinyAccessibilityTest.ResultsComparator dataListener = new TinyAccessibilityTest.ResultsComparator();
		builder.addDataListener(dataListener);
		builder.setDrtEstimator(drtEstimator);

		builder.build().run();


		// ------------
		// Check results
		// ------------

		Map<Tuple<ActivityFacility, Double>, Map<String, Double>> accessibilitiesMap = dataListener.getAccessibilitiesMap();
		Double walkBeelineFactor = config.routing().getBeelineDistanceFactors().get("walk");
		Double walkSpeed = config.routing().getTeleportedModeSpeeds().get("walk");

		// Test that walk is chosen, even though accessibilty with DRT is better.
		// @ Measuring point (3250,0):
		// Access Walk: we expect agent to walk 250 to closest DRT stop (back to node 0)
		// DRT trip is (3000m @ 2.7m/s) * 1.5 for detour factor alpha+ 30 second for detour factor beta  + waiting time of 300 seconds. We assume -12 (=-6 -6) utils/hr.
		// Distance based disutility is 3000m x -.001 utils/m. (Note: for distance disutility we do not include detours)
		// Egress walk should be 400m x 1.3 (beeline factor) and walk speed of 0.83 m/s.
		{
			int x = 3250;
			Map<String, Double> accMap = accessibilitiesMap.entrySet().stream().filter(entry -> entry.getKey().getFirst().getCoord().equals(new Coord(x, 0))).map(Map.Entry::getValue).findFirst().get();
			double accessWalkUtility = 250 * walkBeelineFactor / walkSpeed / 3600 * -12;
			// Note Method "addLink" in SpeedyGraphBuilder also rounds two 2 decimal places; if we don't do that, the difference exceeds our EPSILON
			double drtTripUtility = 10 + (Math.round(0 / carSpeed * 100.) / 100.  + drtWaitTime) / 3600 * -12;
			double egressWalkUtility = 400 * walkBeelineFactor / walkSpeed / 3600 * -12;

			double drtUtilityShould = accessWalkUtility + drtTripUtility + egressWalkUtility;
			double walkShould = (250+400)*walkBeelineFactor / walkSpeed / 3600 * -12;
			// DRT should provide higher accessibility because of the +10 bonus
			Assertions.assertTrue(walkShould < drtUtilityShould);

			// Regardless, because the pickup stop and dropoff stop are the same, walk will be used
			double drtUtilityActual = accMap.get(Modes4Accessibility.estimatedDrt.name());
			Assertions.assertEquals(walkShould, drtUtilityActual, MatsimTestUtils.EPSILON);
		}

	}


	private void createDrtStopsFile(String stopsInputFileName, Network network) throws IOException {
		Scenario dummyScenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		TransitScheduleFactory tf = dummyScenario.getTransitSchedule().getFactory();


		for (Node node : network.getNodes().values()) {

			int nodeId = Integer.parseInt(node.getId().toString());

			TransitStopFacility stop = tf.createTransitStopFacility(Id.create(nodeId, TransitStopFacility.class),node.getCoord(), false);

			stop.setLinkId(Id.create(nodeId, Link.class));
			dummyScenario.getTransitSchedule().addStopFacility(stop);
		}

		if (!Files.exists(Path.of(utils.getInputDirectory()))) {
			Files.createDirectories(Path.of(utils.getInputDirectory()));
		}

		new TransitScheduleWriter(dummyScenario.getTransitSchedule()).writeFile(stopsInputFileName);
	}



	Config createTestConfig(Network network) {
		final Config config = ConfigUtils.createConfig();

		// General
		config.controller().setLastIteration(0);
		config.controller().setOutputDirectory(utils.getOutputDirectory());
		config.controller().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);

		config.routing().setRoutingRandomness(0.);


		// Accessibility Config Group
		AccessibilityConfigGroup acg = ConfigUtils.addOrGetModule(config, AccessibilityConfigGroup.class);
		acg.setAreaOfAccessibilityComputation(AreaOfAccesssibilityComputation.fromBoundingBox);

		acg.setBoundingBoxBottom(-250).setBoundingBoxTop(250).setBoundingBoxLeft(0).setBoundingBoxRight(4000);
		acg.setTileSize_m(500);

		for (Modes4Accessibility mode : Modes4Accessibility.values()) {
			acg.setComputingAccessibilityForMode(mode, mode.equals(Modes4Accessibility.estimatedDrt) || mode.equals(Modes4Accessibility.teleportedWalk));
		}

		acg.setUseParallelization(false);


		// DRT
		// DRT: Scoring Params
		ScoringConfigGroup.ModeParams drtScoringParams = new ScoringConfigGroup.ModeParams(TransportMode.drt);
		config.scoring().addModeParams(drtScoringParams);

		// DRT: DVRP Config Group
		ConfigUtils.addOrGetModule(config, DvrpConfigGroup.class );

		// DRT: MultiModeDrtConfigGroup + DrtConfigGroup

		String stopsInputFileName = utils.getClassInputDirectory() + "drtStops.xml";

		try {
			createDrtStopsFile(stopsInputFileName, network);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}



		DrtConfigGroup drtConfigGroup = new DrtConfigGroup();
		drtConfigGroup.setOperationalScheme(DrtConfigGroup.OperationalScheme.stopbased);
		drtConfigGroup.setTransitStopFile( stopsInputFileName);

		drtConfigGroup.addOrGetDrtOptimizationConstraintsParams().addOrGetDefaultDrtOptimizationConstraintsSet().setMaxWalkDistance(1000);


		MultiModeDrtConfigGroup multiModeDrtConfigGroup = new MultiModeDrtConfigGroup();
		multiModeDrtConfigGroup.addParameterSet(drtConfigGroup);
		config.addModule(multiModeDrtConfigGroup);
		config.addModule(drtConfigGroup);


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
	public Network createLineNetwork() {
		/*
		 * (0)----------(1)----------(2)----------(3)----------(4)
		 * each "-" denotes 100m
		 */
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

			Link tinyLink = NetworkUtils.createAndAddLink(network, Id.createLinkId(i), node, node, 0, carSpeed, capacity, numLanes);
			tinyLink.setAllowedModes(modes);

			if(prevNode != null) {

				Link linkForward = NetworkUtils.createAndAddLink(network, Id.createLinkId(prevNode.getId().toString() + "-" + i), prevNode, node, 1000., carSpeed, capacity, numLanes);
				linkForward.setAllowedModes(modes);
				Link linkBackward = NetworkUtils.createAndAddLink(network, Id.createLinkId(i + "-" + prevNode.getId().toString()), node, prevNode, 1000., carSpeed, capacity, numLanes);
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

}
