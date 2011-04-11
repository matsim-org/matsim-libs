package org.matsim.pt.qsim;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.Event;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.PersonEntersVehicleEvent;
import org.matsim.core.events.PersonLeavesVehicleEvent;
import org.matsim.core.events.TransitDriverStartsEvent;
import org.matsim.core.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.events.VehicleDepartsAtFacilityEvent;
import org.matsim.core.network.NetworkFactoryImpl;
import org.matsim.core.population.PopulationReaderMatsimV4;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.pt.routes.ExperimentalTransitRouteFactory;
import org.matsim.pt.transitSchedule.TransitScheduleReaderV1;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.testcases.utils.SelectiveEventsCollector;
import org.matsim.vehicles.VehicleReaderV1;
import org.xml.sax.SAXException;

/**
 * @author mrieser
 */
public class QSimIntegrationTest {

	@Test
	public void test_twoStopsOnFirstLink() throws SAXException, ParserConfigurationException, IOException {
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
		new TransitScheduleReaderV1(f.scenario.getTransitSchedule(), f.scenario.getNetwork(), f.scenario).parse(new ByteArrayInputStream(scheduleXml.getBytes()));

		EventsManager eventsManager = (EventsManager) EventsUtils.createEventsManager();
		SelectiveEventsCollector coll = new SelectiveEventsCollector(TransitDriverStartsEvent.class,
				AgentDepartureEvent.class, VehicleArrivesAtFacilityEvent.class, VehicleDepartsAtFacilityEvent.class, AgentArrivalEvent.class,
				LinkEnterEvent.class);
		eventsManager.addHandler(coll);

		QSim sim = new QSim(f.scenario, eventsManager);
		sim.run();

		coll.printEvents();

		List<Event> events = coll.getEvents();
		Assert.assertEquals("wrong number of events", 10, events.size());
		Assert.assertTrue(events.get(0) instanceof TransitDriverStartsEvent);
		Assert.assertTrue(events.get(1) instanceof AgentDepartureEvent);
		Assert.assertTrue(events.get(2) instanceof VehicleArrivesAtFacilityEvent); // stop 1
		Assert.assertTrue(events.get(3) instanceof VehicleDepartsAtFacilityEvent); // stop 1
		Assert.assertTrue(events.get(4) instanceof VehicleArrivesAtFacilityEvent); // stop 2
		Assert.assertTrue(events.get(5) instanceof VehicleDepartsAtFacilityEvent); // stop 2
		Assert.assertTrue(events.get(6) instanceof LinkEnterEvent);
		Assert.assertTrue(events.get(7) instanceof VehicleArrivesAtFacilityEvent); // stop 3
		Assert.assertTrue(events.get(8) instanceof VehicleDepartsAtFacilityEvent); // stop 3
		Assert.assertTrue(events.get(9) instanceof AgentArrivalEvent);
	}

	@Test
	public void test_multipleStopsOnFirstLink_singleLinkRoute_noPassengers() throws SAXException, ParserConfigurationException, IOException {
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
		new TransitScheduleReaderV1(f.scenario.getTransitSchedule(), f.scenario.getNetwork(), f.scenario).parse(new ByteArrayInputStream(scheduleXml.getBytes()));

		EventsManager eventsManager = (EventsManager) EventsUtils.createEventsManager();
		SelectiveEventsCollector coll = new SelectiveEventsCollector(TransitDriverStartsEvent.class,
				AgentDepartureEvent.class, VehicleArrivesAtFacilityEvent.class, VehicleDepartsAtFacilityEvent.class, AgentArrivalEvent.class,
				LinkEnterEvent.class);
		eventsManager.addHandler(coll);

		QSim sim = new QSim(f.scenario, eventsManager);
		sim.run();

		coll.printEvents();

		List<Event> events = coll.getEvents();
		Assert.assertEquals("wrong number of events", 11, events.size());
		Assert.assertTrue(events.get(0) instanceof TransitDriverStartsEvent);
		Assert.assertTrue(events.get(1) instanceof AgentDepartureEvent);
		Assert.assertTrue(events.get(2) instanceof VehicleArrivesAtFacilityEvent); // stop 1
		Assert.assertTrue(events.get(3) instanceof VehicleDepartsAtFacilityEvent); // stop 1
		Assert.assertTrue(events.get(4) instanceof VehicleArrivesAtFacilityEvent); // stop 2
		Assert.assertTrue(events.get(5) instanceof VehicleDepartsAtFacilityEvent); // stop 2
		Assert.assertTrue(events.get(6) instanceof VehicleArrivesAtFacilityEvent); // stop 3
		Assert.assertTrue(events.get(7) instanceof VehicleDepartsAtFacilityEvent); // stop 3
		Assert.assertTrue(events.get(8) instanceof VehicleArrivesAtFacilityEvent); // stop 4
		Assert.assertTrue(events.get(9) instanceof VehicleDepartsAtFacilityEvent); // stop 4
		Assert.assertTrue(events.get(10) instanceof AgentArrivalEvent);
	}

