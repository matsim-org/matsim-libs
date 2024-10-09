/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package org.matsim.core.mobsim.qsim.pt;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.api.experimental.events.VehicleDepartsAtFacilityEvent;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.RoutingConfigGroup;
import org.matsim.core.controler.PrepareForSimUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.mobsim.qsim.QSimBuilder;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.TransitScheduleReaderV1;
import org.matsim.testcases.utils.SelectiveEventsCollector;
import org.matsim.vehicles.MatsimVehicleReader;
import org.xml.sax.SAXException;

/**
 * @author mrieser
 */
public class QSimIntegrationTest {

	@Test
	void test_twoStopsOnFirstLink() throws SAXException, ParserConfigurationException, IOException {
		Fixture f = new Fixture();
		String scheduleXml = "" +
				"<?xml version='1.0' encoding='UTF-8'?>" +
				"<!DOCTYPE transitSchedule SYSTEM \"http://www.matsim.org/files/dtd/transitSchedule_v1.dtd\">" +
				"<transitSchedule>" +
				"	<transitStops>" +
				"		<stopFacility id=\"1\" x=\"1050\" y=\"1050\" linkRefId=\"2\"/>" +
				"		<stopFacility id=\"2\" x=\"2050\" y=\"2940\" linkRefId=\"2\"/>" +
				"		<stopFacility id=\"3\" x=\"2050\" y=\"2940\" linkRefId=\"3\"/>" +
				"	</transitStops>" +
				"	<transitLine id=\"A\">" +
				"		<transitRoute id=\"Aa\">" +
				"			<transportMode>train</transportMode>" +
				"			<routeProfile>" +
				"				<stop refId=\"1\" departureOffset=\"00:00:00\"/>" +
				"				<stop refId=\"2\" arrivalOffset=\"00:03:00\"/>" +
				"				<stop refId=\"3\" arrivalOffset=\"00:06:00\"/>" +
				"			</routeProfile>" +
				"			<route>" +
				"				<link refId=\"2\"/>" +
				"				<link refId=\"3\"/>" +
				"			</route>" +
				"			<departures>" +
				"				<departure id=\"0x\" departureTime=\"06:00:00\" vehicleRefId=\"tr_1\" />" +
				"			</departures>" +
				"		</transitRoute>" +
				"	</transitLine>" +
				"</transitSchedule>";
		new TransitScheduleReaderV1(f.scenario).parse(new ByteArrayInputStream(scheduleXml.getBytes()));

		EventsManager eventsManager = EventsUtils.createEventsManager();
		SelectiveEventsCollector coll = new SelectiveEventsCollector(TransitDriverStartsEvent.class,
				PersonDepartureEvent.class, VehicleArrivesAtFacilityEvent.class, VehicleDepartsAtFacilityEvent.class, PersonArrivalEvent.class,
				LinkEnterEvent.class);
		eventsManager.addHandler(coll);

		PrepareForSimUtils.createDefaultPrepareForSim(f.scenario).run();
		new QSimBuilder(f.scenario.getConfig()) //
			.useDefaults() //
			.build(f.scenario, eventsManager) //
			.run();

		coll.printEvents();

		List<Event> events = coll.getEvents();
		Assertions.assertEquals(10, events.size(), "wrong number of events");
		Assertions.assertTrue(events.get(0) instanceof TransitDriverStartsEvent);
		Assertions.assertTrue(events.get(1) instanceof PersonDepartureEvent);
		Assertions.assertTrue(events.get(2) instanceof VehicleArrivesAtFacilityEvent); // stop 1
		Assertions.assertTrue(events.get(3) instanceof VehicleDepartsAtFacilityEvent); // stop 1
		Assertions.assertTrue(events.get(4) instanceof VehicleArrivesAtFacilityEvent); // stop 2
		Assertions.assertTrue(events.get(5) instanceof VehicleDepartsAtFacilityEvent); // stop 2
		Assertions.assertTrue(events.get(6) instanceof LinkEnterEvent);
		Assertions.assertTrue(events.get(7) instanceof VehicleArrivesAtFacilityEvent); // stop 3
		Assertions.assertTrue(events.get(8) instanceof VehicleDepartsAtFacilityEvent); // stop 3
		Assertions.assertTrue(events.get(9) instanceof PersonArrivalEvent);
	}

