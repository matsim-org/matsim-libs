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

package playground.mmoyo.ptRouterAdapted;

import junit.framework.TestCase;

import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.router.TransitRouterImpl;
import org.matsim.pt.router.TransitRouterNetwork;
import org.matsim.pt.router.TransitRouterNetwork.TransitRouterNetworkLink;
import org.matsim.pt.router.TransitRouterNetworkTravelTimeCost;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.testcases.MatsimTestCase;


/**
 * @author manuel after mrieser
 */

/**these are the same tests as in org.matsim.pt.router.TransitRouterNetworkTravelTimeCostTest 
 * using the adapted router instead, to taste the implementation of waiting time as separate routing parameter*/
public class AdaptedTransitRouterNetworkTravelTimeCostTest extends TestCase {

	public void testTravelTime() {
		//create the scenario test
		Fixture f = new Fixture();
		f.init();
		
		//create the adapted router
		MyTransitRouterConfig myTRConfig = new MyTransitRouterConfig(f.scenario.getConfig().planCalcScore(),
				f.scenario.getConfig().plansCalcRoute(), f.scenario.getConfig().transitRouter(),
				f.scenario.getConfig().vspExperimental());
		AdaptedTransitRouterNetworkTravelTimeCost timecost = new AdaptedTransitRouterNetworkTravelTimeCost(myTRConfig);
		AdaptedTransitRouter adaptedRouter = new AdaptedTransitRouter(myTRConfig, f.schedule) ;
		TransitRouterNetwork routerNet = adaptedRouter.getTransitRouterNetwork();

		// find the link connecting C and G on the red line
		TransitRouterNetworkLink testLink = null;
		for (TransitRouterNetworkLink link : routerNet.getLinks().values()) {
			if ((link.getLine() == f.redLine) &&
					(link.fromNode.stop.getStopFacility().getName().equals("C")) &&
					(link.toNode.stop.getStopFacility().getName().equals("G"))) {
				testLink = link;
			}
		}
		
		// at 6am the travel time is 9 mins
		assertEquals(9.0*60, timecost.getLinkTravelTime(testLink, 6.0*3600), MatsimTestCase.EPSILON);
	}

	public void testWaitingTime() {
		Fixture f = new Fixture();
		f.init();
		MyTransitRouterConfig myTRConfig = new MyTransitRouterConfig(f.scenario.getConfig().planCalcScore(),
				f.scenario.getConfig().plansCalcRoute(), f.scenario.getConfig().transitRouter(),
				f.scenario.getConfig().vspExperimental());
		AdaptedTransitRouterNetworkTravelTimeCost timecost = new AdaptedTransitRouterNetworkTravelTimeCost(myTRConfig);
		AdaptedTransitRouter adaptedRouter = new AdaptedTransitRouter(myTRConfig, f.schedule) ;
		TransitRouterNetwork routerNet = adaptedRouter.getTransitRouterNetwork();
	
		// find the link connecting C and D on the blue line
		TransitRouterNetworkLink testLink = null;
		for (TransitRouterNetworkLink link : routerNet.getLinks().values()) {
			if ((link.getLine() == f.blueLine) && (link.fromNode.stop.getStopFacility().getName().equals("C")) && (link.toNode.stop.getStopFacility().getName().equals("D"))) {
				testLink = link;
			}
		}
		
		assertEquals(/*2.0*60 + */ 7.0*60, timecost.getLinkTravelTime(testLink, 6.0*3600), MatsimTestCase.EPSILON);
		assertEquals(/*1.0*60 + */7.0*60, timecost.getLinkTravelTime(testLink, 6.0*3600 + 60), MatsimTestCase.EPSILON);
		assertEquals(/*0.0*60 + */7.0*60, timecost.getLinkTravelTime(testLink, 6.0*3600 + 120), MatsimTestCase.EPSILON);
		assertEquals(/*20.0*60 -1 + */7.0*60, timecost.getLinkTravelTime(testLink, 6.0*3600 + 121), MatsimTestCase.EPSILON);
	
		assertEquals(2.0*60 /*+ 7.0*60*/, timecost.getWaitingTimeAtStop(testLink, 6.0*3600), MatsimTestCase.EPSILON);
		assertEquals(1.0*60 /*+ 7.0*60*/, timecost.getWaitingTimeAtStop(testLink, 6.0*3600 + 60), MatsimTestCase.EPSILON);
		assertEquals(0.0*60 /*+ 7.0*60*/, timecost.getWaitingTimeAtStop(testLink, 6.0*3600 + 120), MatsimTestCase.EPSILON);
		assertEquals(20.0*60 /*+ 7.0*60*/, timecost.getWaitingTimeAtStop(testLink, 6.0*3600 + 121), MatsimTestCase.EPSILON);
	}

