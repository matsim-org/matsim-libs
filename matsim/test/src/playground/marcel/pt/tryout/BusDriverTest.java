/* *********************************************************************** *
 * project: org.matsim.*
 * BusDriverTest.java
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

package playground.marcel.pt.tryout;

import java.io.IOException;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.basic.v01.IdImpl;
import org.matsim.events.ActStartEvent;
import org.matsim.events.Events;
import org.matsim.facilities.Facilities;
import org.matsim.facilities.MatsimFacilitiesReader;
import org.matsim.interfaces.basic.v01.BasicLeg;
import org.matsim.interfaces.basic.v01.Id;
import org.matsim.interfaces.core.v01.Act;
import org.matsim.interfaces.core.v01.Facility;
import org.matsim.interfaces.core.v01.Link;
import org.matsim.interfaces.core.v01.Plan;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.world.World;
import org.xml.sax.SAXException;

import playground.marcel.pt.implementations.VehicleImpl;
import playground.marcel.pt.interfaces.Vehicle;
import playground.marcel.pt.transitSchedule.Departure;
import playground.marcel.pt.transitSchedule.TransitLine;
import playground.marcel.pt.transitSchedule.TransitRoute;
import playground.marcel.pt.transitSchedule.TransitSchedule;
import playground.marcel.pt.transitSchedule.TransitScheduleReader;
import playground.marcel.pt.transitSchedule.TransitScheduleReaderTest;
import playground.marcel.pt.transitSchedule.modules.CreateTimetableForStop;
import playground.marcel.pt.utils.FacilityVisitors;

public class BusDriverTest extends MatsimTestCase {

	public static final String INPUT_TEST_FILE_TRANSITSCHEDULE = "transitSchedule.xml";
	public static final String INPUT_TEST_FILE_NETWORK = "network.xml";
	public static final String INPUT_TEST_FILE_FACILITIES = "facilities.xml";

	public void testPersonsLeavingBus() throws SAXException, ParserConfigurationException, IOException {
		final String inputDir = "test/input/" + TransitScheduleReaderTest.class.getPackage().getName().replace('.', '/') + "/";

		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(inputDir + INPUT_TEST_FILE_NETWORK);
		Facilities facilities = new Facilities();
		new MatsimFacilitiesReader(facilities).readFile(inputDir + INPUT_TEST_FILE_FACILITIES);

		World world = new World();
		world.setFacilityLayer(facilities);
		world.setNetworkLayer(network);
		world.complete();

		TransitSchedule schedule = new TransitSchedule();
		new TransitScheduleReader(schedule, network, facilities).readFile(inputDir + INPUT_TEST_FILE_TRANSITSCHEDULE);

		TransitLine lineT1 = schedule.getTransitLine(new IdImpl("T1"));
		CreateTimetableForStop timetable = new CreateTimetableForStop(lineT1);
		assertNotNull("could not get transit line.", lineT1);

		TransitRoute route1 = lineT1.getRoute(new IdImpl("1"));
		Map<Id, Departure> departures = route1.getDepartures();

		Events events = new Events();

		BusDriver driver = new BusDriver(route1, departures.values().iterator().next());
		Vehicle bus = new VehicleImpl(20, events);
		driver.setVehicle(bus);

		BusPassenger passenger1 = new BusPassenger(new IdImpl("1"), facilities.getFacility(new IdImpl("stop2")));
		BusPassenger passenger2 = new BusPassenger(new IdImpl("2"), facilities.getFacility(new IdImpl("stop3")));
		BusPassenger passenger3 = new BusPassenger(new IdImpl("3"), facilities.getFacility(new IdImpl("stop4")));
		BusPassenger passenger4 = new BusPassenger(new IdImpl("4"), facilities.getFacility(new IdImpl("stop5")));
		BusPassenger passenger5 = new BusPassenger(new IdImpl("5"), facilities.getFacility(new IdImpl("stop6")));
		bus.addPassenger(passenger1);
		bus.addPassenger(passenger2);
		bus.addPassenger(passenger3);
		bus.addPassenger(passenger4);
		bus.addPassenger(passenger5);
		assertEquals("wrong number of passengers.", 5, bus.getPassengers().size());

		Link link = driver.chooseNextLink();
		driver.enterNextLink();
		while (link != null) {
			Link nextLink = driver.chooseNextLink();
			if (nextLink != null) {
				assertEquals("current link and next link must have common node.", link.getToNode(), nextLink.getFromNode());
			}
			driver.leaveCurrentLink();
			link = nextLink;
			if (link != null) {
				driver.enterNextLink();
			}
		}

		assertEquals("wrong number of passengers.", 0, bus.getPassengers().size());
	}

	public void testPersonsEnteringBus() throws SAXException, ParserConfigurationException, IOException {
		final String inputDir = "test/input/" + TransitScheduleReaderTest.class.getPackage().getName().replace('.', '/') + "/";

		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(inputDir + INPUT_TEST_FILE_NETWORK);
		Facilities facilities = new Facilities();
		new MatsimFacilitiesReader(facilities).readFile(inputDir + INPUT_TEST_FILE_FACILITIES);

		World world = new World();
		world.setFacilityLayer(facilities);
		world.setNetworkLayer(network);
		world.complete();

		TransitSchedule schedule = new TransitSchedule();
		new TransitScheduleReader(schedule, network, facilities).readFile(inputDir + INPUT_TEST_FILE_TRANSITSCHEDULE);

		TransitLine lineT1 = schedule.getTransitLine(new IdImpl("T1"));
		CreateTimetableForStop timetable = new CreateTimetableForStop(lineT1);
		assertNotNull("could not get transit line.", lineT1);

		TransitRoute route1 = lineT1.getRoute(new IdImpl("1"));
		Map<Id, Departure> departures = route1.getDepartures();

		Events events = new Events();

		BusDriver driver = new BusDriver(route1, departures.values().iterator().next());
		Vehicle bus = new VehicleImpl(20, events);
		driver.setVehicle(bus);

		FacilityVisitors fv = new FacilityVisitors();
		events.addHandler(fv);
		driver.setFacilityVisitorObserver(fv);

		Facility workFacility = facilities.getFacility(new IdImpl("work"));

		BusPassenger passenger1 = createPassenger("1", facilities.getFacility(new IdImpl("stop2")), workFacility);
		BusPassenger passenger2 = createPassenger("2", facilities.getFacility(new IdImpl("stop3")), workFacility);
		BusPassenger passenger3 = createPassenger("3", facilities.getFacility(new IdImpl("stop4")), workFacility);
		BusPassenger passenger4 = createPassenger("4", facilities.getFacility(new IdImpl("stop5")), workFacility);
		BusPassenger passenger5 = createPassenger("5", facilities.getFacility(new IdImpl("stop6")), workFacility);
		events.processEvent(new ActStartEvent(6.0*3600, passenger1, network.getLink("3"), (Act) passenger1.getPlans().get(0).getActsLegs().get(0)));
		events.processEvent(new ActStartEvent(6.0*3600, passenger1, network.getLink("5"), (Act) passenger2.getPlans().get(0).getActsLegs().get(0)));
		events.processEvent(new ActStartEvent(6.0*3600, passenger1, network.getLink("5"), (Act) passenger3.getPlans().get(0).getActsLegs().get(0)));
		events.processEvent(new ActStartEvent(6.0*3600, passenger1, network.getLink("6"), (Act) passenger4.getPlans().get(0).getActsLegs().get(0)));
		events.processEvent(new ActStartEvent(6.0*3600, passenger1, network.getLink("8"), (Act) passenger5.getPlans().get(0).getActsLegs().get(0)));

		assertEquals("wrong number of passengers.", 0, bus.getPassengers().size());

		Link link = driver.chooseNextLink();
		driver.enterNextLink();
		while (link != null) {
			Link nextLink = driver.chooseNextLink();
			if (nextLink != null) {
				assertEquals("current link and next link must have common node.", link.getToNode(), nextLink.getFromNode());
			}
			driver.leaveCurrentLink();
			link = nextLink;
			if (link != null) {
				driver.enterNextLink();
			}
		}

		assertEquals("wrong number of passengers.", 5, bus.getPassengers().size());
	}

	private BusPassenger createPassenger(final String id, final Facility enterStop, final Facility exitStop) {
		BusPassenger passenger = new BusPassenger(new IdImpl("1"), exitStop);
		Plan plan = passenger.createPlan(true);
		plan.createAct("pt_interaction", enterStop);
		plan.createLeg(BasicLeg.Mode.bus);
		plan.createAct("work", exitStop);
		return passenger;
	}
}