	@Test
	void test_multipleStopsOnFirstLink_singleLinkRoute_noPassengers() throws SAXException, ParserConfigurationException, IOException {
		Fixture f = new Fixture();
		String scheduleXml = "" +
		"<?xml version='1.0' encoding='UTF-8'?>" +
		"<!DOCTYPE transitSchedule SYSTEM \"http://www.matsim.org/files/dtd/transitSchedule_v1.dtd\">" +
		"<transitSchedule>" +
		"	<transitStops>" +
		"		<stopFacility id=\"1\" x=\"1050\" y=\"1050\" linkRefId=\"2\"/>" +
		"		<stopFacility id=\"2\" x=\"2050\" y=\"2940\" linkRefId=\"2\"/>" +
		"		<stopFacility id=\"3\" x=\"2050\" y=\"2940\" linkRefId=\"2\"/>" +
		"		<stopFacility id=\"4\" x=\"2050\" y=\"2940\" linkRefId=\"2\"/>" +
		"	</transitStops>" +
		"	<transitLine id=\"A\">" +
		"		<transitRoute id=\"Aa\">" +
		"			<transportMode>train</transportMode>" +
		"			<routeProfile>" +
		"				<stop refId=\"1\" departureOffset=\"00:00:00\"/>" +
		"				<stop refId=\"2\" arrivalOffset=\"00:03:00\"/>" +
		"				<stop refId=\"3\" arrivalOffset=\"00:04:00\"/>" +
		"				<stop refId=\"4\" arrivalOffset=\"00:05:00\"/>" +
		"			</routeProfile>" +
		"			<route>" +
		"				<link refId=\"2\"/>" +
		"				<link refId=\"2\"/>" +
		"			</route>" +
		"			<departures>" +
		"				<departure id=\"0x\" departureTime=\"06:00:00\" vehicleRefId=\"tr_1\" />" +
		"			</departures>" +
		"		</transitRoute>" +
		"	</transitLine>" +
		"</transitSchedule>";
		new TransitScheduleReaderV1(f.scenario).parse(new ByteArrayInputStream(scheduleXml.getBytes()));

		EventsManager eventsManager = EventsUtils.createEventsManager();
		SelectiveEventsCollector coll = new SelectiveEventsCollector(TransitDriverStartsEvent.class,
				PersonDepartureEvent.class, VehicleArrivesAtFacilityEvent.class, VehicleDepartsAtFacilityEvent.class, PersonArrivalEvent.class,
				LinkEnterEvent.class);
		eventsManager.addHandler(coll);

		PrepareForSimUtils.createDefaultPrepareForSim(f.scenario).run();
		new QSimBuilder(f.scenario.getConfig()) //
			.useDefaults() //
			.build(f.scenario, eventsManager) //
			.run();

		coll.printEvents();

		List<Event> events = coll.getEvents();
		Assertions.assertEquals(11, events.size(), "wrong number of events");
		Assertions.assertTrue(events.get(0) instanceof TransitDriverStartsEvent);
		Assertions.assertTrue(events.get(1) instanceof PersonDepartureEvent);
		Assertions.assertTrue(events.get(2) instanceof VehicleArrivesAtFacilityEvent); // stop 1
		Assertions.assertTrue(events.get(3) instanceof VehicleDepartsAtFacilityEvent); // stop 1
		Assertions.assertTrue(events.get(4) instanceof VehicleArrivesAtFacilityEvent); // stop 2
		Assertions.assertTrue(events.get(5) instanceof VehicleDepartsAtFacilityEvent); // stop 2
		Assertions.assertTrue(events.get(6) instanceof VehicleArrivesAtFacilityEvent); // stop 3
		Assertions.assertTrue(events.get(7) instanceof VehicleDepartsAtFacilityEvent); // stop 3
		Assertions.assertTrue(events.get(8) instanceof VehicleArrivesAtFacilityEvent); // stop 4
		Assertions.assertTrue(events.get(9) instanceof VehicleDepartsAtFacilityEvent); // stop 4
		Assertions.assertTrue(events.get(10) instanceof PersonArrivalEvent);
	}

