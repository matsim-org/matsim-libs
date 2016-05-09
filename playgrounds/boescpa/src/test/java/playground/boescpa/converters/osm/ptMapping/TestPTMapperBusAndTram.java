/*
 * *********************************************************************** *
 * project: org.matsim.*                                                   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package playground.boescpa.converters.osm.ptMapping;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.population.routes.RouteFactoryImpl;
import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.TransitScheduleReaderV1;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.testcases.MatsimTestUtils;
import playground.boescpa.lib.tools.NetworkUtils;

import static org.junit.Assert.assertEquals;

/**
 * Provides Tests for PTLineRouterDefault.
 *
 * @author boescpa
 */
public class TestPTMapperBusAndTram {

	Network network = null;
	TransitSchedule schedule = null;

	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Before
	public void prepareTests() {
		this.network = NetworkUtils.readNetwork(utils.getClassInputDirectory() + "ZurichCentre.xml");
		this.schedule = new TransitScheduleFactoryImpl().createTransitSchedule();
		TransitScheduleReaderV1 reader = new TransitScheduleReaderV1(this.schedule, new RouteFactoryImpl());
		reader.readFile(utils.getClassInputDirectory() + "ScheduleTest.xml");
	}

	@Test
	public void testLinkStationsToNetwork() {
		PTMapperBusAndTram router = new PTMapperBusAndTram(this.schedule, this.network);
		router.linkStationsToNetwork();
		assertEquals("Not all or too many links found.", 54, this.schedule.getFacilities().size());
	}

	@Test
	public void testCreatePtRoutes() {
		PTMapperBusAndTram router = new PTMapperBusAndTram(this.schedule, this.network);
		router.linkStationsToNetwork();
		router.createPTRoutes();
		new TransitScheduleWriter(schedule).writeFile(utils.getOutputDirectory() + "Schedule_RouterTest.xml");
	}

	@Test
	public void testCleanStationsAndNetwork() {
		PTMapperBusAndTram router = new PTMapperBusAndTram(this.schedule, this.network);
		router.linkStationsToNetwork();
		router.createPTRoutes();
		router.cleanStationsAndNetwork();
		new TransitScheduleWriter(this.schedule).writeFile(utils.getOutputDirectory() + "Schedule_CleanerTest.xml");
		new NetworkWriter(network).write(utils.getOutputDirectory() + "Network_CleanerTest.xml");
	}

	@Test
	public void testCreateAdditionalLinks() {
		TransitSchedule transitSchedule = new TransitScheduleFactoryImpl().createTransitSchedule();
		TransitScheduleReaderV1 reader = new TransitScheduleReaderV1(transitSchedule, new RouteFactoryImpl());
		reader.readFile(utils.getClassInputDirectory() + "ScheduleTest_AdditionalLinks.xml");
		PTMapperBusAndTram router = new PTMapperBusAndTram(transitSchedule, this.network);
		router.linkStationsToNetwork();
		router.createPTRoutes();
		router.cleanStationsAndNetwork();
		new TransitScheduleWriter(transitSchedule).writeFile(utils.getOutputDirectory() + "Schedule_AdditionalTest.xml");
		new NetworkWriter(network).write(utils.getOutputDirectory() + "Network_AdditionalTest.xml");
	}
}
