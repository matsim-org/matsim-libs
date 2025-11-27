/* *********************************************************************** *
 * project: org.matsim.*
 * SimulationConfigGroup.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2025 by the members listed in the COPYING,        *
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

package org.matsim.core.events;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.PersonInitializedEvent;
import org.matsim.api.core.v01.events.handler.PersonInitializedEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup.PersonInitializedEventsSetting;
import org.matsim.core.config.groups.ScoringConfigGroup.ActivityParams;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;

public class PersonInitializedEventTest {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();


	@Test
	void testWriteReadXml() {
		final PersonInitializedEvent event1 = new PersonInitializedEvent(0, Id.createPersonId("testPerson"));
		final PersonInitializedEvent event2 = XmlEventsTester.testWriteReadXml(utils.getOutputDirectory() + "events.xml", event1);
		assertEquals(event1.getTime(), event2.getTime(), MatsimTestUtils.EPSILON);
		assertEquals(event1.getPersonId(), event2.getPersonId());
		assertNull(event2.getCoord());
	}
	
	@Test
	void testWriteReadXmlWithCoord() {
		final PersonInitializedEvent event1 = new PersonInitializedEvent(0, Id.createPersonId("testPerson"), new Coord(12345, 67890));
		final PersonInitializedEvent event2 = XmlEventsTester.testWriteReadXml(utils.getOutputDirectory() + "events.xml", event1);
		assertEquals(event1.getTime(), event2.getTime(), MatsimTestUtils.EPSILON);
		assertEquals(event1.getPersonId(), event2.getPersonId());
		assertEquals(event1.getCoord(), event2.getCoord());
	}
	
	@ParameterizedTest
	@EnumSource
	void testQSimSettings(PersonInitializedEventsSetting setting) {
		Controler controler = new Controler(createScenario());
		Config config = controler.getConfig();
		config.qsim().setPersonInitializedEventsSetting(setting);
		config.controller().setCreateGraphsInterval(0);
		config.controller().setDumpDataAtEnd(true);
		config.controller().setWritePlansInterval(0);
		config.controller().setLastIteration(0);
		config.controller().setOutputDirectory(utils.getOutputDirectory() + "/" + setting.name());
		
		PersonInitializedEventCollector collector = new PersonInitializedEventCollector();
		controler.addOverridingModule(new AbstractModule() {
			@Override public void install() {
				this.addEventHandlerBinding().toInstance(collector);
			}
		});
		controler.run();
		
		switch(setting) {
		case none:
			assertTrue(collector.events.isEmpty());
			break;
		case singleActAgentsOnly:
		{
			assertEquals(1, collector.events.size());
			PersonInitializedEvent stationaryPersonEvent = collector.events.get("stationary");
			assertNotNull(stationaryPersonEvent);
			assertEquals(0, stationaryPersonEvent.getTime(), 0);
			assertEquals(new Coord(0, 0), stationaryPersonEvent.getCoord());
			break;
		}
		case all:
		{
			assertEquals(2, collector.events.size());
			PersonInitializedEvent stationaryPersonEvent = collector.events.get("stationary");
			assertNotNull(stationaryPersonEvent);
			assertEquals(0, stationaryPersonEvent.getTime(), 0);
			assertEquals(new Coord(0, 0), stationaryPersonEvent.getCoord());
			PersonInitializedEvent mobilePersonEvent = collector.events.get("mobile");
			assertNotNull(mobilePersonEvent);
			assertEquals(0, mobilePersonEvent.getTime(), 0);
			assertEquals(new Coord(-100, 0), mobilePersonEvent.getCoord());
			break;
		}
		default:
			throw new RuntimeException("Please add a test for enum value " + setting.name());
		}
	}
	
	private static class PersonInitializedEventCollector implements PersonInitializedEventHandler {
		Map<String, PersonInitializedEvent> events = new HashMap<>();
		@Override
		public void reset(int iteration) {
			events.clear();
		}
		@Override
		public void handleEvent(PersonInitializedEvent event) {
			events.put(event.getPersonId().toString(), event);
		}
	}
	
	/**
	 * Creates a scenario with two agents, one doing home all day, one switching its location from one act to another.
	 * Also simulates a simple pt setup to ensure that the driver agents wont cause events
	 * 
	 * @return
	 */
	private static Scenario createScenario() {
		Config config = ConfigUtils.createConfig();
		ActivityParams homeParams = new ActivityParams("home");
		homeParams.setTypicalDuration(100);
		config.scoring().addActivityParams(homeParams);
		config.transit().setUseTransit(true);
		
		Scenario scen = ScenarioUtils.createScenario(config);
		PopulationFactory popFactory = scen.getPopulation().getFactory();
		{
			Person stationaryPerson = popFactory.createPerson(Id.createPersonId("stationary"));
			Plan stationaryPlan = popFactory.createPlan();
			stationaryPlan.addActivity(popFactory.createActivityFromCoord("home", new Coord(0, 0)));
			stationaryPerson.addPlan(stationaryPlan);
			scen.getPopulation().addPerson(stationaryPerson);
		}
		{
			Person mobilePerson = popFactory.createPerson(Id.createPersonId("mobile"));
			Plan mobilePlan = popFactory.createPlan();
			Activity act1 = popFactory.createActivityFromCoord("home", new Coord(-100, 0));
			act1.setEndTime(100);
			Leg leg = popFactory.createLeg("walk");
			Activity act2 = popFactory.createActivityFromCoord("home", new Coord(50, 50));
			act1.setStartTime(500);
			mobilePlan.addActivity(act1);
			mobilePlan.addLeg(leg);
			mobilePlan.addActivity(act2);
			mobilePerson.addPlan(mobilePlan);
			scen.getPopulation().addPerson(mobilePerson);
		}
		
		// and now we do a lot of things just to have some working pt in it. This is to make sure transit drivers do not throw Creation events
		NetworkFactory networkFactory = scen.getNetwork().getFactory();
		Node node1 = networkFactory.createNode(Id.createNodeId("node1"), new Coord(200, -80));
		scen.getNetwork().addNode(node1);
		Node node2 = networkFactory.createNode(Id.createNodeId("node2"), new Coord(200, 200));
		scen.getNetwork().addNode(node2);
		Link link12 = networkFactory.createLink(Id.createLinkId("link12"), node1, node2);
		link12.setAllowedModes(Set.of("pt"));
		scen.getNetwork().addLink(link12);
		Link link11 = networkFactory.createLink(Id.createLinkId("link11"), node1, node1);
		link11.setAllowedModes(Set.of("pt"));
		scen.getNetwork().addLink(link11);
		Link link22 = networkFactory.createLink(Id.createLinkId("link22"), node2, node2);
		link22.setAllowedModes(Set.of("pt"));
		scen.getNetwork().addLink(link22);
				
		TransitScheduleFactory transitFactory = scen.getTransitSchedule().getFactory();
		TransitStopFacility stop1 = transitFactory.createTransitStopFacility(Id.create("stop1", TransitStopFacility.class), new Coord(200, -80), false);
		stop1.setLinkId(Id.createLinkId("link11"));
		scen.getTransitSchedule().addStopFacility(stop1);
		TransitStopFacility stop2 = transitFactory.createTransitStopFacility(Id.create("stop2", TransitStopFacility.class), new Coord(200, 200), false);
		stop2.setLinkId(Id.createLinkId("link22"));
		scen.getTransitSchedule().addStopFacility(stop2);
		TransitLine line = transitFactory.createTransitLine(Id.create("line", TransitLine.class));
		TransitRouteStop rstop1 = transitFactory.createTransitRouteStop(stop1, 0, 0);
		TransitRouteStop rstop2 = transitFactory.createTransitRouteStop(stop1, 70, 70);
		NetworkRoute nroute = RouteUtils.createNetworkRoute(List.of(Id.createLinkId("link11"), Id.createLinkId("link12"), Id.createLinkId("link22")));
		TransitRoute troute = transitFactory.createTransitRoute(Id.create("troute", TransitRoute.class), nroute, List.of(rstop1, rstop2), "pt");
		Departure departure = transitFactory.createDeparture(Id.create("dep", Departure.class), 0);
		VehicleType tvtype = scen.getTransitVehicles().getFactory().createVehicleType(Id.create("tvtype", VehicleType.class));
		tvtype.getCapacity().setSeats(10);
		tvtype.setNetworkMode("pt");
		scen.getTransitVehicles().addVehicleType(tvtype);
		Vehicle tvehicle = scen.getTransitVehicles().getFactory().createVehicle(Id.createVehicleId("tvehicle"), tvtype);
		scen.getTransitVehicles().addVehicle(tvehicle);
		departure.setVehicleId(tvehicle.getId());
		troute.addDeparture(departure);
		line.addRoute(troute);
		scen.getTransitSchedule().addTransitLine(line);
		
		return scen;
	}

}