	@Test
	void test_multipleStopsOnFirstLink_singleLinkRoute_withPassengersAtFirstStop() throws SAXException, ParserConfigurationException, IOException {
		Fixture f = new Fixture();
		String scheduleXml = "" +
				"<?xml version='1.0' encoding='UTF-8'?>" +
				"<!DOCTYPE transitSchedule SYSTEM \"http://www.matsim.org/files/dtd/transitSchedule_v1.dtd\">" +
				"<transitSchedule>" +
				"	<transitStops>" +
				"		<stopFacility id=\"1\" x=\"1050\" y=\"1050\" linkRefId=\"2\"/>" +
				"		<stopFacility id=\"2\" x=\"2050\" y=\"2940\" linkRefId=\"2\"/>" +
				"		<stopFacility id=\"3\" x=\"2050\" y=\"2940\" linkRefId=\"2\"/>" +
				"		<stopFacility id=\"4\" x=\"2050\" y=\"2940\" linkRefId=\"2\"/>" +
				"	</transitStops>" +
				"	<transitLine id=\"A\">" +
				"		<transitRoute id=\"Aa\">" +
				"			<transportMode>train</transportMode>" +
				"			<routeProfile>" +
				"				<stop refId=\"1\" departureOffset=\"00:00:00\"/>" +
				"				<stop refId=\"2\" arrivalOffset=\"00:03:00\"/>" +
				"				<stop refId=\"3\" arrivalOffset=\"00:04:00\"/>" +
				"				<stop refId=\"4\" arrivalOffset=\"00:05:00\"/>" +
				"			</routeProfile>" +
				"			<route>" +
				"				<link refId=\"2\"/>" +
				"				<link refId=\"2\"/>" +
				"			</route>" +
				"			<departures>" +
				"				<departure id=\"0x\" departureTime=\"06:00:00\" vehicleRefId=\"tr_1\" />" +
				"			</departures>" +
				"		</transitRoute>" +
				"	</transitLine>" +
				"</transitSchedule>";
		new TransitScheduleReaderV1(f.scenario).parse(new ByteArrayInputStream(scheduleXml.getBytes()));

		String plansXml = "<?xml version=\"1.0\" ?>" +
				"<!DOCTYPE plans SYSTEM \"http://www.matsim.org/files/dtd/plans_v4.dtd\">" +
				"<plans>" +
				"<person id=\"1\">" +
				"	<plan>" +
				"		<act type=\"h\" x=\"1000\" y=\"1000\" link=\"2\" end_time=\"05:45\" />" +
				"		<leg mode=\"pt\">" +
				"			<route>PT1===1===A===Aa===3</route>" +
				"		</leg>" +
				"		<act type=\"w\" x=\"10000\" y=\"0\" link=\"3\" dur=\"00:10\" />" +
				"	</plan>" +
				"</person>" +
				"</plans>";
		new PopulationReader(f.scenario).parse(new ByteArrayInputStream(plansXml.getBytes()));

		EventsManager eventsManager = EventsUtils.createEventsManager();
		SelectiveEventsCollector coll = new SelectiveEventsCollector(TransitDriverStartsEvent.class,
				PersonDepartureEvent.class, VehicleArrivesAtFacilityEvent.class, VehicleDepartsAtFacilityEvent.class, PersonArrivalEvent.class,
				LinkEnterEvent.class, PersonEntersVehicleEvent.class, PersonLeavesVehicleEvent.class);
		eventsManager.addHandler(coll);

		PrepareForSimUtils.createDefaultPrepareForSim(f.scenario).run();
		new QSimBuilder(f.scenario.getConfig()) //
			.useDefaults() //
			.build(f.scenario, eventsManager) //
			.run();

		coll.printEvents();

		List<Event> events = coll.getEvents();
		Assertions.assertEquals(17, events.size(), "wrong number of events");
		int idx = 0;
		Assertions.assertTrue(events.get(idx++) instanceof PersonDepartureEvent); // passenger
		Assertions.assertTrue(events.get(idx++) instanceof TransitDriverStartsEvent);
		Assertions.assertTrue(events.get(idx++) instanceof PersonDepartureEvent); // pt-driver
		Assertions.assertTrue(events.get(idx++) instanceof PersonEntersVehicleEvent); // pt-driver
		Assertions.assertTrue(events.get(idx++) instanceof VehicleArrivesAtFacilityEvent); // stop 1
		Assertions.assertTrue(events.get(idx++) instanceof PersonEntersVehicleEvent);
		Assertions.assertTrue(events.get(idx++) instanceof VehicleDepartsAtFacilityEvent); // stop 1
		Assertions.assertTrue(events.get(idx++) instanceof VehicleArrivesAtFacilityEvent); // stop 2
		Assertions.assertTrue(events.get(idx++) instanceof VehicleDepartsAtFacilityEvent); // stop 2
		Assertions.assertTrue(events.get(idx++) instanceof VehicleArrivesAtFacilityEvent); // stop 3
		Assertions.assertTrue(events.get(idx++) instanceof PersonLeavesVehicleEvent);
		Assertions.assertTrue(events.get(idx++) instanceof PersonArrivalEvent); // passenger
		Assertions.assertTrue(events.get(idx++) instanceof VehicleDepartsAtFacilityEvent); // stop 3
		Assertions.assertTrue(events.get(idx++) instanceof VehicleArrivesAtFacilityEvent); // stop 4
		Assertions.assertTrue(events.get(idx++) instanceof VehicleDepartsAtFacilityEvent); // stop 4
		Assertions.assertTrue(events.get(idx++) instanceof PersonLeavesVehicleEvent); // pt-driver
		Assertions.assertTrue(events.get(idx++) instanceof PersonArrivalEvent);
	}

