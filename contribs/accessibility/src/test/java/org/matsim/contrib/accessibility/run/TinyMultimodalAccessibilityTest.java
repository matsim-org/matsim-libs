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

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.accessibility.AccessibilityConfigGroup;
import org.matsim.contrib.accessibility.AccessibilityConfigGroup.AreaOfAccesssibilityComputation;
import org.matsim.contrib.accessibility.AccessibilityModule;
import org.matsim.contrib.accessibility.Modes4Accessibility;
import org.matsim.contrib.accessibility.interfaces.FacilityDataExchangeInterface;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.FacilitiesConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.testcases.MatsimTestUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A small test that enables to easily compare results with hand-computed results.
 *
 * @author dziemke
 */
public class TinyMultimodalAccessibilityTest {

	private static final Logger LOG = Logger.getLogger(TinyMultimodalAccessibilityTest.class);

	@Rule public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void testWithBoundingBox() {
		final Config config = createTestConfig();

		double min = 0.; // Values for bounding box usually come from a config file
		double max = 400.;

		AccessibilityConfigGroup acg = ConfigUtils.addOrGetModule(config, AccessibilityConfigGroup.class) ;
		acg.setAreaOfAccessibilityComputation(AreaOfAccesssibilityComputation.fromBoundingBox);
		acg.setBoundingBoxBottom(min);
		acg.setBoundingBoxTop(max);
		acg.setBoundingBoxLeft(min);
		acg.setBoundingBoxRight(max);

		final Scenario sc = createTestScenario(config);

		Controler controler = new Controler(sc);

		final AccessibilityModule module = new AccessibilityModule();
		final ResultsComparator resultsComparator = new ResultsComparator();
		module.addFacilityDataExchangeListener(resultsComparator);
		controler.addOverridingModule(module);

		controler.run();
	}


	private Config createTestConfig() {
		final Config config = ConfigUtils.createConfig();

		final AccessibilityConfigGroup acg = ConfigUtils.addOrGetModule(config, AccessibilityConfigGroup.class);
		acg.setTileSize_m(100);
		acg.setComputingAccessibilityForMode(Modes4Accessibility.freespeed, true);
		acg.setComputingAccessibilityForMode(Modes4Accessibility.car, true);
		//acg.setComputingAccessibilityForMode(Modes4Accessibility.pt, true);
		acg.setUseParallelization(false); // TODO Needed as long as threads don't reliably forward errors

		config.controler().setLastIteration(0);
		config.controler().setOutputDirectory(utils.getOutputDirectory());
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);

