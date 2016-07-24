/* *********************************************************************** *
 * project: org.matsim.*
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

/**
 * 
 */
package playground.ikaddoura.analysis.extCost;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;

import playground.vsp.congestion.analysis.CongestionAnalysisEventHandler;
import playground.vsp.congestion.events.CongestionEventsReader;

/**
 * @author ikaddoura , lkroeger
 */

public class ExtCostEventHandlerTest {
	
//	private static final Logger log = Logger.getLogger(ExtCostEventHandlerTest.class);
	
	@Rule
	public MatsimTestUtils testUtils = new MatsimTestUtils();
	
	private Id<Link> link1 = Id.create("link1", Link.class);
	private Id<Link> link2 = Id.create("link2", Link.class);
	private Id<Link> link3 = Id.create("link3", Link.class);
	private Id<Link> link4 = Id.create("link4", Link.class);
	
	private EventsManager events;

	// two agents: testAgent1 (one trip), testAgent2 (three trips), testAgent3 (three trips, but only one trip with MoneyEvents) 
	// testing, if more than one agent can be handled,
	// if more than (one or) two trips per agent can be handled
	// if the 
	@Test
	public final void test3agents() {
		
		Scenario sc = loadScenario1();
		
		String eventsFile = testUtils.getPackageInputDirectory()+"ExtCostEventHandlerTest/events01.xml";
		
		EventsManager events = EventsUtils.createEventsManager();

		CongestionAnalysisEventHandler handler = new CongestionAnalysisEventHandler(sc, true);
		events.addHandler(handler);
		
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(eventsFile);
		
		CongestionEventsReader congestionEventsReader = new CongestionEventsReader(events);		
		congestionEventsReader.readFile(eventsFile);
	
		Map<Double, Double> tripDepTime2avgAmount = handler.getAvgAmountPerTripDepartureTime(TransportMode.car);
		Map<Double, Double> tripDistance2avgAmount = handler.getAvgAmountPerTripDistance(TransportMode.car);
		
		double sumAvgAmountsTimeBased = 0.;
		double sumAvgAmountsDistanceBased = 0.;
		
		for(Double time : tripDepTime2avgAmount.keySet()) {
			sumAvgAmountsTimeBased = sumAvgAmountsTimeBased + tripDepTime2avgAmount.get(time);
		}
		for(Double distance : tripDistance2avgAmount.keySet()) {
			sumAvgAmountsDistanceBased = sumAvgAmountsDistanceBased + tripDistance2avgAmount.get(distance);
		}
		
		Assert.assertEquals("wrong average amount", 2.666666666666, tripDepTime2avgAmount.get(36900.0), MatsimTestUtils.EPSILON);
		Assert.assertEquals("wrong average amount", 8, tripDepTime2avgAmount.get(47700.0), MatsimTestUtils.EPSILON);
		Assert.assertEquals("wrong average amount", 101.5, tripDepTime2avgAmount.get(54000.0), MatsimTestUtils.EPSILON);

		double a = 2.666666666666 + 8 + 101.5;
		
		Assert.assertEquals("wrong average amount", 101.5, tripDistance2avgAmount.get(1500.0), MatsimTestUtils.EPSILON);
		Assert.assertEquals("wrong average amount", 2.666666666666, tripDistance2avgAmount.get(2500.0), MatsimTestUtils.EPSILON);
		Assert.assertEquals("wrong average amount", 8, tripDistance2avgAmount.get(5500.0), MatsimTestUtils.EPSILON);
		
		double b = 101.5 + 2.666666666666 + 8;
		
		// Checking if there are no other entries
		Assert.assertEquals("any other entries", a, sumAvgAmountsTimeBased, MatsimTestUtils.EPSILON);
		Assert.assertEquals("any other entries", b, sumAvgAmountsDistanceBased, MatsimTestUtils.EPSILON);
	}
	
