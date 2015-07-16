/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package org.matsim.pt.counts;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimUtils;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.routes.ExperimentalTransitRouteFactory;
import org.matsim.pt.transitSchedule.TransitScheduleReaderV1;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.VehicleReaderV1;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Set;

/**
 * @author mrieser / senozon
 */
public class OccupancyAnalyzerTest {

	@Test
	public void test_SinglePersonOccupancy() throws SAXException, ParserConfigurationException, IOException {
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
		"<person id=\"2\">" +
		"	<plan>" +
		"		<act type=\"h\" x=\"1000\" y=\"1000\" link=\"2\" end_time=\"05:45\" />" +
		"		<leg mode=\"car\">" +
		"		</leg>" +
		"		<act type=\"w\" x=\"10000\" y=\"0\" link=\"3\" dur=\"00:10\" />" +
		"	</plan>" +
		"</person>" +
		"</plans>";
		new MatsimPopulationReader(f.scenario).parse(new ByteArrayInputStream(plansXml.getBytes()));

		EventsManager eventsManager = EventsUtils.createEventsManager();
		OccupancyAnalyzer oa = new OccupancyAnalyzer(3600, 12*3600);
		eventsManager.addHandler(oa);

		QSim sim = (QSim) QSimUtils.createDefaultQSim(f.scenario, eventsManager);
		sim.run();

		Set<Id<TransitStopFacility>> enterStops = oa.getBoardStopIds();
		Assert.assertEquals(1, enterStops.size());
		Assert.assertTrue(enterStops.contains(Id.create(2, TransitStopFacility.class)));

		Set<Id<TransitStopFacility>> exitStops = oa.getAlightStopIds();
		Assert.assertEquals(1, exitStops.size());
		Assert.assertTrue(exitStops.contains(Id.create(4, TransitStopFacility.class)));
		
		int[] occupancy = oa.getOccupancyVolumesForStop(Id.create(1, TransitStopFacility.class));
		Assert.assertEquals(0, occupancy[6]);
		occupancy = oa.getOccupancyVolumesForStop(Id.create(2, TransitStopFacility.class));
		Assert.assertEquals(1, occupancy[6]);
		occupancy = oa.getOccupancyVolumesForStop(Id.create(3, TransitStopFacility.class));
		Assert.assertEquals(1, occupancy[6]);
		occupancy = oa.getOccupancyVolumesForStop(Id.create(4, TransitStopFacility.class));
		Assert.assertEquals(0, occupancy[6]);
		
		Assert.assertEquals(0, oa.getBoardVolumesForStop(Id.create(1, TransitStopFacility.class))[6]);
		Assert.assertEquals(1, oa.getBoardVolumesForStop(Id.create(2, TransitStopFacility.class))[6]);
		Assert.assertEquals(0, oa.getBoardVolumesForStop(Id.create(3, TransitStopFacility.class))[6]);
		Assert.assertEquals(0, oa.getBoardVolumesForStop(Id.create(4, TransitStopFacility.class))[6]);

		Assert.assertEquals(0, oa.getAlightVolumesForStop(Id.create(1, TransitStopFacility.class))[6]);
		Assert.assertEquals(0, oa.getAlightVolumesForStop(Id.create(2, TransitStopFacility.class))[6]);
		Assert.assertEquals(0, oa.getAlightVolumesForStop(Id.create(3, TransitStopFacility.class))[6]);
		Assert.assertEquals(1, oa.getAlightVolumesForStop(Id.create(4, TransitStopFacility.class))[6]);
	}
	
	private static class Fixture {
		public final ScenarioImpl scenario;
		public Fixture() throws SAXException, ParserConfigurationException, IOException {
			// setup: config
			final Config config = ConfigUtils.createConfig();
			config.transit().setUseTransit(true);
			config.qsim().setEndTime(8.0*3600);

			this.scenario = (ScenarioImpl) ScenarioUtils.createScenario(config);

			// setup: network
			Network network = this.scenario.getNetwork();
			Node node1 = network.getFactory().createNode(Id.create("1", Node.class), this.scenario.createCoord(   0, 0));
			Node node2 = network.getFactory().createNode(Id.create("2", Node.class), this.scenario.createCoord(1000, 0));
			Node node3 = network.getFactory().createNode(Id.create("3", Node.class), this.scenario.createCoord(2000, 0));
			Node node4 = network.getFactory().createNode(Id.create("4", Node.class), this.scenario.createCoord(3000, 0));
			Node node5 = network.getFactory().createNode(Id.create("5", Node.class), this.scenario.createCoord(4000, 0));
			Node node6 = network.getFactory().createNode(Id.create("6", Node.class), this.scenario.createCoord(5000, 0));
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

			((PopulationFactoryImpl) this.scenario.getPopulation().getFactory()).setRouteFactory(TransportMode.pt, new ExperimentalTransitRouteFactory());

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
			new VehicleReaderV1(this.scenario.getTransitVehicles()).parse(new ByteArrayInputStream(vehiclesXml.getBytes()));
		}

		private void setDefaultLinkAttributes(final Link link) {
			link.setLength(1000.0);
			link.setFreespeed(10.0);
			link.setCapacity(3600.0);
			link.setNumberOfLanes(1);
		}
	}
}