	@Test
	public void test_multipleStopsOnFirstLink_singleLinkRoute_withPassengersAtFirstStop() throws SAXException, ParserConfigurationException, IOException {
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
		new TransitScheduleReaderV1(f.scenario.getTransitSchedule(), f.scenario.getNetwork(), f.scenario).parse(new ByteArrayInputStream(scheduleXml.getBytes()));

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
		new PopulationReaderMatsimV4(f.scenario).parse(new ByteArrayInputStream(plansXml.getBytes()));

		EventsManager eventsManager = (EventsManager) EventsUtils.createEventsManager();
		SelectiveEventsCollector coll = new SelectiveEventsCollector(TransitDriverStartsEvent.class,
				AgentDepartureEvent.class, VehicleArrivesAtFacilityEvent.class, VehicleDepartsAtFacilityEvent.class, AgentArrivalEvent.class,
				LinkEnterEvent.class, PersonEntersVehicleEvent.class, PersonLeavesVehicleEvent.class);
		eventsManager.addHandler(coll);

		QSim sim = new QSim(f.scenario, eventsManager);
		sim.run();

		coll.printEvents();

		List<Event> events = coll.getEvents();
		Assert.assertEquals("wrong number of events", 15, events.size());
		int idx = 0;
		Assert.assertTrue(events.get(idx++) instanceof AgentDepartureEvent); // passenger
		Assert.assertTrue(events.get(idx++) instanceof TransitDriverStartsEvent);
		Assert.assertTrue(events.get(idx++) instanceof AgentDepartureEvent); // pt-driver
		Assert.assertTrue(events.get(idx++) instanceof VehicleArrivesAtFacilityEvent); // stop 1
		Assert.assertTrue(events.get(idx++) instanceof PersonEntersVehicleEvent);
		Assert.assertTrue(events.get(idx++) instanceof VehicleDepartsAtFacilityEvent); // stop 1
		Assert.assertTrue(events.get(idx++) instanceof VehicleArrivesAtFacilityEvent); // stop 2
		Assert.assertTrue(events.get(idx++) instanceof VehicleDepartsAtFacilityEvent); // stop 2
		Assert.assertTrue(events.get(idx++) instanceof VehicleArrivesAtFacilityEvent); // stop 3
		Assert.assertTrue(events.get(idx++) instanceof PersonLeavesVehicleEvent);
		Assert.assertTrue(events.get(idx++) instanceof AgentArrivalEvent); // passenger
		Assert.assertTrue(events.get(idx++) instanceof VehicleDepartsAtFacilityEvent); // stop 3
		Assert.assertTrue(events.get(idx++) instanceof VehicleArrivesAtFacilityEvent); // stop 4
		Assert.assertTrue(events.get(idx++) instanceof VehicleDepartsAtFacilityEvent); // stop 4
		Assert.assertTrue(events.get(idx++) instanceof AgentArrivalEvent);
	}

