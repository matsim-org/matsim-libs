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

import org.matsim.pt.router.TransitRouterNetwork;
import org.matsim.pt.router.TransitRouterNetwork.TransitRouterNetworkLink;
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
		//at 6:00 the link travel time = 540
		assertEquals(9.0*60, timecost.getLinkTravelTime(testLink, 6.0*3600), MatsimTestCase.EPSILON);
	}

	public void testVehArrivalTime() {
		Fixture f = new Fixture();
		f.init();
		MyTransitRouterConfig myTRConfig = new MyTransitRouterConfig(f.scenario.getConfig().planCalcScore(),
				f.scenario.getConfig().plansCalcRoute(), f.scenario.getConfig().transitRouter(),
				f.scenario.getConfig().vspExperimental());
		AdaptedTransitRouterNetworkTravelTimeCost timecost = new AdaptedTransitRouterNetworkTravelTimeCost(myTRConfig);
		AdaptedTransitRouter adaptedRouter = new AdaptedTransitRouter(myTRConfig, f.schedule) ;
		TransitRouterNetwork adapRouterNet = adaptedRouter.getTransitRouterNetwork();
	
		// find the link connecting C and D on the blue line
		TransitRouterNetworkLink testLink = null;
		for (TransitRouterNetworkLink link : adapRouterNet.getLinks().values()) {
			if ((link.getLine() == f.blueLine) && (link.fromNode.stop.getStopFacility().getName().equals("C")) && (link.toNode.stop.getStopFacility().getName().equals("D"))) {
				testLink = link;
			}
		}
		
		//original MATSim
		//assertEquals(2.0*60 + 7.0*60, timecost.getLinkTravelTime(testLink, 6.0*3600), MatsimTestCase.EPSILON);
		//with adaption   (link travel time is not departureTime dependent anymore) 
		//link travel time = 9 mins    
		assertEquals(9.0*60 , timecost.getLinkTravelTime(testLink, 6.0*3600) , MatsimTestCase.EPSILON);
		
		/*
		//if the agents arrives to stop (in or outside the veh) before  06:02, his/her "arrrival veh time" is 05:58
		//veh arrival time to stop= 05:58 
		//veh departure from stop = 06:02 
		//System.out.println("veh arr: " + timecost.getVehArrivalTime(testLink , (6.0*3600) + (1*60)));
		//System.out.println("next departure: " + timecost.getNextDepartureTime(testLink.getRoute(), testLink.getFromNode().getStop(), (6.0*3600) + 60));
		*/
		
		//this arrival would be for waiting outside the vehicle at the stop
		assertEquals((5.0*3600) + (58*60) , timecost.getVehArrivalTime(testLink, (5.0*3600) + (56*60)), MatsimTestCase.EPSILON);
		assertEquals((5.0*3600) + (58*60) , timecost.getVehArrivalTime(testLink, (5.0*3600) + (57*60)), MatsimTestCase.EPSILON);
		
		//this arrival would be for waiting inside the vehicle
		assertEquals((5.0*3600) + (58*60) , timecost.getVehArrivalTime(testLink, (5.0*3600) + (58*60)), MatsimTestCase.EPSILON);
		assertEquals((5.0*3600) + (58*60) , timecost.getVehArrivalTime(testLink, (5.0*3600) + (59*60)), MatsimTestCase.EPSILON);
		assertEquals((5.0*3600) + (58*60) , timecost.getVehArrivalTime(testLink, (6.0*3600) + (1*60)), MatsimTestCase.EPSILON);
		assertEquals((5.0*3600) + (58*60) , timecost.getVehArrivalTime(testLink, (6.0*3600) + (2*60)), MatsimTestCase.EPSILON);
	
		//if the agents arrives after to stop after 06:02, his/her "arrrival veh time" is displaced to 06:18
		assertEquals((6.0*3600) + (18*60)  , timecost.getVehArrivalTime(testLink, (6.0*3600) + (3*60)), MatsimTestCase.EPSILON);
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
		//original matsim (in current adaption link Travel Time is not dependent of departure time)
		// planned departure at 25:00, has to wait until 05:22 = 29:22
		//assertEquals(22.0*60 + 4.0*3600 + 7.0*60, timecost.getLinkTravelTime(testLink, 25.0*3600), MatsimTestCase.EPSILON);
		//after adaption : next veh arrival is in 29:18
		assertEquals((29.0*3600) + (18*60.0) , timecost.getVehArrivalTime(testLink, 25.0*3600), MatsimTestCase.EPSILON);
		
		//original 
		// planned departure at 47:00, has to wait until 05:22 = 53:22
		//assertEquals(22.0*60 + 6.0*3600 + 7.0*60, tc.getLinkTravelTime(testLink, 47.0*3600), MatsimTestCase.EPSILON);
		//after adaption : next veh arrival is in 53:18
		assertEquals((53.0*3600) + (18*60.0), timecost.getVehArrivalTime(testLink, 47.0*3600), MatsimTestCase.EPSILON);

		//original 
		// planned departure at 49:00, has to wait until 05:22 = 53:22, tests explicitly > 2*MIDNIGHT
		//assertEquals(22.0*60 + 4.0*3600 + 7.0*60, timecost.getLinkTravelTime(testLink, 49.0*3600), MatsimTestCase.EPSILON);
		//after adaption : next veh arrival is in 53:18
		assertEquals((53.0*3600) + (18*60.0), timecost.getVehArrivalTime(testLink, 49.0*3600), MatsimTestCase.EPSILON);
	}

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
		double cost1 = timecost.getLinkTravelDisutility(testLink, 7.0*3600, null, null);
		myTRConfig.setUtilityOfLineSwitch_utl(0.0);
		double cost2 = timecost.getLinkTravelDisutility(testLink, 6.0*3600, null, null); // use different time because of internal caching effects
		assertEquals(oldCost, cost1 - cost2, MatsimTestCase.EPSILON);
		myTRConfig.setUtilityOfLineSwitch_utl(-40.125);
		double cost3 = timecost.getLinkTravelDisutility(testLink, 5.0*3600, null, null);
		assertEquals(40.125, cost3 - cost2, MatsimTestCase.EPSILON);
	}

	
	public void testTravelCostLineSwitch_AdditionalTransferTime() {
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
		double cost1 = timecost.getLinkTravelDisutility(testLink, 7.0*3600, null, null);
		myTRConfig.setUtilityOfLineSwitch_utl(0.0);
		double cost2 = timecost.getLinkTravelDisutility(testLink, 6.0*3600, null, null); // use different time because of internal caching effects
		assertEquals(oldCost, cost1 - cost2, MatsimTestCase.EPSILON);
		myTRConfig.additionalTransferTime = 120.0;
		double cost3 = timecost.getLinkTravelDisutility(testLink, 5.0*3600, null, null);
		assertEquals(-120.0 * myTRConfig.getMarginalUtiltityOfWaiting_utl_s(), cost3 - cost2, MatsimTestCase.EPSILON);
		// test with custom value for utility of waiting, just in case too many of the default marginal utilities are 0.0
		myTRConfig.setMarginalUtiltityOfWaiting_utl_s(-12.0 / 3600.0);
		double cost4 = timecost.getLinkTravelDisutility(testLink, 7.0*3600, null, null);
		assertEquals(120.0 * 12.0 / 3600.0, cost4 - cost2, MatsimTestCase.EPSILON);
	}


}