	@Test
	void test_multipleStopsOnFirstLink_singleLinkRoute_withPassengersAtSecondStop() throws SAXException, ParserConfigurationException, IOException {
		Fixture f = new Fixture();
		String scheduleXml = "" +
		"<?xml version='1.0' encoding='UTF-8'?>" +
		"<!DOCTYPE transitSchedule SYSTEM \"http://www.matsim.org/files/dtd/transitSchedule_v1.dtd\">" +
		"<transitSchedule>" +
		"	<transitStops>" +
		"		<stopFacility id=\"1\" x=\"1050\" y=\"1050\" linkRefId=\"2\"/>" +
		"		<stopFacility id=\"2\" x=\"2050\" y=\"2940\" linkRefId=\"2\"/>" +
		"		<stopFacility id=\"3\" x=\"2050\" y=\"2940\" linkRefId=\"2\"/>" +
		"		<stopFacility id=\"4\" x=\"2050\" y=\"2940\" linkRefId=\"2\"/>" +
		"	</transitStops>" +
		"	<transitLine id=\"A\">" +
		"		<transitRoute id=\"Aa\">" +
		"			<transportMode>train</transportMode>" +
		"			<routeProfile>" +
		"				<stop refId=\"1\" departureOffset=\"00:00:00\"/>" +
		"				<stop refId=\"2\" arrivalOffset=\"00:03:00\"/>" +
		"				<stop refId=\"3\" arrivalOffset=\"00:04:00\"/>" +
		"				<stop refId=\"4\" arrivalOffset=\"00:05:00\"/>" +
		"			</routeProfile>" +
		"			<route>" +
		"				<link refId=\"2\"/>" +
		"				<link refId=\"2\"/>" +
		"			</route>" +
		"			<departures>" +
		"				<departure id=\"0x\" departureTime=\"06:00:00\" vehicleRefId=\"tr_1\" />" +
		"			</departures>" +
		"		</transitRoute>" +
		"	</transitLine>" +
		"</transitSchedule>";
		new TransitScheduleReaderV1(f.scenario).parse(new ByteArrayInputStream(scheduleXml.getBytes()));

		String plansXml = "<?xml version=\"1.0\" ?>" +
		"<!DOCTYPE plans SYSTEM \"http://www.matsim.org/files/dtd/plans_v4.dtd\">" +
		"<plans>" +
		"<person id=\"1\">" +
		"	<plan>" +
		"		<act type=\"h\" x=\"1000\" y=\"1000\" link=\"2\" end_time=\"05:45\" />" +
		"		<leg mode=\"pt\">" +
		"			<route>PT1===2===A===Aa===4</route>" +
		"		</leg>" +
		"		<act type=\"w\" x=\"10000\" y=\"0\" link=\"3\" dur=\"00:10\" />" +
		"	</plan>" +
		"</person>" +
		"</plans>";
		new PopulationReader(f.scenario).parse(new ByteArrayInputStream(plansXml.getBytes()));

		EventsManager eventsManager = EventsUtils.createEventsManager();
		SelectiveEventsCollector coll = new SelectiveEventsCollector(TransitDriverStartsEvent.class,
				PersonDepartureEvent.class, VehicleArrivesAtFacilityEvent.class, VehicleDepartsAtFacilityEvent.class, PersonArrivalEvent.class,
				LinkEnterEvent.class, PersonEntersVehicleEvent.class, PersonLeavesVehicleEvent.class);
		eventsManager.addHandler(coll);

		PrepareForSimUtils.createDefaultPrepareForSim(f.scenario).run();
		new QSimBuilder(f.scenario.getConfig()) //
			.useDefaults() //
			.build(f.scenario, eventsManager) //
			.run();

		coll.printEvents();

		List<Event> events = coll.getEvents();
		Assertions.assertEquals(17, events.size(), "wrong number of events");
		int idx = 0;
		Assertions.assertTrue(events.get(idx++) instanceof PersonDepartureEvent); // passenger
		Assertions.assertTrue(events.get(idx++) instanceof TransitDriverStartsEvent);
		Assertions.assertTrue(events.get(idx++) instanceof PersonDepartureEvent); // pt-driver
		Assertions.assertTrue(events.get(idx++) instanceof PersonEntersVehicleEvent); // pt-driver
		Assertions.assertTrue(events.get(idx++) instanceof VehicleArrivesAtFacilityEvent); // stop 1
		Assertions.assertTrue(events.get(idx++) instanceof VehicleDepartsAtFacilityEvent); // stop 1
		Assertions.assertTrue(events.get(idx++) instanceof VehicleArrivesAtFacilityEvent); // stop 2
		Assertions.assertTrue(events.get(idx++) instanceof PersonEntersVehicleEvent);
		Assertions.assertTrue(events.get(idx++) instanceof VehicleDepartsAtFacilityEvent); // stop 2
		Assertions.assertTrue(events.get(idx++) instanceof VehicleArrivesAtFacilityEvent); // stop 3
		Assertions.assertTrue(events.get(idx++) instanceof VehicleDepartsAtFacilityEvent); // stop 3
		Assertions.assertTrue(events.get(idx++) instanceof VehicleArrivesAtFacilityEvent); // stop 4
		Assertions.assertTrue(events.get(idx++) instanceof PersonLeavesVehicleEvent);
		Assertions.assertTrue(events.get(idx++) instanceof PersonArrivalEvent); // passenger
		Assertions.assertTrue(events.get(idx++) instanceof VehicleDepartsAtFacilityEvent); // stop 4
		Assertions.assertTrue(events.get(idx++) instanceof PersonLeavesVehicleEvent); // pt-driver
		Assertions.assertTrue(events.get(idx++) instanceof PersonArrivalEvent);
	}

