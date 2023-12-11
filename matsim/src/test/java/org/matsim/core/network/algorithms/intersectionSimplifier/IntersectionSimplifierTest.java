/* *********************************************************************** *
 * project: org.matsim.*
 * NetworkSimplifierTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package org.matsim.core.network.algorithms.intersectionSimplifier;


import java.util.Iterator;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.NetworkCalcTopoType;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.network.algorithms.NetworkSimplifier;
import org.matsim.core.network.algorithms.intersectionSimplifier.containers.Cluster;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.testcases.MatsimTestUtils;

public class IntersectionSimplifierTest {
	@RegisterExtension private MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void testComplexIntersection() {
		Network network = null;
		try {
			network = buildComplexIntersection();
			new NetworkWriter(network).write(utils.getOutputDirectory() + "network.xml.gz");
		} catch (Exception e) {
			Assertions.fail("Should build and write without exception.");
		}

		Assertions.assertEquals(28, network.getNodes().size(), "Wrong number of nodes");
		Assertions.assertEquals(50, network.getLinks().size(), "Wrong number of links");
	}

	@Test
	void testSimplifyCallOnlyOnce() {
		Network network = buildComplexIntersection();
		IntersectionSimplifier is = new IntersectionSimplifier(10.0, 2);
		try{
			is.simplify(network);
			is.writeClustersToFile(utils.getOutputDirectory() + "clusters.csv");
		} catch(Exception e) {
			Assertions.fail("Should not throw exceptions when simplifying network.");
		}

		try {
			is.simplify(network);
			Assertions.fail("Should not allow to simplify a network more than once");
		} catch(Exception e) {
			/* Pass. */
			e.printStackTrace();
		}
	}


	@Test
	void testGetClusteredNode() {
		Network network = buildComplexIntersection();
		IntersectionSimplifier is = new IntersectionSimplifier(10.0, 2);

		/* Test before simplifying. */
		Node n = network.getNodes().get(Id.createNodeId(5));
		Assertions.assertNull(is.getClusteredNode(n), "Cannot be clustered before simplification was done.");

		/* Test after simplifying. */
		is.simplify(network);
		is.writeClustersToFile(utils.getOutputDirectory() + "clusters.csv");
		Node centroid = is.getClusteredNode(n);
		Assertions.assertNotNull(centroid, "Must be associated with a cluster.");

		Coord centroidCoord = CoordUtils.createCoord(85.0, 85.0);
		Assertions.assertTrue(centroidCoord.equals(centroid.getCoord()), "Wrong centroid Coord.");
		Assertions.assertTrue(centroid.getId().toString().contains("-"), "Wrong centroid Id (does not contain \"-\" separator between merged node ids).");
	}


	@Test
	void testSimplifyOne() {
		Network network = buildComplexIntersection();
		IntersectionSimplifier is = new IntersectionSimplifier(10.0, 2);
		Network simpleNetwork = is.simplify(network);
		is.writeClustersToFile(utils.getOutputDirectory() + "clusters.csv");
		List<Cluster> clusters = is.getClusters();
		Assertions.assertEquals(6l, clusters.size(), "Wrong number of clusters");

		/* Check some clusters. */
		Cluster c1 = findCluster(clusters, CoordUtils.createCoord(85.0, 85.0));
		Assertions.assertNotNull(c1, "Could not find a cluster with centroid (85.0,85.0)");
		Assertions.assertEquals(4, c1.getPoints().size(), "Wrong number of points");

		Cluster c2 = findCluster(clusters, CoordUtils.createCoord(225.0, 85.0));
		Assertions.assertNotNull(c2, "Could not find cluster with centroid (225.0,85.0)");
		Assertions.assertEquals(4, c2.getPoints().size(), "Wrong number of points");

		/* Write the cleaned network to file. */
		new NetworkWriter(simpleNetwork).write(utils.getOutputDirectory() + "cleanNetwork.xml");
	}

	@Test
	void testSimplifyTwo() {
		Network network = buildComplexIntersection();
		IntersectionSimplifier is = new IntersectionSimplifier(30.0, 4);
		Network simpleNetwork = is.simplify(network);
		is.writeClustersToFile(utils.getOutputDirectory() + "clusters.csv");
		List<Cluster> clusters = is.getClusters();
		Assertions.assertEquals(2l, clusters.size(), "Wrong number of clusters");

		/* Check some clusters. */
		Cluster c1 = findCluster(clusters, CoordUtils.createCoord(85.0, 85.0));
		Assertions.assertNotNull(c1, "Could not find cluster with centroid (85.0,85.0)");
		Assertions.assertEquals(4, c1.getPoints().size(), "Wrong number of points");

		Cluster c2 = findCluster(clusters, CoordUtils.createCoord(225.0, 85.0));
		Assertions.assertNotNull(c2, "Could not find cluster with centroid (225.0,85.0)");
		Assertions.assertEquals(12, c2.getPoints().size(), "Wrong number of points");

		/* Write the cleaned network to file. */
		new NetworkWriter(simpleNetwork).write(utils.getOutputDirectory() + "cleanNetwork.xml");
	}

	/**
	 * The network cleaner will/should ensure that full connectivity remains.
	 */
	@Test
	void testNetworkCleaner() {
		Network network = buildComplexIntersection();
		IntersectionSimplifier is = new IntersectionSimplifier(10.0, 2);
		Network simpleNetwork = is.simplify(network);
		is.writeClustersToFile(utils.getOutputDirectory() + "clusters.csv");
		new NetworkWriter(simpleNetwork).write(utils.getOutputDirectory() + "network1.xml");

		new NetworkCleaner().run(simpleNetwork);
		new NetworkWriter(simpleNetwork).write(utils.getOutputDirectory() + "network2.xml");

		Assertions.assertEquals(18l, simpleNetwork.getNodes().size(), "Wrong number of nodes.");

		Assertions.assertNotNull(simpleNetwork.getNodes().get(Id.createNodeId("1")), "Should find node '1'");
		Assertions.assertNotNull(simpleNetwork.getNodes().get(Id.createNodeId("3")), "Should find node '3'");
		Assertions.assertNotNull(simpleNetwork.getNodes().get(Id.createNodeId("10")), "Should find node '10'");
		Assertions.assertNotNull(simpleNetwork.getNodes().get(Id.createNodeId("11")), "Should find node '11'");
		Assertions.assertNotNull(simpleNetwork.getNodes().get(Id.createNodeId("26")), "Should find node '26'");
		Assertions.assertNotNull(simpleNetwork.getNodes().get(Id.createNodeId("28")), "Should find node '28'");

		Assertions.assertNotNull(simpleNetwork.getNodes().get(Id.createNodeId("2")), "Should find node '2'");
		Assertions.assertNotNull(simpleNetwork.getNodes().get(Id.createNodeId("4")), "Should find node '4'");
		Assertions.assertNotNull(simpleNetwork.getNodes().get(Id.createNodeId("9")), "Should find node '9'");
		Assertions.assertNotNull(simpleNetwork.getNodes().get(Id.createNodeId("12")), "Should find node '12'");
		Assertions.assertNotNull(simpleNetwork.getNodes().get(Id.createNodeId("25")), "Should find node '25'");
		Assertions.assertNotNull(simpleNetwork.getNodes().get(Id.createNodeId("27")), "Should find node '27'");

		Assertions.assertNotNull(simpleNetwork.getNodes().get(Id.createNodeId("5-6-7-8")), "Should find simplified node '5-6-7-8'");
		Assertions.assertNotNull(simpleNetwork.getNodes().get(Id.createNodeId("13-14")), "Should find simplified node '13-14'");
		Assertions.assertNotNull(simpleNetwork.getNodes().get(Id.createNodeId("16-17-20-21")), "Should find simplified node '16-17-20-21'");
		Assertions.assertNotNull(simpleNetwork.getNodes().get(Id.createNodeId("18-22")), "Should find simplified node '18-22'");
		Assertions.assertNotNull(simpleNetwork.getNodes().get(Id.createNodeId("15-19")), "Should find simplified node '15-19'");
		Assertions.assertNotNull(simpleNetwork.getNodes().get(Id.createNodeId("23-24")), "Should find simplified node '23-24'");
	}

	@Test
	void testNetworkSimplifier() {
		Network network = buildComplexIntersection();
		IntersectionSimplifier is = new IntersectionSimplifier(10.0, 2);
		Network simpleNetwork = is.simplify(network);
		is.writeClustersToFile(utils.getOutputDirectory() + "clusters.csv");

		NetworkCalcTopoType nct = new NetworkCalcTopoType();
		nct.run(simpleNetwork);
		NetworkSimplifier ns = new NetworkSimplifier();
		ns.run(simpleNetwork);
		new NetworkCleaner().run(simpleNetwork);
		new NetworkWriter(simpleNetwork).write(utils.getOutputDirectory() + "network.xml");

		Assertions.assertEquals(12l, simpleNetwork.getNodes().size(), "Wrong number of nodes.");

		Assertions.assertNotNull(simpleNetwork.getNodes().get(Id.createNodeId("1")), "Should find node '1'");
		Assertions.assertNotNull(simpleNetwork.getNodes().get(Id.createNodeId("3")), "Should find node '3'");
		Assertions.assertNotNull(simpleNetwork.getNodes().get(Id.createNodeId("10")), "Should find node '10'");
		Assertions.assertNotNull(simpleNetwork.getNodes().get(Id.createNodeId("11")), "Should find node '11'");
		Assertions.assertNotNull(simpleNetwork.getNodes().get(Id.createNodeId("26")), "Should find node '26'");
		Assertions.assertNotNull(simpleNetwork.getNodes().get(Id.createNodeId("28")), "Should find node '28'");

		Assertions.assertNull(simpleNetwork.getNodes().get(Id.createNodeId("2")), "Should NOT find node '2'");
		Assertions.assertNull(simpleNetwork.getNodes().get(Id.createNodeId("4")), "Should NOT find node '4'");
		Assertions.assertNull(simpleNetwork.getNodes().get(Id.createNodeId("9")), "Should NOT find node '9'");
		Assertions.assertNull(simpleNetwork.getNodes().get(Id.createNodeId("12")), "Should NOT find node '12'");
		Assertions.assertNull(simpleNetwork.getNodes().get(Id.createNodeId("25")), "Should NOT find node '25'");
		Assertions.assertNull(simpleNetwork.getNodes().get(Id.createNodeId("27")), "Should NOT find node '27'");

		Assertions.assertNotNull(simpleNetwork.getNodes().get(Id.createNodeId("5-6-7-8")), "Should find simplified node '5-6-7-8'");
		Assertions.assertNotNull(simpleNetwork.getNodes().get(Id.createNodeId("13-14")), "Should find simplified node '13-14'");
		Assertions.assertNotNull(simpleNetwork.getNodes().get(Id.createNodeId("16-17-20-21")), "Should find simplified node '16-17-20-21'");
		Assertions.assertNotNull(simpleNetwork.getNodes().get(Id.createNodeId("18-22")), "Should find simplified node '18-22'");
		Assertions.assertNotNull(simpleNetwork.getNodes().get(Id.createNodeId("15-19")), "Should find simplified node '15-19'");
		Assertions.assertNotNull(simpleNetwork.getNodes().get(Id.createNodeId("23-24")), "Should find simplified node '23-24'");
	}


	private Cluster findCluster(List<Cluster> clusters, Coord c) {
		Cluster cluster = null;
		Iterator<Cluster> iterator = clusters.iterator();
		while(cluster == null & iterator.hasNext()) {
			Cluster thisCluster = iterator.next();
			if(thisCluster.getCenterOfGravity().equals(c)) {
				cluster = thisCluster;
			}
		}
		return cluster;
	}


	/**
	 * The following layout is not according to scale, but shows the structure
	 * of the two 'complex' intersections.
	 *                                                                         11
	 *                                                                          |
	 *                                                                          |
	 *                                                                         12
	 *                      3                                                /   \
	 *                      |                                               /     \
	 *                      |                                          .__ 13     14 __.
	 *                      4                                         /     |      |    \
	 *                    /   \                                      |      |      |     |
	 *             .____ 5 ___ 6 _____________> 110m ______________ 15 ___ 16 ___ 17 ___ 18 ___.
	 *    1 ___ 2 /      |     |                                            | \ /  |            \ 27 ___ 28
	 *            \      |     |                                            |  X   |            /
	 *             \.___ 7 ___ 8 ______________ 110m <_____________ 19 ___ 20 /_\ 21 ___ 22 ___/
	 *                    \   /                                      |      |      |      |
	 *                      9                                         \     |      |     /
	 *                      |                                          \__ 23     24 __/
	 *                      |                                               \     /
	 *                     10                                                \   /
	 *                                                                         25
	 *                                                                          |
	 *                                                                          |
	 *                                                                         26
	 */
	private Network buildComplexIntersection() {
		Network network = NetworkUtils.createNetwork();

		/* Left cluster */
		Node n01 = NetworkUtils.createAndAddNode(network, Id.createNodeId( 1), CoordUtils.createCoord(  0.0,  85.0));
		Node n02 = NetworkUtils.createAndAddNode(network, Id.createNodeId( 2), CoordUtils.createCoord( 50.0,  85.0));
		Node n03 = NetworkUtils.createAndAddNode(network, Id.createNodeId( 3), CoordUtils.createCoord( 85.0, 170.0));
		Node n04 = NetworkUtils.createAndAddNode(network, Id.createNodeId( 4), CoordUtils.createCoord( 85.0, 120.0));
		Node n05 = NetworkUtils.createAndAddNode(network, Id.createNodeId( 5), CoordUtils.createCoord( 80.0,  90.0));
		Node n06 = NetworkUtils.createAndAddNode(network, Id.createNodeId( 6), CoordUtils.createCoord( 90.0,  90.0));
		Node n07 = NetworkUtils.createAndAddNode(network, Id.createNodeId( 7), CoordUtils.createCoord( 80.0,  80.0));
		Node n08 = NetworkUtils.createAndAddNode(network, Id.createNodeId( 8), CoordUtils.createCoord( 90.0,  80.0));
		Node n09 = NetworkUtils.createAndAddNode(network, Id.createNodeId( 9), CoordUtils.createCoord( 85.0,  50.0));
		Node n10 = NetworkUtils.createAndAddNode(network, Id.createNodeId(10), CoordUtils.createCoord( 85.0,   0.0));

		NetworkUtils.createAndAddLink(network, Id.createLinkId("1_2"), n01, n02, 50.0, 80.0/3.6, 1000.0, 1.0 );
		NetworkUtils.createAndAddLink(network, Id.createLinkId("2_1"), n02, n01, 50.0, 80.0/3.6, 1000.0, 1.0 );
		NetworkUtils.createAndAddLink(network, Id.createLinkId("2_5"), n02, n05, 50.0, 80.0/3.6, 1000.0, 1.0 );
		NetworkUtils.createAndAddLink(network, Id.createLinkId("3_4"), n03, n04, 50.0, 80.0/3.6, 1000.0, 1.0 );
		NetworkUtils.createAndAddLink(network, Id.createLinkId("4_3"), n04, n03, 50.0, 80.0/3.6, 1000.0, 1.0 );
		NetworkUtils.createAndAddLink(network, Id.createLinkId("4_6"), n04, n06, 50.0, 80.0/3.6, 1000.0, 1.0 );
		NetworkUtils.createAndAddLink(network, Id.createLinkId("5_4"), n05, n04, 50.0, 80.0/3.6, 1000.0, 1.0 );
		NetworkUtils.createAndAddLink(network, Id.createLinkId("5_6"), n05, n06, 50.0, 80.0/3.6, 1000.0, 1.0 );
		NetworkUtils.createAndAddLink(network, Id.createLinkId("6_8"), n06, n08, 50.0, 80.0/3.6, 1000.0, 1.0 );
		NetworkUtils.createAndAddLink(network, Id.createLinkId("7_2"), n07, n02, 50.0, 80.0/3.6, 1000.0, 1.0 );
		NetworkUtils.createAndAddLink(network, Id.createLinkId("7_5"), n07, n05, 50.0, 80.0/3.6, 1000.0, 1.0 );
		NetworkUtils.createAndAddLink(network, Id.createLinkId("8_7"), n08, n07, 50.0, 80.0/3.6, 1000.0, 1.0 );
		NetworkUtils.createAndAddLink(network, Id.createLinkId("8_9"), n08, n09, 50.0, 80.0/3.6, 1000.0, 1.0 );
		NetworkUtils.createAndAddLink(network, Id.createLinkId("9_7"), n09, n07, 50.0, 80.0/3.6, 1000.0, 1.0 );
		NetworkUtils.createAndAddLink(network, Id.createLinkId("9_10"), n09, n10, 50.0, 80.0/3.6, 1000.0, 1.0 );
		NetworkUtils.createAndAddLink(network, Id.createLinkId("10_9"), n10, n09, 50.0, 80.0/3.6, 1000.0, 1.0 );

		/* Right cluster */
		Node n11 = NetworkUtils.createAndAddNode(network, Id.createNodeId(11), CoordUtils.createCoord(225.0, 170.0));
		Node n12 = NetworkUtils.createAndAddNode(network, Id.createNodeId(12), CoordUtils.createCoord(225.0, 140.0));
		Node n13 = NetworkUtils.createAndAddNode(network, Id.createNodeId(13), CoordUtils.createCoord(220.0, 110.0));
		Node n14 = NetworkUtils.createAndAddNode(network, Id.createNodeId(14), CoordUtils.createCoord(230.0, 110.0));
		Node n15 = NetworkUtils.createAndAddNode(network, Id.createNodeId(15), CoordUtils.createCoord(200.0,  90.0));
		Node n16 = NetworkUtils.createAndAddNode(network, Id.createNodeId(16), CoordUtils.createCoord(220.0,  90.0));
		Node n17 = NetworkUtils.createAndAddNode(network, Id.createNodeId(17), CoordUtils.createCoord(230.0,  90.0));
		Node n18 = NetworkUtils.createAndAddNode(network, Id.createNodeId(18), CoordUtils.createCoord(250.0,  90.0));
		Node n19 = NetworkUtils.createAndAddNode(network, Id.createNodeId(19), CoordUtils.createCoord(200.0,  80.0));
		Node n20 = NetworkUtils.createAndAddNode(network, Id.createNodeId(20), CoordUtils.createCoord(220.0,  80.0));
		Node n21 = NetworkUtils.createAndAddNode(network, Id.createNodeId(21), CoordUtils.createCoord(230.0,  80.0));
		Node n22 = NetworkUtils.createAndAddNode(network, Id.createNodeId(22), CoordUtils.createCoord(250.0,  80.0));
		Node n23 = NetworkUtils.createAndAddNode(network, Id.createNodeId(23), CoordUtils.createCoord(220.0,  60.0));
		Node n24 = NetworkUtils.createAndAddNode(network, Id.createNodeId(24), CoordUtils.createCoord(230.0,  60.0));
		Node n25 = NetworkUtils.createAndAddNode(network, Id.createNodeId(25), CoordUtils.createCoord(225.0,  30.0));
		Node n26 = NetworkUtils.createAndAddNode(network, Id.createNodeId(26), CoordUtils.createCoord(225.0,   0.0));
		Node n27 = NetworkUtils.createAndAddNode(network, Id.createNodeId(27), CoordUtils.createCoord(280.0,  85.0));
		Node n28 = NetworkUtils.createAndAddNode(network, Id.createNodeId(28), CoordUtils.createCoord(320.0,  85.0));

		NetworkUtils.createAndAddLink(network, Id.createLinkId("11_12"), n11, n12, 50.0, 80.0/3.6, 1000.0, 1.0 );
		NetworkUtils.createAndAddLink(network, Id.createLinkId("12_11"), n12, n11, 50.0, 80.0/3.6, 1000.0, 1.0 );
		NetworkUtils.createAndAddLink(network, Id.createLinkId("12_14"), n12, n14, 50.0, 80.0/3.6, 1000.0, 1.0 );
		NetworkUtils.createAndAddLink(network, Id.createLinkId("13_12"), n13, n12, 50.0, 80.0/3.6, 1000.0, 1.0 );
		NetworkUtils.createAndAddLink(network, Id.createLinkId("14_17"), n14, n17, 50.0, 80.0/3.6, 1000.0, 1.0 );
		NetworkUtils.createAndAddLink(network, Id.createLinkId("14_18"), n14, n18, 50.0, 80.0/3.6, 1000.0, 1.0 );
		NetworkUtils.createAndAddLink(network, Id.createLinkId("15_13"), n15, n13, 50.0, 80.0/3.6, 1000.0, 1.0 );
		NetworkUtils.createAndAddLink(network, Id.createLinkId("15_16"), n15, n16, 50.0, 80.0/3.6, 1000.0, 1.0 );
		NetworkUtils.createAndAddLink(network, Id.createLinkId("16_13"), n16, n13, 50.0, 80.0/3.6, 1000.0, 1.0 );
		NetworkUtils.createAndAddLink(network, Id.createLinkId("16_17"), n16, n17, 50.0, 80.0/3.6, 1000.0, 1.0 );
		NetworkUtils.createAndAddLink(network, Id.createLinkId("16_21"), n16, n21, 50.0, 80.0/3.6, 1000.0, 1.0 );
		NetworkUtils.createAndAddLink(network, Id.createLinkId("17_18"), n17, n18, 50.0, 80.0/3.6, 1000.0, 1.0 );
		NetworkUtils.createAndAddLink(network, Id.createLinkId("17_20"), n17, n20, 50.0, 80.0/3.6, 1000.0, 1.0 );
		NetworkUtils.createAndAddLink(network, Id.createLinkId("17_21"), n17, n21, 50.0, 80.0/3.6, 1000.0, 1.0 );
		NetworkUtils.createAndAddLink(network, Id.createLinkId("18_27"), n18, n27, 50.0, 80.0/3.6, 1000.0, 1.0 );
		NetworkUtils.createAndAddLink(network, Id.createLinkId("20_16"), n20, n16, 50.0, 80.0/3.6, 1000.0, 1.0 );
		NetworkUtils.createAndAddLink(network, Id.createLinkId("20_17"), n20, n17, 50.0, 80.0/3.6, 1000.0, 1.0 );
		NetworkUtils.createAndAddLink(network, Id.createLinkId("20_19"), n20, n19, 50.0, 80.0/3.6, 1000.0, 1.0 );
		NetworkUtils.createAndAddLink(network, Id.createLinkId("21_16"), n21, n16, 50.0, 80.0/3.6, 1000.0, 1.0 );
		NetworkUtils.createAndAddLink(network, Id.createLinkId("21_20"), n21, n20, 50.0, 80.0/3.6, 1000.0, 1.0 );
		NetworkUtils.createAndAddLink(network, Id.createLinkId("21_24"), n21, n24, 50.0, 80.0/3.6, 1000.0, 1.0 );
		NetworkUtils.createAndAddLink(network, Id.createLinkId("22_21"), n22, n21, 50.0, 80.0/3.6, 1000.0, 1.0 );
		NetworkUtils.createAndAddLink(network, Id.createLinkId("22_24"), n22, n24, 50.0, 80.0/3.6, 1000.0, 1.0 );
		NetworkUtils.createAndAddLink(network, Id.createLinkId("23_19"), n23, n19, 50.0, 80.0/3.6, 1000.0, 1.0 );
		NetworkUtils.createAndAddLink(network, Id.createLinkId("23_20"), n23, n20, 50.0, 80.0/3.6, 1000.0, 1.0 );
		NetworkUtils.createAndAddLink(network, Id.createLinkId("24_25"), n24, n25, 50.0, 80.0/3.6, 1000.0, 1.0 );
		NetworkUtils.createAndAddLink(network, Id.createLinkId("25_23"), n25, n23, 50.0, 80.0/3.6, 1000.0, 1.0 );
		NetworkUtils.createAndAddLink(network, Id.createLinkId("25_26"), n25, n26, 50.0, 80.0/3.6, 1000.0, 1.0 );
		NetworkUtils.createAndAddLink(network, Id.createLinkId("26_25"), n26, n25, 50.0, 80.0/3.6, 1000.0, 1.0 );
		NetworkUtils.createAndAddLink(network, Id.createLinkId("27_22"), n27, n22, 50.0, 80.0/3.6, 1000.0, 1.0 );
		NetworkUtils.createAndAddLink(network, Id.createLinkId("27_28"), n27, n28, 50.0, 80.0/3.6, 1000.0, 1.0 );
		NetworkUtils.createAndAddLink(network, Id.createLinkId("28_27"), n28, n27, 50.0, 80.0/3.6, 1000.0, 1.0 );

		/* Link the two clusters */
		NetworkUtils.createAndAddLink(network, Id.createLinkId("6_15"), n06, n15, 50.0, 80.0/3.6, 1000.0, 2.0 );
		NetworkUtils.createAndAddLink(network, Id.createLinkId("19_8"), n19, n08, 50.0, 80.0/3.6, 1000.0, 2.0 );

		network.setName("Two complex intersections test network.");

		return network;
	}
}
