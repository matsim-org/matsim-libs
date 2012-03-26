/* *********************************************************************** *
 * project: org.matsim.*
 * NetworkTest.java
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
//package playground.thibautd.parknride;
//
//import java.util.Collection;
//
//import org.junit.Assert;
//import org.junit.Before;
//import org.junit.Ignore;
//import org.junit.Rule;
//import org.junit.Test;
//
//import org.matsim.api.core.v01.network.Link;
//import org.matsim.api.core.v01.network.Node;
//import org.matsim.core.config.Config;
//import org.matsim.core.config.ConfigUtils;
//import org.matsim.core.scenario.ScenarioImpl;
//import org.matsim.pt.router.TransitRouterNetwork;
//import org.matsim.pt.router.TransitRouterNetwork.TransitRouterNetworkLink;
//import org.matsim.pt.router.TransitRouterNetwork.TransitRouterNetworkNode;
//import org.matsim.pt.transitSchedule.api.TransitSchedule;
//import org.matsim.testcases.MatsimTestUtils;
//
///**
// * Tests the routing network.
// * @author thibautd
// */
//public class NetworkTest {
//	@Rule
//	public final MatsimTestUtils utils = new MatsimTestUtils();
//
//	private static final String CONFIG_FILE = "config.xml";
//	private static final double MAX_BEE_LINE_DIST = 500;
//	private TransitRouterNetwork transitNetwork;
//	private ParkAndRideRouterNetwork pnrNetwork;
//
//	@Before
//	public void init() {
//		Config config = ConfigUtils.createConfig();
//		ParkAndRideUtils.setConfigGroup( config );
//		ConfigUtils.loadConfig( config , utils.getPackageInputDirectory() + "/" + CONFIG_FILE );
//		ScenarioImpl scenario = (ScenarioImpl) ParkAndRideUtils.loadScenario( config );
//
//		TransitSchedule schedule = scenario.getTransitSchedule();
//		ParkAndRideFacilities facilities = ParkAndRideUtils.getParkAndRideFacilities( scenario );
//
//		transitNetwork =
//			TransitRouterNetwork.createFromSchedule(
//					schedule,
//					MAX_BEE_LINE_DIST );
//		pnrNetwork = 
//			new ParkAndRideRouterNetwork(
//				scenario.getNetwork(),
//				schedule,
//				MAX_BEE_LINE_DIST,
//				facilities);
//	}
//
//	@Test
//	public void testParkAndRideContainsTransitNodes() throws Exception {
//		testContainsAllNodes( transitNetwork.getNodes().values() , pnrNetwork.getNodes().values() );
//	}
//
//	@Test
//	public void testParkAndRideContainsTransitLinks() throws Exception {
//		testContainsAllLinks( transitNetwork.getLinks().values() , pnrNetwork.getLinks().values() );
//	}
//
//	@Test @Ignore
//	public void testNoExcedentaryNodeLink() throws Exception {
//		
//	}
//
//	private final void testContainsAllNodes(
//			final Collection<? extends Node> transitNodes,
//			final Collection<? extends Node> pnrNodes) {
//		outer:
//		for (Node node : transitNodes) {
//			TransitRouterNetworkNode transitNode = (TransitRouterNetworkNode) node;
//			
//			for (Node node2 : pnrNodes) {
//				if (node2 instanceof TransitRouterNetworkNode) {
//					TransitRouterNetworkNode pnrNode = (TransitRouterNetworkNode) node2;
//
//					if (areEquals( pnrNode.getRoute() , transitNode.getRoute() ) &&
//							areEquals( pnrNode.getLine() , transitNode.getLine() ) &&
//							areEquals( pnrNode.getStop() , transitNode.getStop() ) ) {
//						// there is a corresponding node: examine next
//						continue outer;
//					}
//				}
//			}
//
//			// no corresponding node
//			Assert.fail( "no node corresponding to "+transitNode );
//		}
//	}
//
//	private final void testContainsAllLinks(
//			final Collection<? extends Link> transitLinks,
//			final Collection<? extends Link> pnrLinks) {
//		outer:
//		for (Link link : transitLinks) {
//			TransitRouterNetworkLink transitLink = (TransitRouterNetworkLink) link;
//			
//			for (Link link2 : pnrLinks) {
//				if (link2 instanceof TransitRouterNetworkLink) {
//					TransitRouterNetworkLink pnrLink = (TransitRouterNetworkLink) link2;
//
//					if (areEquals( pnrLink.getRoute() , transitLink.getRoute() ) &&
//							areEquals( pnrLink.getLine() , transitLink.getLine() ) ) {
//						// there is a corresponding link: examine next
//						continue outer;
//					}
//				}
//			}
//
//			// no corresponding link
//			Assert.fail( "no link corresponding to "+transitLink );
//		}
//	}
//
//	private static boolean areEquals(
//			final Object o1,
//			final Object o2) {
//		return o1 == null ? o2 == null : o1.equals( o2 );
//	}
//}
//
