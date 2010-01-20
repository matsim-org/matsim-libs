/* *********************************************************************** *
 * project: org.matsim.*
 * LinearInterpolationLegTravelTimeEstimatorTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package org.matsim.planomat.costestimators;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlanomatConfigGroup;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.LinkEnterEventImpl;
import org.matsim.core.events.LinkLeaveEventImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.router.costcalculators.TravelTimeDistanceCostCalculator;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.misc.Time;
import org.matsim.testcases.MatsimTestCase;

public class LinearInterpolationLegTravelTimeEstimatorTest extends MatsimTestCase {

	private static final Id FIRST_LINK_ID = new IdImpl("1002");
	private static final Id HIGHWAY_LINK_ID = new IdImpl("4005");
	
	private final static Logger logger = Logger.getLogger(LinearInterpolationLegTravelTimeEstimatorTest.class);

	@Override
	protected void setUp() throws Exception {
		super.setUp();
	}

	public void testGetLegTravelTimeEstimation() {

		Config config = super.loadConfig(null);

		NetworkLayer network = this.createNetwork();
		logger.info(network.toString());
		
		DepartureDelayAverageCalculator tDepDelayCalc = new DepartureDelayAverageCalculator(network, 900);
		TravelTimeCalculator linkTravelTimeEstimator = new TravelTimeCalculator(network, config.travelTimeCalculator());
		TravelCost linkTravelCostEstimator = new TravelTimeDistanceCostCalculator(linkTravelTimeEstimator, config.charyparNagelScoring());

		PlansCalcRoute plansCalcRoute = new PlansCalcRoute(
				config.plansCalcRoute(), 
				network, 
				linkTravelCostEstimator, 
				linkTravelTimeEstimator);

		LegTravelTimeEstimatorFactory legTravelTimeEstimatorFactory = new LegTravelTimeEstimatorFactory(linkTravelTimeEstimator, tDepDelayCalc);
		LinearInterpolationLegTravelTimeEstimator testee = (LinearInterpolationLegTravelTimeEstimator) legTravelTimeEstimatorFactory.getLegTravelTimeEstimator(
				null,
				config.planomat().getSimLegInterpretation(),
				PlanomatConfigGroup.RoutingCapability.linearInterpolation,
				plansCalcRoute,
				network);

		testee.setDoLogging(true);
		
		Id dummyPersonId = new IdImpl(123456);
		ActivityImpl homeActivity = new ActivityImpl("home", new IdImpl("1002"));
		homeActivity.setCoord(new CoordImpl(5000.0, 10000.0));
		ActivityImpl workActivity = new ActivityImpl("work", new IdImpl("5006"));
		workActivity.setCoord(new CoordImpl(35000.0, 10000.0));
		
		for (TransportMode mode : new TransportMode[]{TransportMode.car, TransportMode.pt, TransportMode.bike, TransportMode.walk, TransportMode.car}) {
			logger.info(mode.toString());
			for (String str : new String[] { 
					"06:10:00", 
					"07:00:00",
					"08:15:00", 
					"23:00:00", 
					"06:15:00"}) {

				LegImpl legIntermediate = new LegImpl(mode);
				
				double travelTime = testee.getLegTravelTimeEstimation(
						dummyPersonId, Time.parseTime(str), homeActivity,
						workActivity, legIntermediate, Boolean.FALSE);
				
				switch(mode) {
				case car:
					assertEquals(Time.parseTime("02:00:00"), travelTime);
					break;
				case pt:
					assertEquals(Time.parseTime("04:00:00"), travelTime);
					break;
				case bike:
					assertEquals(Time.parseTime("01:59:59"), travelTime);
					break;
				case walk:
					assertEquals(Time.parseTime("10:00:00"), travelTime);
					break;
				}
				
				logger.info(str + "\t" + Time.writeTime(travelTime));
				
			}
			logger.info("");
		}
		logger.info("");
		
		testee.resetPlanSpecificInformation();
		
		// now let's repeat the same stuff with some events that indicate a very long travel time on the highway
		// the result must be the free speed travel time of the alternate route
		EventsManagerImpl events = new EventsManagerImpl();
		events.addHandler(linkTravelTimeEstimator);
		
		events.processEvent(new LinkEnterEventImpl(Time.parseTime("06:50:00"), dummyPersonId, HIGHWAY_LINK_ID));
		events.processEvent(new LinkLeaveEventImpl(Time.parseTime("07:49:00"), dummyPersonId, HIGHWAY_LINK_ID));
		
		LegImpl legIntermediate = new LegImpl(TransportMode.car);
		for (String str : new String[] { 
				"06:10:00", 
				"07:00:00",
				"08:15:00", 
				"23:00:00", 
				"06:15:00"}) {
			
			double travelTime = testee.getLegTravelTimeEstimation(
					dummyPersonId, Time.parseTime(str), homeActivity,
					workActivity, legIntermediate, Boolean.FALSE);
			logger.info(str + "\t" + Time.writeTime(travelTime));
			
			if (str.equals("06:10:00")) {
				assertEquals("02:20:00", Time.writeTime(travelTime));
			} else if (str.equals("06:15:00")) {
				assertEquals("02:18:00", Time.writeTime(travelTime));
			} else {
				assertEquals("02:00:00", Time.writeTime(travelTime));
			}
			
		}
		logger.info("");
		
	}
	
	private NetworkLayer createNetwork() {

		NetworkLayer network = new NetworkLayer();

		for (int nodeId = 1; nodeId <= 6; nodeId++) {
			double x = 0.0, y = 0.0;
			switch(nodeId) {
			case 1:
				x = 0.0; y = 10000.0; break;
			case 2:
				x = 10000.0; y = 10000.0; break;
			case 3:
				x = 20000.0; y = 20000.0; break;
			case 4:
				x = 20000.0; y = 0.0; break;
			case 5:
				x = 30000.0; y = 10000.0; break;
			case 6:
				x = 40000.0; y = 10000.0; break;
			}
			network.createAndAddNode(new IdImpl(nodeId), new CoordImpl(x, y));
			logger.info(network.getNodes().get(new IdImpl(nodeId)).toString());
		}

		for (int[] nodePair : new int[][]{{1, 2}, {2, 3}, {2, 4}, {3, 5}, {4, 5}, {5, 6}}) {
			
			IdImpl linkId = new IdImpl(nodePair[0] * 1000 + nodePair[1]);
			
			double freespeed = 0.0;
			if (linkId.equals(HIGHWAY_LINK_ID)) {
				freespeed = 100.0 / 3.6;
			} else if (linkId.equals(FIRST_LINK_ID)) {
				freespeed = 80.0 / 3.6;
			} else {
				freespeed = 50.0 / 3.6;
			}
			
			network.createAndAddLink(
					linkId, 
					network.getNodes().get(new IdImpl(nodePair[0])), 
					network.getNodes().get(new IdImpl(nodePair[1])), 
					40000.0, 
					freespeed, 
					1000.0, 
					1);
			
			logger.info(network.getLinks().get(linkId).toString());
		}

		return network;
		
	}
	
}
