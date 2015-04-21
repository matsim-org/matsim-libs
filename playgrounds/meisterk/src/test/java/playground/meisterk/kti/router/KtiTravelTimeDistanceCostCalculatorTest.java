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
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.misc.Time;
import org.matsim.testcases.MatsimTestCase;
import playground.meisterk.kti.config.KtiConfigGroup;

public class KtiTravelTimeDistanceCostCalculatorTest extends MatsimTestCase {

	private static final Id<Link> TEST_LINK_ID = Id.create(1, Link.class);
	private static final Id<Person> DUMMY_PERSON_ID = Id.create(1000, Person.class);

	private NetworkImpl network = null;
	private KtiTravelTimeDistanceCostCalculator testee = null;
	private EventsManager events = null;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.network = NetworkImpl.createNetwork();
		Node node1 = network.createAndAddNode(Id.create(1, Node.class), new CoordImpl(1000.0, 1000.0));
		Node node2 = network.createAndAddNode(Id.create(2, Node.class), new CoordImpl(2000.0, 1000.0));
		network.createAndAddLink(TEST_LINK_ID, node1, node2, 1000.0, 50.0/3.6, 2000.0, 1);

		Config config = new Config();
		config = super.loadConfig(null);
		KtiConfigGroup ktiConfigGroup = new KtiConfigGroup();
		config.addModule(ktiConfigGroup);

		ktiConfigGroup.setDistanceCostCar(5.0);

//		config.charyparNagelScoring().setMarginalUtlOfDistanceCar(-0.5);
		config.planCalcScore().setMonetaryDistanceCostRateCar(-0.5) ;
		config.planCalcScore().setMarginalUtilityOfMoney(1.) ;

		TravelTimeCalculator travelTimeCalculator = TravelTimeCalculator.create(this.network, config.travelTimeCalculator());

		this.events = EventsUtils.createEventsManager();
		this.events.addHandler(travelTimeCalculator);

		KtiTravelCostCalculatorFactory costCalculatorFactory = new KtiTravelCostCalculatorFactory(ktiConfigGroup);
		this.testee =
			(KtiTravelTimeDistanceCostCalculator) costCalculatorFactory.createTravelDisutility(
					travelTimeCalculator.getLinkTravelTimes(),
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

		double actualMinimumCost = testee.getLinkMinimumTravelDisutility(this.network.getLinks().get(TEST_LINK_ID));
		assertEquals(2.74, actualMinimumCost, MatsimTestCase.EPSILON);
	}

	public void testGetLinkTravelCost() {

		LinkEnterEvent enter = new LinkEnterEvent(Time.parseTime("06:01:00"), DUMMY_PERSON_ID, TEST_LINK_ID, null);
		this.events.processEvent(enter);
		LinkLeaveEvent leave = new LinkLeaveEvent(Time.parseTime("06:21:00"), DUMMY_PERSON_ID, TEST_LINK_ID, null);
		this.events.processEvent(leave);

		double expectedLinkTravelCost = 6.5;

		double actualLinkTravelCost = this.testee.getLinkTravelDisutility(this.network.getLinks().get(TEST_LINK_ID), Time.parseTime("06:10:00"), null, null);
		assertEquals(expectedLinkTravelCost, actualLinkTravelCost, MatsimTestCase.EPSILON);

		actualLinkTravelCost = this.testee.getLinkTravelDisutility(this.network.getLinks().get(TEST_LINK_ID), Time.parseTime("05:55:55"), null, null);
		assertEquals(2.74, actualLinkTravelCost, MatsimTestCase.EPSILON);

		actualLinkTravelCost = this.testee.getLinkTravelDisutility(this.network.getLinks().get(TEST_LINK_ID), Time.parseTime("06:31:00"), null, null);
		assertEquals(2.74, actualLinkTravelCost, MatsimTestCase.EPSILON);

	}

}