		return config;
	}


	private static Scenario createTestScenario(final Config config) {
//		final Scenario scenario = ScenarioUtils.loadScenario(config);
		MutableScenario scenario = (MutableScenario) ScenarioUtils.loadScenario(config);
		Network network = createLessSymmetricMultimodalTestNetwork();
		scenario.setNetwork(network);

		// Creating test opportunities (facilities); one on each link with same ID as link and coord on center of link
		final ActivityFacilities opportunities = scenario.getActivityFacilities();
		ActivityFacility facility1 = opportunities.getFactory().createActivityFacility(Id.create("1", ActivityFacility.class), new Coord(200, 0));
		opportunities.addActivityFacility(facility1);
		ActivityFacility facility2 = opportunities.getFactory().createActivityFacility(Id.create("2", ActivityFacility.class), new Coord(200, 200));
		opportunities.addActivityFacility(facility2);
		//ActivityFacility facility3 = opportunities.getFactory().createActivityFacility(Id.create("3", ActivityFacility.class), new Coord(280, 40));
		//opportunities.addActivityFacility(facility3);
		scenario.getConfig().facilities().setFacilitiesSource(FacilitiesConfigGroup.FacilitiesSource.setInScenario);
		return scenario;
	}


	/**
	 * This method creates a test network. It is used for example in PtMatrixTest.java to test the pt simulation in MATSim.
	 * The network has 9 nodes and 8 links (see the sketch below).
	 *
	 * @return the created test network
	 *
	 * @author thomas
	 * @author tthunig
	 */
	public static Network createLessSymmetricMultimodalTestNetwork() {
		/*
		 * (2)		(5)------(8)
		 * 	|		 |
		 * 	|		 |
		 * (1)------(4)------(7)
		 * 	|		 |
		 * 	| (10)---|--(11)---------(12)
		 * (3)		(6)------(9)
		 */
		double freespeed = 2.7;
		double capacity = 500.;
		double numLanes = 1.;

		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());

		Network network = (Network) scenario.getNetwork();

		// Nodes
		Node node1 = NetworkUtils.createAndAddNode(network, Id.create(1, Node.class), new Coord((double) 10, (double) 100));
		Node node2 = NetworkUtils.createAndAddNode(network, Id.create(2, Node.class), new Coord((double) 10, (double) 200));
		Node node3 = NetworkUtils.createAndAddNode(network, Id.create(3, Node.class), new Coord((double) 10, (double) 0));
		Node node4 = NetworkUtils.createAndAddNode(network, Id.create(4, Node.class), new Coord((double) 110, (double) 100));
		Node node5 = NetworkUtils.createAndAddNode(network, Id.create(5, Node.class), new Coord((double) 110, (double) 200));
		Node node6 = NetworkUtils.createAndAddNode(network, Id.create(6, Node.class), new Coord((double) 110, (double) 0));
		Node node7 = NetworkUtils.createAndAddNode(network, Id.create(7, Node.class), new Coord((double) 190, (double) 100));
		Node node8 = NetworkUtils.createAndAddNode(network, Id.create(8, Node.class), new Coord((double) 190, (double) 200));
		Node node9 = NetworkUtils.createAndAddNode(network, Id.create(9, Node.class), new Coord((double) 190, (double) 0));
		Node node10 = NetworkUtils.createAndAddNode(network, Id.create(10, Node.class), new Coord((double) 40, (double) 40));
		Node node11 = NetworkUtils.createAndAddNode(network, Id.create(11, Node.class), new Coord((double) 140, (double) 40));
		Node node12 = NetworkUtils.createAndAddNode(network, Id.create(12, Node.class), new Coord((double) 270, (double) 40));

		Set<String> modes = new HashSet<>();
		modes.add("car");

		// Links (bi-directional)
		NetworkUtils.createAndAddLink(network,Id.create(1, Link.class), node1, node2, (double) 100, freespeed, capacity, numLanes);
		network.getLinks().get(Id.create(1, Link.class)).setAllowedModes(modes);

		NetworkUtils.createAndAddLink(network,Id.create(2, Link.class), node2, node1, (double) 100, freespeed, capacity, numLanes);
		network.getLinks().get(Id.create(2, Link.class)).setAllowedModes(modes);

		NetworkUtils.createAndAddLink(network,Id.create(3, Link.class), node1, node3, (double) 100, freespeed, capacity, numLanes);
		network.getLinks().get(Id.create(3, Link.class)).setAllowedModes(modes);

		NetworkUtils.createAndAddLink(network,Id.create(4, Link.class), node3, node1, (double) 100, freespeed, capacity, numLanes);
		network.getLinks().get(Id.create(4, Link.class)).setAllowedModes(modes);

		NetworkUtils.createAndAddLink(network,Id.create(5, Link.class), node1, node4, (double) 100, freespeed, capacity, numLanes);
		network.getLinks().get(Id.create(5, Link.class)).setAllowedModes(modes);

		NetworkUtils.createAndAddLink(network,Id.create(6, Link.class), node4, node1, (double) 100, freespeed, capacity, numLanes);
		network.getLinks().get(Id.create(6, Link.class)).setAllowedModes(modes);

		NetworkUtils.createAndAddLink(network,Id.create(7, Link.class), node4, node5, (double) 100, freespeed, capacity, numLanes);
		network.getLinks().get(Id.create(7, Link.class)).setAllowedModes(modes);

		NetworkUtils.createAndAddLink(network,Id.create(8, Link.class), node5, node4, (double) 100, freespeed, capacity, numLanes);
		network.getLinks().get(Id.create(8, Link.class)).setAllowedModes(modes);

		NetworkUtils.createAndAddLink(network,Id.create(9, Link.class), node4, node6, (double) 100, freespeed, capacity, numLanes);
		network.getLinks().get(Id.create(9, Link.class)).setAllowedModes(modes);

		NetworkUtils.createAndAddLink(network,Id.create(10, Link.class), node6, node4, (double) 100, freespeed, capacity, numLanes);
		network.getLinks().get(Id.create(10, Link.class)).setAllowedModes(modes);

		NetworkUtils.createAndAddLink(network,Id.create(11, Link.class), node4, node7, (double) 80, freespeed, capacity, numLanes);
		network.getLinks().get(Id.create(11, Link.class)).setAllowedModes(modes);

		NetworkUtils.createAndAddLink(network,Id.create(12, Link.class), node7, node4, (double) 80, freespeed, capacity, numLanes);
		network.getLinks().get(Id.create(12, Link.class)).setAllowedModes(modes);

		NetworkUtils.createAndAddLink(network,Id.create(13, Link.class), node5, node8, (double) 80, freespeed, capacity, numLanes);
		network.getLinks().get(Id.create(13, Link.class)).setAllowedModes(modes);

		NetworkUtils.createAndAddLink(network,Id.create(14, Link.class), node8, node5, (double) 80, freespeed, capacity, numLanes);
		network.getLinks().get(Id.create(14, Link.class)).setAllowedModes(modes);

		NetworkUtils.createAndAddLink(network,Id.create(15, Link.class), node6, node9, (double) 80, freespeed, capacity, numLanes);
		network.getLinks().get(Id.create(15, Link.class)).setAllowedModes(modes);

		NetworkUtils.createAndAddLink(network,Id.create(16, Link.class), node9, node6, (double) 80, freespeed, capacity, numLanes);
		network.getLinks().get(Id.create(16, Link.class)).setAllowedModes(modes);

		Set<String> ptModes = new HashSet<>();
		ptModes.add("pt");

		NetworkUtils.createAndAddLink(network,Id.create(17, Link.class), node10, node11, (double) 100, freespeed, capacity, numLanes);
		network.getLinks().get(Id.create(17, Link.class)).setAllowedModes(ptModes);
		Link link = network.getLinks().get(Id.create(17, Link.class));
		LOG.error("Link " + link.getId() + " has following allowed modes: " + link.getAllowedModes());

		NetworkUtils.createAndAddLink(network,Id.create(18, Link.class), node11, node10, (double) 100, freespeed, capacity, numLanes);
		network.getLinks().get(Id.create(18, Link.class)).setAllowedModes(ptModes);

		NetworkUtils.createAndAddLink(network,Id.create(19, Link.class), node11, node12, (double) 130, freespeed, capacity, numLanes);
		network.getLinks().get(Id.create(19, Link.class)).setAllowedModes(ptModes);

		NetworkUtils.createAndAddLink(network,Id.create(20, Link.class), node12, node11, (double) 130, freespeed, capacity, numLanes);
		network.getLinks().get(Id.create(20, Link.class)).setAllowedModes(ptModes);

		return network;
	}


	static class ResultsComparator implements FacilityDataExchangeInterface{
		private Map<Tuple<ActivityFacility, Double>, Map<String,Double>> accessibilitiesMap = new HashMap<>() ;

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
			for (Tuple<ActivityFacility, Double> tuple : accessibilitiesMap.keySet()) {
				LOG.error("CHECK X = " + tuple.getFirst().getCoord().getX() + " -- Y = " + tuple.getFirst().getCoord().getY() + " -- value = " + accessibilitiesMap.get(tuple).get(TransportMode.car));
				if (tuple.getFirst().getCoord().getX() == 50.) {
					if (tuple.getFirst().getCoord().getY() == 50. || tuple.getFirst().getCoord().getY() == 150.) {
						Assert.assertEquals(0.08573977315253786, accessibilitiesMap.get(tuple).get("freespeed"), MatsimTestUtils.EPSILON);
					}
				}
				if (tuple.getFirst().getCoord().getX() == 150.) {
					if (tuple.getFirst().getCoord().getY() == 50. || tuple.getFirst().getCoord().getY() == 150.) {
						Assert.assertEquals(0.2091965632759945, accessibilitiesMap.get(tuple).get("freespeed"), MatsimTestUtils.EPSILON);
					}
				}
			}
		}
	}
}