	public void testTravelTimeAfterMidnight() {
		Fixture f = new Fixture();
		f.init();
		MyTransitRouterConfig myTRConfig = new MyTransitRouterConfig(f.scenario.getConfig().planCalcScore(),
				f.scenario.getConfig().plansCalcRoute(), f.scenario.getConfig().transitRouter(),
				f.scenario.getConfig().vspExperimental());
		AdaptedTransitRouterNetworkTravelTimeCost timecost = new AdaptedTransitRouterNetworkTravelTimeCost(myTRConfig);
		AdaptedTransitRouter adaptedRouter = new AdaptedTransitRouter(myTRConfig, f.schedule) ;
		TransitRouterNetwork routerNet = adaptedRouter.getTransitRouterNetwork();
		// find the link connecting C and D on the blue line
		TransitRouterNetworkLink testLink = null;
		for (TransitRouterNetworkLink link : routerNet.getLinks().values()) {
			if ((link.getLine() == f.blueLine) &&
					(link.fromNode.stop.getStopFacility().getName().equals("C")) &&
					(link.toNode.stop.getStopFacility().getName().equals("D"))) {
				testLink = link;
			}
		}
		
		
		// planned departure at 25:00, has to wait until 05:22 = 29:22
		//original for matsim router
		//assertEquals(22.0*60 + 4.0*3600 + 7.0*60, timecost.getLinkTravelTime(testLink, 25.0*3600) , MatsimTestCase.EPSILON);
		//with separate Waiting time
		assertEquals(22.0*60 + 4.0*3600 + 7.0*60, timecost.getLinkTravelTime(testLink, 25.0*3600) + timecost.getWaitingTimeAtStop(testLink, 25.0*3600), MatsimTestCase.EPSILON);
		
		// planned departure at 47:00, has to wait until 05:22 = 53:22
		//original for matsim router 
		//assertEquals(22.0*60 + 6.0*3600 + 7.0*60, timecost.getLinkTravelTime(testLink, 47.0*3600), MatsimTestCase.EPSILON);
		//with separate waiting time
		assertEquals(22.0*60 + 6.0*3600 + 7.0*60, timecost.getLinkTravelTime(testLink, 47.0*3600) + timecost.getWaitingTimeAtStop(testLink, 47.0*3600), MatsimTestCase.EPSILON);
		
		// planned departure at 49:00, has to wait until 05:22 = 53:22, tests explicitly > 2*MIDNIGHT
		//original for matsim router
		//assertEquals(22.0*60 + 4.0*3600 + 7.0*60, timecost.getLinkTravelTime(testLink, 49.0*3600), MatsimTestCase.EPSILON);
		//with separate waiting time
		assertEquals(22.0*60 + 4.0*3600 + 7.0*60, timecost.getLinkTravelTime(testLink, 49.0*3600) + timecost.getWaitingTimeAtStop(testLink, 49.0*3600), MatsimTestCase.EPSILON);
	}

