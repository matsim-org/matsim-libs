/* *********************************************************************** *
 * project: org.matsim.*
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

package org.matsim.contrib.minibus.schedule;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.minibus.PConfigGroup;
import org.matsim.contrib.minibus.routeProvider.PScenarioHelper;
import org.matsim.core.config.groups.NetworkConfigGroup;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.testcases.MatsimTestUtils;


public class CreatePStopsOnJunctionApproachesAndBetweenJunctionsTest {
	@RegisterExtension private MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	final void testPScenarioHelperTestNetwork() {

		Network net = PScenarioHelper.createTestNetwork().getNetwork();
		PConfigGroup pC = new PConfigGroup();
		pC.addParam("stopLocationSelector", "outsideJunctionAreas");
		pC.addParam("stopLocationSelectorParameter", "50.0,2,Double.positiveInfinity");

		String realPtStopLink = "3233";

		/* Modify some link attributes to check whether these links are excluded as specified in the config */
		String tooLowCapacityLink = "2122";
		String tooHighFreespeedLink = "2223";
		net.getLinks().get(Id.createLinkId(tooLowCapacityLink)).setCapacity(100);
		net.getLinks().get(Id.createLinkId(tooHighFreespeedLink)).setFreespeed(100);
		pC.addParam("minCapacityForStops", "0");
		pC.addParam("speedLimitForStops", "1000");

		TransitSchedule transitSchedule = CreatePStopsOnJunctionApproachesAndBetweenJunctions.createPStops(net, pC, new NetworkConfigGroup());

		int numberOfParaStops = 0;
		for (TransitStopFacility stopFacility : transitSchedule.getFacilities().values()) {
			if (stopFacility.getId().toString().startsWith(pC.getPIdentifier())) {
				numberOfParaStops++;
			}
		}

		/* 4 inner junctions with 4 approaches + 8 outer junctions with 3 approaches + 4 corners without junctions = 40 approach links */
		Assertions.assertEquals(40, numberOfParaStops, MatsimTestUtils.EPSILON, "All 40 junction approach links got a paratransit stop");

		/* Check whether these links are included as specified in the config */
		Assertions.assertNotNull(transitSchedule.getFacilities().get(Id.create(pC.getPIdentifier() + realPtStopLink, TransitStopFacility.class)), "Paratransit stop at link without real pt stop");
		Assertions.assertNotNull(transitSchedule.getFacilities().get(Id.create(pC.getPIdentifier() + tooLowCapacityLink, TransitStopFacility.class)), "Paratransit stop at link with small capacity");
		Assertions.assertNotNull(transitSchedule.getFacilities().get(Id.create(pC.getPIdentifier() + tooHighFreespeedLink, TransitStopFacility.class)), "Paratransit stop at link with high freespeed");

		TransitScheduleFactoryImpl tSF = new TransitScheduleFactoryImpl();

		TransitSchedule realTransitSchedule = tSF.createTransitSchedule();

		TransitStopFacility stop1 = tSF.createTransitStopFacility(Id.create(realPtStopLink, TransitStopFacility.class), new Coord(0.0, 0.0), false);
		stop1.setLinkId(Id.create(realPtStopLink, Link.class));
		realTransitSchedule.addStopFacility(stop1);

		/* Modify config to exclude some links */
		pC.addParam("minCapacityForStops", "800");
		pC.addParam("speedLimitForStops", "20");

		transitSchedule = CreatePStopsOnJunctionApproachesAndBetweenJunctions.createPStops(net, pC, realTransitSchedule, new NetworkConfigGroup());

		numberOfParaStops = 0;
		for (TransitStopFacility stopFacility : transitSchedule.getFacilities().values()) {
			if (stopFacility.getId().toString().startsWith(pC.getPIdentifier())) {
				numberOfParaStops++;
			}
		}

		Assertions.assertEquals(40 - 3, numberOfParaStops, MatsimTestUtils.EPSILON, "All car links minus one stop from formal transit got a paratransit stop");

		Assertions.assertNull(transitSchedule.getFacilities().get(Id.create(pC.getPIdentifier() + realPtStopLink, TransitStopFacility.class)), "No paratransit stop at link with real pt stop");
		Assertions.assertNull(transitSchedule.getFacilities().get(Id.create(pC.getPIdentifier() + tooLowCapacityLink, TransitStopFacility.class)), "No paratransit stop at link with too small capacity");
		Assertions.assertNull(transitSchedule.getFacilities().get(Id.create(pC.getPIdentifier() + tooHighFreespeedLink, TransitStopFacility.class)), "No paratransit stop at link with too high freespeed");

	}

	/** {@link org.matsim.core.network.algorithms.intersectionSimplifier.IntersectionSimplifierTest} */
	@Test
	final void testComplexIntersection() {

		Network network = buildComplexIntersection();
		PConfigGroup pC = new PConfigGroup();
		pC.addParam("stopLocationSelector", "outsideJunctionAreas");
		pC.addParam("stopLocationSelectorParameter", "30.0,2,500");

		TransitSchedule transitSchedule = CreatePStopsOnJunctionApproachesAndBetweenJunctions.createPStops(network, pC, new NetworkConfigGroup());

		int numberOfParaStops = 0;
		for (TransitStopFacility stopFacility : transitSchedule.getFacilities().values()) {
			if (stopFacility.getId().toString().startsWith(pC.getPIdentifier())) {
				numberOfParaStops++;
			}
		}

		Assertions.assertEquals(16, numberOfParaStops, MatsimTestUtils.EPSILON, "Check number of paratransit stops");

		/* approaches to (unclustered) dead-ends */
		Assertions.assertNotNull(transitSchedule.getFacilities().get(Id.create(pC.getPIdentifier() + "2_1", TransitStopFacility.class)), "Should find paratransit stop 'p_2_1'");
		Assertions.assertNotNull(transitSchedule.getFacilities().get(Id.create(pC.getPIdentifier() + "4_3", TransitStopFacility.class)), "Should find paratransit stop 'p_4_3'");
		Assertions.assertNotNull(transitSchedule.getFacilities().get(Id.create(pC.getPIdentifier() + "9_10", TransitStopFacility.class)), "Should find paratransit stop 'p_9_10'");
		Assertions.assertNotNull(transitSchedule.getFacilities().get(Id.create(pC.getPIdentifier() + "30_31", TransitStopFacility.class)), "Should find paratransit stop 'p_30_31'");

		/* left junction: clustered nodes 5-6-7-8 */
		Assertions.assertNotNull(transitSchedule.getFacilities().get(Id.create(pC.getPIdentifier() + "2_5", TransitStopFacility.class)), "Should find junction approach paratransit stop 'p_2_5'");
		Assertions.assertNotNull(transitSchedule.getFacilities().get(Id.create(pC.getPIdentifier() + "4_6", TransitStopFacility.class)), "Should find junction approach paratransit stop 'p_4_6'");
		Assertions.assertNotNull(transitSchedule.getFacilities().get(Id.create(pC.getPIdentifier() + "19_8", TransitStopFacility.class)), "Should find junction approach paratransit stop 'p_19_8'");
		Assertions.assertNotNull(transitSchedule.getFacilities().get(Id.create(pC.getPIdentifier() + "9_7", TransitStopFacility.class)), "Should find junction approach paratransit stop 'p_9_7'");

		Assertions.assertNull(transitSchedule.getFacilities().get(Id.create(pC.getPIdentifier() + "5_6", TransitStopFacility.class)), "Should NOT find paratransit stop at link in junction '5_6'");
		Assertions.assertNull(transitSchedule.getFacilities().get(Id.create(pC.getPIdentifier() + "6_8", TransitStopFacility.class)), "Should NOT find paratransit stop at link in junction '6_8'");
		Assertions.assertNull(transitSchedule.getFacilities().get(Id.create(pC.getPIdentifier() + "8_7", TransitStopFacility.class)), "Should NOT find paratransit stop at link in junction '8_7'");
		Assertions.assertNull(transitSchedule.getFacilities().get(Id.create(pC.getPIdentifier() + "7_5", TransitStopFacility.class)), "Should NOT find paratransit stop at link in junction '7_5'");

		/* clustered nodes 11-12: dead-end, therefore only one stop approaching */
		Assertions.assertNotNull(transitSchedule.getFacilities().get(Id.create(pC.getPIdentifier() + "13_12", TransitStopFacility.class)), "Should find junction approach paratransit stop 'p_13_12'");

		/* right junction: clustered nodes 13-14-15-16-17-18-19-20-21-22-23-24 */
		Assertions.assertNotNull(transitSchedule.getFacilities().get(Id.create(pC.getPIdentifier() + "6_15", TransitStopFacility.class)), "Should find junction approach paratransit stop 'p_6_15'");
		Assertions.assertNotNull(transitSchedule.getFacilities().get(Id.create(pC.getPIdentifier() + "12_14", TransitStopFacility.class)), "Should find junction approach paratransit stop 'p_12_14'");
		Assertions.assertNotNull(transitSchedule.getFacilities().get(Id.create(pC.getPIdentifier() + "27_22", TransitStopFacility.class)), "Should find junction approach paratransit stop 'p_27_22'");
		Assertions.assertNotNull(transitSchedule.getFacilities().get(Id.create(pC.getPIdentifier() + "25_23", TransitStopFacility.class)), "Should find junction approach paratransit stop 'p_25_23'");
		// in-junction links
		// east-west, north-south
		Assertions.assertNull(transitSchedule.getFacilities().get(Id.create(pC.getPIdentifier() + "15_16", TransitStopFacility.class)), "Should NOT find paratransit stop at link in junction '15_16'");
		Assertions.assertNull(transitSchedule.getFacilities().get(Id.create(pC.getPIdentifier() + "16_17", TransitStopFacility.class)), "Should NOT find paratransit stop at link in junction '16_17'");
		Assertions.assertNull(transitSchedule.getFacilities().get(Id.create(pC.getPIdentifier() + "17_18", TransitStopFacility.class)), "Should NOT find paratransit stop at link in junction '17_18'");
		Assertions.assertNull(transitSchedule.getFacilities().get(Id.create(pC.getPIdentifier() + "14_17", TransitStopFacility.class)), "Should NOT find paratransit stop at link in junction '14_17'");
		Assertions.assertNull(transitSchedule.getFacilities().get(Id.create(pC.getPIdentifier() + "17_21", TransitStopFacility.class)), "Should NOT find paratransit stop at link in junction '17_21'");
		Assertions.assertNull(transitSchedule.getFacilities().get(Id.create(pC.getPIdentifier() + "21_24", TransitStopFacility.class)), "Should NOT find paratransit stop at link in junction '21_24'");
		Assertions.assertNull(transitSchedule.getFacilities().get(Id.create(pC.getPIdentifier() + "22_21", TransitStopFacility.class)), "Should NOT find paratransit stop at link in junction '22_21'");
		Assertions.assertNull(transitSchedule.getFacilities().get(Id.create(pC.getPIdentifier() + "21_20", TransitStopFacility.class)), "Should NOT find paratransit stop at link in junction '21_20'");
		Assertions.assertNull(transitSchedule.getFacilities().get(Id.create(pC.getPIdentifier() + "20_19", TransitStopFacility.class)), "Should NOT find paratransit stop at link in junction '20_19'");
		Assertions.assertNull(transitSchedule.getFacilities().get(Id.create(pC.getPIdentifier() + "23_20", TransitStopFacility.class)), "Should NOT find paratransit stop at link in junction '23_20'");
		Assertions.assertNull(transitSchedule.getFacilities().get(Id.create(pC.getPIdentifier() + "20_16", TransitStopFacility.class)), "Should NOT find paratransit stop at link in junction '20_16'");
		Assertions.assertNull(transitSchedule.getFacilities().get(Id.create(pC.getPIdentifier() + "16_13", TransitStopFacility.class)), "Should NOT find paratransit stop at link in junction '16_13'");
		// outer avoidance links
		Assertions.assertNull(transitSchedule.getFacilities().get(Id.create(pC.getPIdentifier() + "15_13", TransitStopFacility.class)), "Should NOT find paratransit stop at link in junction '15_13'");
		Assertions.assertNull(transitSchedule.getFacilities().get(Id.create(pC.getPIdentifier() + "14_18", TransitStopFacility.class)), "Should NOT find paratransit stop at link in junction '14_18'");
		Assertions.assertNull(transitSchedule.getFacilities().get(Id.create(pC.getPIdentifier() + "22_24", TransitStopFacility.class)), "Should NOT find paratransit stop at link in junction '22_24'");
		Assertions.assertNull(transitSchedule.getFacilities().get(Id.create(pC.getPIdentifier() + "23_19", TransitStopFacility.class)), "Should NOT find paratransit stop at link in junction '23_19'");
		// crossing
		Assertions.assertNull(transitSchedule.getFacilities().get(Id.create(pC.getPIdentifier() + "17_20", TransitStopFacility.class)), "Should NOT find paratransit stop at link in junction '17_20'");
		Assertions.assertNull(transitSchedule.getFacilities().get(Id.create(pC.getPIdentifier() + "20_17", TransitStopFacility.class)), "Should NOT find paratransit stop at link in junction '20_17'");
		Assertions.assertNull(transitSchedule.getFacilities().get(Id.create(pC.getPIdentifier() + "16_21", TransitStopFacility.class)), "Should NOT find paratransit stop at link in junction '16_21'");
		Assertions.assertNull(transitSchedule.getFacilities().get(Id.create(pC.getPIdentifier() + "21_16", TransitStopFacility.class)), "Should NOT find paratransit stop at link in junction '21_16'");

		/* clustered nodes 25-26: dead-end, therefore only one stop approaching */
		Assertions.assertNotNull(transitSchedule.getFacilities().get(Id.create(pC.getPIdentifier() + "24_25", TransitStopFacility.class)), "Should find junction approach paratransit stop 'p_24_25'");

		/* links exiting junctions (towards dead-ends) */
		Assertions.assertNull(transitSchedule.getFacilities().get(Id.create(pC.getPIdentifier() + "7_2", TransitStopFacility.class)), "Should NOT find paratransit stop at link exiting junction '7_2'");
		Assertions.assertNull(transitSchedule.getFacilities().get(Id.create(pC.getPIdentifier() + "5_4", TransitStopFacility.class)), "Should NOT find paratransit stop at link exiting junction '5_4'");
		Assertions.assertNull(transitSchedule.getFacilities().get(Id.create(pC.getPIdentifier() + "8_9", TransitStopFacility.class)), "Should NOT find paratransit stop at link exiting junction '8_9'");
		Assertions.assertNull(transitSchedule.getFacilities().get(Id.create(pC.getPIdentifier() + "7_2", TransitStopFacility.class)), "Should NOT find paratransit stop at link exiting junction '18_19'");

		/* Infill Stops between junctions / dead-ends */
		Assertions.assertNotNull(transitSchedule.getFacilities().get(Id.create(pC.getPIdentifier() + "30_29", TransitStopFacility.class)), "Should find infill paratransit stop 'p_30_29'");
		Assertions.assertNotNull(transitSchedule.getFacilities().get(Id.create(pC.getPIdentifier() + "27_28", TransitStopFacility.class)), "Should find infill paratransit stop 'p_27_28'");

		/* Check whether CalcTopoTypes is considered (type 8 : intersections only) */
		pC.addParam("TopoTypesForStops", "8");

		transitSchedule = CreatePStopsOnJunctionApproachesAndBetweenJunctions.createPStops(network, pC, new NetworkConfigGroup());
		Assertions.assertNull(transitSchedule.getFacilities().get(Id.create(pC.getPIdentifier() + "30_31", TransitStopFacility.class)), "Should NOT find paratransit stop at link with wrong topo type (not an intersection) '30_31'");
	}

	/**
	 * The following layout is not according to scale, but shows the structure
	 * of the two 'complex' intersections.
	 *                                                        11
	 *                                                        |
	 *                                                        |
	 *                                                        12
	 *                      3                               /   \
	 *                      |                              /     \
	 *                      |                         .__ 13     14 __.
	 *                      4                        /     |      |    \
	 *                    /   \                     |      |      |     |
	 *             .____ 5 ___ 6 _____> 110m _____ 15 ___ 16 ___ 17 ___ 18 ___.
	 *    1 ___ 2 /      |     |                           | \ /  |            \ 27 ___ 28 __-400m-__ 29 __-100m-__ 30 __-100m-__ 31
	 *            \      |     |                           |  X   |            /
	 *             \.___ 7 ___ 8 _____ 110m <_____ 19 ___ 20 /_\ 21 ___ 22 ___/
	 *                    \   /                     |      |      |     |
	 *                      9                        \     |      |    /
	 *                      |                         \__ 23     24 __/
	 *                      |                              \     /
	 *                     10                               \   /
	 *                                                        25
	 *                                                        |
	 *                                                        |
	 *                                                        26
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

		/* Eastern extension, note length different from beeline distance */
		Node n29 = NetworkUtils.createAndAddNode(network, Id.createNodeId(29), CoordUtils.createCoord(700.0,  85.0));
		Node n30 = NetworkUtils.createAndAddNode(network, Id.createNodeId(30), CoordUtils.createCoord(800.0,  85.0));
		Node n31 = NetworkUtils.createAndAddNode(network, Id.createNodeId(31), CoordUtils.createCoord(900.0,  85.0));

		NetworkUtils.createAndAddLink(network, Id.createLinkId("28_29"), n28, n29, 400.0, 80.0/3.6, 1000.0, 1.0 );
		NetworkUtils.createAndAddLink(network, Id.createLinkId("29_28"), n29, n28, 400.0, 80.0/3.6, 1000.0, 1.0 );
		NetworkUtils.createAndAddLink(network, Id.createLinkId("29_30"), n29, n30, 100.0, 80.0/3.6, 1000.0, 1.0 );
		NetworkUtils.createAndAddLink(network, Id.createLinkId("30_29"), n30, n29, 100.0, 80.0/3.6, 1000.0, 1.0 );
		NetworkUtils.createAndAddLink(network, Id.createLinkId("30_31"), n30, n31, 100.0, 80.0/3.6, 1000.0, 1.0 );
		NetworkUtils.createAndAddLink(network, Id.createLinkId("31_30"), n31, n30, 100.0, 80.0/3.6, 1000.0, 1.0 );

		network.setName("Two complex intersections test network.");

		return network;
	}
}
