/* *********************************************************************** *
 * project: org.matsim.*
 * TransitRouterNetworkTravelTimeCostTest.java
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

package playground.mrieser.pt.router;

import junit.framework.TestCase;

import org.matsim.testcases.MatsimTestCase;

import playground.mrieser.pt.router.TransitRouter;
import playground.mrieser.pt.router.TransitRouterConfig;
import playground.mrieser.pt.router.TransitRouterNetwork;
import playground.mrieser.pt.router.TransitRouterNetworkTravelTimeCost;
import playground.mrieser.pt.router.TransitRouterNetwork.TransitRouterNetworkLink;

public class TransitRouterNetworkTravelTimeCostTest extends TestCase {

	public void testTravelTime() {
		Fixture f = new Fixture();
		f.init();
		TransitRouterConfig conf = new TransitRouterConfig();
		TransitRouterNetworkTravelTimeCost tc = new TransitRouterNetworkTravelTimeCost(conf);
		TransitRouter router = new TransitRouter(f.schedule);
		TransitRouterNetwork routerNet = router.getTransitRouterNetwork();
		// find the link connecting C and D on the blue line
		TransitRouterNetworkLink testLink = null;
		for (TransitRouterNetworkLink link : routerNet.getLinks().values()) {
			if ((link.line == f.redLine) &&
					(link.fromNode.stop.getStopFacility().getName().equals("C")) &&
					(link.toNode.stop.getStopFacility().getName().equals("G"))) {
				testLink = link;
			}
		}
		assertEquals(9.0*60, tc.getLinkTravelTime(testLink, 6.0*3600), MatsimTestCase.EPSILON);
	}

	public void testWaitingTime() {
		Fixture f = new Fixture();
		f.init();
		TransitRouterConfig conf = new TransitRouterConfig();
		TransitRouterNetworkTravelTimeCost tc = new TransitRouterNetworkTravelTimeCost(conf);
		TransitRouter router = new TransitRouter(f.schedule);
		TransitRouterNetwork routerNet = router.getTransitRouterNetwork();
		// find the link connecting C and D on the blue line
		TransitRouterNetworkLink testLink = null;
		for (TransitRouterNetworkLink link : routerNet.getLinks().values()) {
			if ((link.line == f.blueLine) &&
					(link.fromNode.stop.getStopFacility().getName().equals("C")) &&
					(link.toNode.stop.getStopFacility().getName().equals("D"))) {
				testLink = link;
			}
		}
		assertEquals(2.0*60 + 7.0*60, tc.getLinkTravelTime(testLink, 6.0*3600), MatsimTestCase.EPSILON);
		assertEquals(1.0*60 + 7.0*60, tc.getLinkTravelTime(testLink, 6.0*3600 + 60), MatsimTestCase.EPSILON);
		assertEquals(0.0*60 + 7.0*60, tc.getLinkTravelTime(testLink, 6.0*3600 + 120), MatsimTestCase.EPSILON);
		assertEquals(20.0*60 -1 + 7.0*60, tc.getLinkTravelTime(testLink, 6.0*3600 + 121), MatsimTestCase.EPSILON);
	}

	public void testTravelTimeAfterMidnight() {
		Fixture f = new Fixture();
		f.init();
		TransitRouterConfig conf = new TransitRouterConfig();
		TransitRouterNetworkTravelTimeCost tc = new TransitRouterNetworkTravelTimeCost(conf);
		TransitRouter router = new TransitRouter(f.schedule);
		TransitRouterNetwork routerNet = router.getTransitRouterNetwork();
		// find the link connecting C and D on the blue line
		TransitRouterNetworkLink testLink = null;
		for (TransitRouterNetworkLink link : routerNet.getLinks().values()) {
			if ((link.line == f.blueLine) &&
					(link.fromNode.stop.getStopFacility().getName().equals("C")) &&
					(link.toNode.stop.getStopFacility().getName().equals("D"))) {
				testLink = link;
			}
		}
		// planned departure at 25:00, has to wait until 05:22 = 29:22
		assertEquals(22.0*60 + 4.0*3600 + 7.0*60, tc.getLinkTravelTime(testLink, 25.0*3600), MatsimTestCase.EPSILON);
		// planned departure at 47:00, has to wait until 05:22 = 53:22
		assertEquals(22.0*60 + 6.0*3600 + 7.0*60, tc.getLinkTravelTime(testLink, 47.0*3600), MatsimTestCase.EPSILON);
		// planned departure at 49:00, has to wait until 05:22 = 53:22, tests explicitly > 2*MIDNIGHT
		assertEquals(22.0*60 + 4.0*3600 + 7.0*60, tc.getLinkTravelTime(testLink, 49.0*3600), MatsimTestCase.EPSILON);
	}

	public void testTravelCostLineSwitch() {
		Fixture f = new Fixture();
		f.init();
		TransitRouterConfig conf = new TransitRouterConfig();
		TransitRouterNetworkTravelTimeCost tc = new TransitRouterNetworkTravelTimeCost(conf);
		TransitRouter router = new TransitRouter(f.schedule);
		TransitRouterNetwork routerNet = router.getTransitRouterNetwork();
		// find the link connecting C and D on the blue line
		TransitRouterNetworkLink testLink = null;
		for (TransitRouterNetworkLink link : routerNet.getLinks().values()) {
			if ((link.line == null) &&
					(link.fromNode.stop.getStopFacility().getName().equals("C")) &&
					(link.toNode.stop.getStopFacility().getName().equals("C"))) {
				testLink = link;
			}
		}

		double oldCost = conf.costLineSwitch;
		double cost1 = tc.getLinkTravelCost(testLink, 7.0*3600);
		conf.costLineSwitch = 0.0;
		double cost2 = tc.getLinkTravelCost(testLink, 7.0*3600);
		assertEquals(oldCost, cost1 - cost2, MatsimTestCase.EPSILON);
		conf.costLineSwitch = 40.125;
		double cost3 = tc.getLinkTravelCost(testLink, 7.0*3600);
		assertEquals(40.125, cost3 - cost2, MatsimTestCase.EPSILON);
	}

}