	// two agents (one agent with one pt and one car trip, another agent with three pt-trips)
	// two pt-vehicles
	// one trip with transition between the different pt-vehicles
	@Test
	public final void testPtAndCar() {
		Scenario sc = loadScenario1();
		
		String eventsFile = testUtils.getPackageInputDirectory()+"ExtCostEventHandlerTest/events02.xml";
		
		EventsManager events = EventsUtils.createEventsManager();

		CongestionAnalysisEventHandler handler = new CongestionAnalysisEventHandler(sc, true);
		events.addHandler(handler);
		
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(eventsFile);
		
		CongestionEventsReader congestionEventsReader = new CongestionEventsReader(events);		
		congestionEventsReader.readFile(eventsFile);
	
		Map<Double, Double> tripDepTime2avgAmountCar = handler.getAvgAmountPerTripDepartureTime(TransportMode.car);
		Map<Double, Double> tripDistance2avgAmountCar = handler.getAvgAmountPerTripDistance(TransportMode.car);
		Map<Double, Double> tripDepTime2avgAmountPt = handler.getAvgAmountPerTripDepartureTime(TransportMode.pt);
		Map<Double, Double> tripDistance2avgAmountPt = handler.getAvgAmountPerTripDistance(TransportMode.pt);
		
		double sumAvgAmountsTimeBasedPt = 0.;
		double sumAvgAmountsDistanceBasedPt = 0.;
		double sumAvgAmountsTimeBasedCar = 0.;
		double sumAvgAmountsDistanceBasedCar = 0.;
		
		for(Double time : tripDepTime2avgAmountPt.keySet()) {
			sumAvgAmountsTimeBasedPt = sumAvgAmountsTimeBasedPt + tripDepTime2avgAmountPt.get(time);
		}
		for(Double distance : tripDistance2avgAmountPt.keySet()) {
			sumAvgAmountsDistanceBasedPt = sumAvgAmountsDistanceBasedPt + tripDistance2avgAmountPt.get(distance);
		}
		for(Double time : tripDepTime2avgAmountCar.keySet()) {
			sumAvgAmountsTimeBasedCar = sumAvgAmountsTimeBasedCar + tripDepTime2avgAmountCar.get(time);
		}
		for(Double distance : tripDistance2avgAmountCar.keySet()) {
			sumAvgAmountsDistanceBasedCar = sumAvgAmountsDistanceBasedCar + tripDistance2avgAmountCar.get(distance);
		}
		
		Assert.assertEquals("wrong average amount", 25.0, tripDepTime2avgAmountPt.get(57600.0), MatsimTestUtils.EPSILON);
		Assert.assertEquals("wrong average amount", 400.0, tripDepTime2avgAmountPt.get(70200.0), MatsimTestUtils.EPSILON);

		double a = 25 + 400;
		
		Assert.assertEquals("wrong average amount", 20.0, tripDistance2avgAmountPt.get(5500.0), MatsimTestUtils.EPSILON);
		Assert.assertEquals("wrong average amount", 30.0, tripDistance2avgAmountPt.get(6500.0), MatsimTestUtils.EPSILON);
		Assert.assertEquals("wrong average amount", 400.0, tripDistance2avgAmountPt.get(10500.0), MatsimTestUtils.EPSILON);
		
		double b = 20.0 + 30.0 + 400.0;
		
		Assert.assertEquals("wrong average amount", 52.0, tripDepTime2avgAmountCar.get(63900.0), MatsimTestUtils.EPSILON);
		
		double c = 52;
		
		Assert.assertEquals("wrong average amount", 52.0, tripDistance2avgAmountCar.get(1500.0), MatsimTestUtils.EPSILON);
		
		double d = 52;
		
		// Checking if there are no other entries
		Assert.assertEquals("any other entries", a, sumAvgAmountsTimeBasedPt, MatsimTestUtils.EPSILON);
		Assert.assertEquals("any other entries", b, sumAvgAmountsDistanceBasedPt, MatsimTestUtils.EPSILON);
		Assert.assertEquals("any other entries", c, sumAvgAmountsTimeBasedCar, MatsimTestUtils.EPSILON);
		Assert.assertEquals("any other entries", d, sumAvgAmountsDistanceBasedCar, MatsimTestUtils.EPSILON);
	}

//	// TODO: Is this test still necessary? 
//	// The boolean useMoneyEvents shouldn't have any influence on the results
//	@Test
//	public final void testUseMoneyEvents() {
//		
//	}

	// ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
	
	private Scenario loadScenario1() {
		
		// 				
		// (4) <- link3 <- (3)
		//  |				A
		//	V				|
		// link4		  link2
		//	|				A
		//  V				|
		// (1) -> link1 -> (2)
		//
		
		Config config = testUtils.createConfig();
		QSimConfigGroup qSimConfigGroup = config.qsim();
		qSimConfigGroup.setFlowCapFactor(1.0);
		qSimConfigGroup.setStorageCapFactor(1.0);
		qSimConfigGroup.setInsertingWaitingVehiclesBeforeDrivingVehicles(true);
		qSimConfigGroup.setRemoveStuckVehicles(true);
		qSimConfigGroup.setStuckTime(3600.0);
		Scenario scenario = (ScenarioUtils.createScenario(config));
	
		Network network = (Network) scenario.getNetwork();
		network.setEffectiveCellSize(7.5);
		network.setCapacityPeriod(3600.);

		Node node1 = network.getFactory().createNode(Id.create("nodeId1", Node.class), new Coord(0., 0.));
		Node node2 = network.getFactory().createNode(Id.create("nodeId2", Node.class), new Coord(1000., 0.));
		Node node3 = network.getFactory().createNode(Id.create("nodeId3", Node.class), new Coord(1000., 1000.));
		Node node4 = network.getFactory().createNode(Id.create("nodeId4", Node.class), new Coord(0., 1000.));
		
		Link link1 = network.getFactory().createLink(this.link1, node1, node2);
		Link link2 = network.getFactory().createLink(this.link2, node2, node3);
		Link link3 = network.getFactory().createLink(this.link3, node3, node4);
		Link link4 = network.getFactory().createLink(this.link4, node4, node1);
		
		Set<String> modes = new HashSet<String>();
		modes.add("car");
		
		// link 1
		link1.setAllowedModes(modes);
		link1.setCapacity(3600);
		link1.setFreespeed(10);
		link1.setNumberOfLanes(100);
		link1.setLength(1000);
		
		// link 2
		link2.setAllowedModes(modes);
		link2.setCapacity(3600);
		link2.setFreespeed(10);
		link2.setNumberOfLanes(100);
		link2.setLength(1000);
				
		// link 3
		link3.setAllowedModes(modes);
		link3.setCapacity(3600);
		link3.setFreespeed(10);
		link3.setNumberOfLanes(100);
		link3.setLength(1000);
				
		// link 4
		link4.setAllowedModes(modes);
		link4.setCapacity(3600);
		link4.setFreespeed(10);
		link4.setNumberOfLanes(100);
		link4.setLength(1000);
		
		network.addNode(node1);
		network.addNode(node2);
		network.addNode(node3);
		network.addNode(node4);

		network.addLink(link1);
		network.addLink(link2);
		network.addLink(link3);
		network.addLink(link4);

		this.events = EventsUtils.createEventsManager();
		return scenario;
	}
	
}