	/*
	public void testTravelCostLineSwitch() {
		Fixture f = new Fixture();
		f.init();
		MyTransitRouterConfig myTRConfig = new MyTransitRouterConfig(f.scenario.getConfig().planCalcScore(),
				f.scenario.getConfig().plansCalcRoute(), f.scenario.getConfig().transitRouter(),
				f.scenario.getConfig().vspExperimental());
		AdaptedTransitRouterNetworkTravelTimeCost timecost = new AdaptedTransitRouterNetworkTravelTimeCost(myTRConfig);
		AdaptedTransitRouter adaptedRouter = new AdaptedTransitRouter(myTRConfig, f.schedule) ;
		TransitRouterNetwork routerNet = adaptedRouter.getTransitRouterNetwork();
		// find the link connecting C and D on the blue line
		TransitRouterNetworkLink testLink = null;
		for (TransitRouterNetworkLink link : routerNet.getLinks().values()) {
			if ((link.getLine() == null) &&
					(link.fromNode.stop.getStopFacility().getName().equals("C")) &&
					(link.toNode.stop.getStopFacility().getName().equals("C"))) {
				testLink = link;
			}
		}

		double oldCost = - myTRConfig.getUtilityOfLineSwitch_utl();
		double cost1 = timecost.getLinkGeneralizedTravelCost(testLink, 7.0*3600);
		myTRConfig.setUtilityOfLineSwitch_utl(0.0);
		double cost2 = timecost.getLinkGeneralizedTravelCost(testLink, 6.0*3600); // use different time because of internal caching effects
		assertEquals(oldCost, cost1 - cost2, MatsimTestCase.EPSILON);
		myTRConfig.setUtilityOfLineSwitch_utl(-40.125);
		double cost3 = timecost.getLinkGeneralizedTravelCost(testLink, 5.0*3600);
		assertEquals(40.125, cost3 - cost2, MatsimTestCase.EPSILON);
	}
	*/

	/*
	public void testTravelCostLineSwitch_AdditionalTransferTime() {
		Fixture f = new Fixture();
		f.init();
		TransitRouterConfig conf = new TransitRouterConfig(f.scenario.getConfig().planCalcScore(),
				f.scenario.getConfig().plansCalcRoute(), f.scenario.getConfig().transitRouter(),
				f.scenario.getConfig().vspExperimental());
		TransitRouterNetworkTravelTimeCost tc = new TransitRouterNetworkTravelTimeCost(conf);
		TransitRouterImpl router = new TransitRouterImpl(f.schedule, conf);
		TransitRouterNetwork routerNet = router.getTransitRouterNetwork();
		// find the link connecting C and D on the blue line
		TransitRouterNetworkLink testLink = null;
		for (TransitRouterNetworkLink link : routerNet.getLinks().values()) {
			if ((link.getLine() == null) &&
					(link.fromNode.stop.getStopFacility().getName().equals("C")) &&
					(link.toNode.stop.getStopFacility().getName().equals("C"))) {
				testLink = link;
			}
		}

		double oldCost = - conf.getUtilityOfLineSwitch_utl();
		double cost1 = tc.getLinkGeneralizedTravelCost(testLink, 7.0*3600);
		conf.setUtilityOfLineSwitch_utl(0.0);
		double cost2 = tc.getLinkGeneralizedTravelCost(testLink, 6.0*3600); // use different time because of internal caching effects
		assertEquals(oldCost, cost1 - cost2, MatsimTestCase.EPSILON);
		conf.additionalTransferTime = 120.0;
		double cost3 = tc.getLinkGeneralizedTravelCost(testLink, 5.0*3600);
		assertEquals(-120.0 * conf.getMarginalUtiltityOfWaiting_utl_s(), cost3 - cost2, MatsimTestCase.EPSILON);
		// test with custom value for utility of waiting, just in case too many of the default marginal utilities are 0.0
		conf.setMarginalUtiltityOfWaiting_utl_s(-12.0 / 3600.0);
		double cost4 = tc.getLinkGeneralizedTravelCost(testLink, 7.0*3600);
		assertEquals(120.0 * 12.0 / 3600.0, cost4 - cost2, MatsimTestCase.EPSILON);
	}
	*/

}