	/**
	 * This checks for the right events in the case of a stupid-looking transit route which has only two stops which are even the same.
	 * But think about round-trip ship cruises, tourist buses etc and it makes more sense.
	 * To add a twist assume some non-useful (because automatically generated) network route with only a single link and no
	 * real round-trip route.
	 *
	 * And yes, this case has appeared in real data, that's why there is a test case for it... (mrieser, jan2014)
	 *
	 * @throws SAXException
	 * @throws ParserConfigurationException
	 * @throws IOException
	 */
	@Test
	void test_circularEmptyRoute_singleLinkRoute_noPassengers() throws SAXException, ParserConfigurationException, IOException {
		Fixture f = new Fixture();
		String scheduleXml = "" +
		"<?xml version='1.0' encoding='UTF-8'?>" +
		"<!DOCTYPE transitSchedule SYSTEM \"http://www.matsim.org/files/dtd/transitSchedule_v1.dtd\">" +
		"<transitSchedule>" +
		"	<transitStops>" +
		"		<stopFacility id=\"1\" x=\"1050\" y=\"1050\" linkRefId=\"2\"/>" +
		"	</transitStops>" +
		"	<transitLine id=\"A\">" +
		"		<transitRoute id=\"Aa\">" +
		"			<transportMode>train</transportMode>" +
		"			<routeProfile>" +
		"				<stop refId=\"1\" departureOffset=\"00:00:00\"/>" +
		"				<stop refId=\"1\" arrivalOffset=\"00:15:00\"/>" +
		"			</routeProfile>" +
		"			<route>" +
		"				<link refId=\"2\"/>" +
		"				<link refId=\"2\"/>" +
		"			</route>" +
		"			<departures>" +
		"				<departure id=\"0x\" departureTime=\"06:00:00\" vehicleRefId=\"tr_1\" />" +
		"			</departures>" +
		"		</transitRoute>" +
		"	</transitLine>" +
		"</transitSchedule>";
		new TransitScheduleReaderV1(f.scenario).parse(new ByteArrayInputStream(scheduleXml.getBytes()));

		EventsManager eventsManager = EventsUtils.createEventsManager();
		SelectiveEventsCollector coll = new SelectiveEventsCollector(TransitDriverStartsEvent.class,
				PersonDepartureEvent.class, VehicleArrivesAtFacilityEvent.class, VehicleDepartsAtFacilityEvent.class, PersonArrivalEvent.class,
				LinkEnterEvent.class);
		eventsManager.addHandler(coll);

		PrepareForSimUtils.createDefaultPrepareForSim(f.scenario).run();
		new QSimBuilder(f.scenario.getConfig()) //
			.useDefaults() //
			.build(f.scenario, eventsManager) //
			.run();

		coll.printEvents();

		List<Event> events = coll.getEvents();
		Assertions.assertEquals(7, events.size(), "wrong number of events");
		Assertions.assertTrue(events.get(0) instanceof TransitDriverStartsEvent);
		Assertions.assertTrue(events.get(1) instanceof PersonDepartureEvent);
		Assertions.assertTrue(events.get(2) instanceof VehicleArrivesAtFacilityEvent); // stop 1
		Assertions.assertTrue(events.get(3) instanceof VehicleDepartsAtFacilityEvent); // stop 1
		Assertions.assertTrue(events.get(4) instanceof VehicleArrivesAtFacilityEvent); // stop 1
		Assertions.assertTrue(events.get(5) instanceof VehicleDepartsAtFacilityEvent); // stop 1
		Assertions.assertTrue(events.get(6) instanceof PersonArrivalEvent);
	}


