/* *********************************************************************** *
 * project: org.matsim.*
 * KtiTravelTimeDistanceCostCalculatorTest.java
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

package playground.meisterk.kti.router;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.LinkEnterEventImpl;
import org.matsim.core.events.LinkLeaveEventImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.core.trafficmonitoring.TravelTimeCalculatorFactory;
import org.matsim.core.trafficmonitoring.TravelTimeCalculatorFactoryImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.misc.Time;
import org.matsim.testcases.MatsimTestCase;

import playground.meisterk.kti.config.KtiConfigGroup;

public class KtiTravelTimeDistanceCostCalculatorTest extends MatsimTestCase {

	private static final Id TEST_LINK_ID = new IdImpl(1);
	private static final Id DUMMY_PERSON_ID = new IdImpl(1000);

	private NetworkImpl network = null;
	private KtiTravelTimeDistanceCostCalculator testee = null;
	private EventsManager events = null;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.network = NetworkImpl.createNetwork();
		Node node1 = network.createAndAddNode(new IdImpl(1), new CoordImpl(1000.0, 1000.0));
		Node node2 = network.createAndAddNode(new IdImpl(2), new CoordImpl(2000.0, 1000.0));
		network.createAndAddLink(TEST_LINK_ID, node1, node2, 1000.0, 50.0/3.6, 2000.0, 1);

		Config config = new Config();
		config = super.loadConfig(null);
		KtiConfigGroup ktiConfigGroup = new KtiConfigGroup();
		config.addModule(KtiConfigGroup.GROUP_NAME, ktiConfigGroup);

		ktiConfigGroup.setDistanceCostCar(5.0);

//		config.charyparNagelScoring().setMarginalUtlOfDistanceCar(-0.5);
		config.planCalcScore().setMonetaryDistanceCostRateCar(-0.5) ;
		config.planCalcScore().setMarginalUtilityOfMoney(1.) ;

		TravelTimeCalculatorFactory travelTimeCalculatorFactory = new TravelTimeCalculatorFactoryImpl();
		TravelTimeCalculator travelTimeCalculator = travelTimeCalculatorFactory.createTravelTimeCalculator(
				this.network,
				config.travelTimeCalculator());

		this.events = new EventsManagerImpl();
		this.events.addHandler(travelTimeCalculator);

		KtiTravelCostCalculatorFactory costCalculatorFactory = new KtiTravelCostCalculatorFactory(ktiConfigGroup);
		this.testee =
			(KtiTravelTimeDistanceCostCalculator) costCalculatorFactory.createTravelCostCalculator(
					travelTimeCalculator,
					config.planCalcScore());


	}

	@Override
	protected void tearDown() throws Exception {
		this.testee = null;
		this.events = null;
		this.network = null;
		super.tearDown();
	}

	public void testKtiTravelTimeDistanceCostCalculator() {

		assertEquals(-0.0025, testee.getMarginalUtlOfDistance(), MatsimTestCase.EPSILON);
		assertEquals(0.003333334, testee.getTravelCostFactor(), 1e-6);

	}

	public void testGetLinkMinimumTravelCost() {

		double actualMinimumCost = testee.getLinkMinimumTravelCost(this.network.getLinks().get(TEST_LINK_ID));
		assertEquals(2.74, actualMinimumCost, MatsimTestCase.EPSILON);
	}

	public void testGetLinkTravelCost() {

		LinkEnterEvent enter = new LinkEnterEventImpl(Time.parseTime("06:01:00"), DUMMY_PERSON_ID, TEST_LINK_ID);
		this.events.processEvent(enter);
		LinkLeaveEvent leave = new LinkLeaveEventImpl(Time.parseTime("06:21:00"), DUMMY_PERSON_ID, TEST_LINK_ID);
		this.events.processEvent(leave);

		double expectedLinkTravelCost = 6.5;

		double actualLinkTravelCost = this.testee.getLinkGeneralizedTravelCost(this.network.getLinks().get(TEST_LINK_ID), Time.parseTime("06:10:00"));
		assertEquals(expectedLinkTravelCost, actualLinkTravelCost, MatsimTestCase.EPSILON);

		actualLinkTravelCost = this.testee.getLinkGeneralizedTravelCost(this.network.getLinks().get(TEST_LINK_ID), Time.parseTime("05:55:55"));
		assertEquals(2.74, actualLinkTravelCost, MatsimTestCase.EPSILON);

		actualLinkTravelCost = this.testee.getLinkGeneralizedTravelCost(this.network.getLinks().get(TEST_LINK_ID), Time.parseTime("06:31:00"));
		assertEquals(2.74, actualLinkTravelCost, MatsimTestCase.EPSILON);

	}

}