	@Test
	public void test_multipleStopsOnFirstLink_singleLinkRoute_withPassengersAtSecondStop() throws SAXException, ParserConfigurationException, IOException {
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
		new TransitScheduleReaderV1(f.scenario.getTransitSchedule(), f.scenario.getNetwork(), f.scenario).parse(new ByteArrayInputStream(scheduleXml.getBytes()));

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
		new PopulationReaderMatsimV4(f.scenario).parse(new ByteArrayInputStream(plansXml.getBytes()));

		EventsManager eventsManager = (EventsManager) EventsUtils.createEventsManager();
		SelectiveEventsCollector coll = new SelectiveEventsCollector(TransitDriverStartsEvent.class,
				AgentDepartureEvent.class, VehicleArrivesAtFacilityEvent.class, VehicleDepartsAtFacilityEvent.class, AgentArrivalEvent.class,
				LinkEnterEvent.class, PersonEntersVehicleEvent.class, PersonLeavesVehicleEvent.class);
		eventsManager.addHandler(coll);

		QSim sim = new QSim(f.scenario, eventsManager);
		sim.run();

		coll.printEvents();

		List<Event> events = coll.getEvents();
		Assert.assertEquals("wrong number of events", 15, events.size());
		int idx = 0;
		Assert.assertTrue(events.get(idx++) instanceof AgentDepartureEvent); // passenger
		Assert.assertTrue(events.get(idx++) instanceof TransitDriverStartsEvent);
		Assert.assertTrue(events.get(idx++) instanceof AgentDepartureEvent); // pt-driver
		Assert.assertTrue(events.get(idx++) instanceof VehicleArrivesAtFacilityEvent); // stop 1
		Assert.assertTrue(events.get(idx++) instanceof VehicleDepartsAtFacilityEvent); // stop 1
		Assert.assertTrue(events.get(idx++) instanceof VehicleArrivesAtFacilityEvent); // stop 2
		Assert.assertTrue(events.get(idx++) instanceof PersonEntersVehicleEvent);
		Assert.assertTrue(events.get(idx++) instanceof VehicleDepartsAtFacilityEvent); // stop 2
		Assert.assertTrue(events.get(idx++) instanceof VehicleArrivesAtFacilityEvent); // stop 3
		Assert.assertTrue(events.get(idx++) instanceof VehicleDepartsAtFacilityEvent); // stop 3
		Assert.assertTrue(events.get(idx++) instanceof VehicleArrivesAtFacilityEvent); // stop 4
		Assert.assertTrue(events.get(idx++) instanceof PersonLeavesVehicleEvent);
		Assert.assertTrue(events.get(idx++) instanceof AgentArrivalEvent); // passenger
		Assert.assertTrue(events.get(idx++) instanceof VehicleDepartsAtFacilityEvent); // stop 4
		Assert.assertTrue(events.get(idx++) instanceof AgentArrivalEvent);
	}

	private static class Fixture {
		public final ScenarioImpl scenario;
		public Fixture() throws SAXException, ParserConfigurationException, IOException {
			// setup: config
			this.scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
			this.scenario.getConfig().scenario().setUseTransit(true);
			this.scenario.getConfig().scenario().setUseVehicles(true);
			this.scenario.getConfig().addQSimConfigGroup(new QSimConfigGroup());
			this.scenario.getConfig().getQSimConfigGroup().setEndTime(8.0*3600);

			// setup: network
			Network network = this.scenario.getNetwork();
			Node node1 = network.getFactory().createNode(this.scenario.createId("1"), this.scenario.createCoord(   0, 0));
			Node node2 = network.getFactory().createNode(this.scenario.createId("2"), this.scenario.createCoord(1000, 0));
			Node node3 = network.getFactory().createNode(this.scenario.createId("3"), this.scenario.createCoord(2000, 0));
			Node node4 = network.getFactory().createNode(this.scenario.createId("4"), this.scenario.createCoord(3000, 0));
			Node node5 = network.getFactory().createNode(this.scenario.createId("5"), this.scenario.createCoord(4000, 0));
			Node node6 = network.getFactory().createNode(this.scenario.createId("6"), this.scenario.createCoord(5000, 0));
			network.addNode(node1);
			network.addNode(node2);
			network.addNode(node3);
			network.addNode(node4);
			network.addNode(node5);
			network.addNode(node6);
			Link link1 = network.getFactory().createLink(this.scenario.createId("1"), node1, node2);
			Link link2 = network.getFactory().createLink(this.scenario.createId("2"), node2, node3);
			Link link3 = network.getFactory().createLink(this.scenario.createId("3"), node3, node4);
			Link link4 = network.getFactory().createLink(this.scenario.createId("4"), node4, node5);
			Link link5 = network.getFactory().createLink(this.scenario.createId("5"), node5, node6);
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

			((NetworkFactoryImpl) network.getFactory()).setRouteFactory(TransportMode.pt, new ExperimentalTransitRouteFactory());

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
			new VehicleReaderV1(this.scenario.getVehicles()).parse(new ByteArrayInputStream(vehiclesXml.getBytes()));
		}

		private void setDefaultLinkAttributes(final Link link) {
			link.setLength(1000.0);
			link.setFreespeed(10.0);
			link.setCapacity(3600.0);
			link.setNumberOfLanes(1);
		}
	}
}