	private static class Fixture {
		public final MutableScenario scenario;
		public Fixture() throws SAXException, ParserConfigurationException, IOException {
			// setup: config
			final Config config = ConfigUtils.createConfig();
			config.transit().setUseTransit(true);
			config.routing().setNetworkRouteConsistencyCheck(RoutingConfigGroup.NetworkRouteConsistencyCheck.disable);
			config.qsim().setEndTime(8.0*3600);

			this.scenario = (MutableScenario) ScenarioUtils.createScenario(config);

			// setup: network
			Network network = this.scenario.getNetwork();
			Node node1 = network.getFactory().createNode(Id.create("1", Node.class), new Coord(0, 0));
			Node node2 = network.getFactory().createNode(Id.create("2", Node.class), new Coord(1000, 0));
			Node node3 = network.getFactory().createNode(Id.create("3", Node.class), new Coord(2000, 0));
			Node node4 = network.getFactory().createNode(Id.create("4", Node.class), new Coord(3000, 0));
			Node node5 = network.getFactory().createNode(Id.create("5", Node.class), new Coord(4000, 0));
			Node node6 = network.getFactory().createNode(Id.create("6", Node.class), new Coord(5000, 0));
			network.addNode(node1);
			network.addNode(node2);
			network.addNode(node3);
			network.addNode(node4);
			network.addNode(node5);
			network.addNode(node6);
			Link link1 = network.getFactory().createLink(Id.create("1", Link.class), node1, node2);
			Link link2 = network.getFactory().createLink(Id.create("2", Link.class), node2, node3);
			Link link3 = network.getFactory().createLink(Id.create("3", Link.class), node3, node4);
			Link link4 = network.getFactory().createLink(Id.create("4", Link.class), node4, node5);
			Link link5 = network.getFactory().createLink(Id.create("5", Link.class), node5, node6);
			setDefaultLinkAttributes(link1);
			setDefaultLinkAttributes(link2);
			setDefaultLinkAttributes(link3);
			setDefaultLinkAttributes(link4);
			setDefaultLinkAttributes(link5);
			network.addLink(link1);
			network.addLink(link2);
			network.addLink(link3);
			network.addLink(link4);
			network.addLink(link5);

			// setup: vehicles
			String vehiclesXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
			"<vehicleDefinitions xmlns=\"http://www.matsim.org/files/dtd\"" +
			" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
			" xsi:schemaLocation=\"http://www.matsim.org/files/dtd http://www.matsim.org/files/dtd/vehicleDefinitions_v1.0.xsd\">" +
			"	<vehicleType id=\"1\">" +
			"		<description>Small Train</description>" +
			"		<capacity>" +
			"			<seats persons=\"50\"/>" +
			"			<standingRoom persons=\"30\"/>" +
			"		</capacity>" +
			"		<length meter=\"50.0\"/>" +
			"	</vehicleType>" +
			"	<vehicle id=\"tr_1\" type=\"1\"/>" +
			"	<vehicle id=\"tr_2\" type=\"1\"/>" +
			"	<vehicle id=\"tr_3\" type=\"1\"/>" +
			"</vehicleDefinitions>";
			new MatsimVehicleReader(this.scenario.getTransitVehicles()).readStream(new ByteArrayInputStream(vehiclesXml.getBytes()) );
		}

		private void setDefaultLinkAttributes(final Link link) {
			link.setLength(1000.0);
			link.setFreespeed(10.0);
			link.setCapacity(3600.0);
			link.setNumberOfLanes(1);
		}
	}
}
