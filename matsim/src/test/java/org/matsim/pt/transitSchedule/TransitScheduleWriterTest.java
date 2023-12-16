/* *********************************************************************** *
 * project: org.matsim.*
 * TransitScheduleWriterTest.java
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

package org.matsim.pt.transitSchedule;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.core.population.routes.RouteFactories;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.testcases.MatsimTestUtils;
import org.xml.sax.SAXException;

/**
 * @author mrieser
 */
public class TransitScheduleWriterTest {

	@RegisterExtension private MatsimTestUtils utils = new MatsimTestUtils();

	/**
	 * Tests that the default format written is in v2 format.
	 *
	 * @throws IOException
	 * @throws SAXException
	 * @throws ParserConfigurationException
	 */
	@Test
	void testDefaultV2() throws IOException, SAXException, ParserConfigurationException {
		String filename = this.utils.getOutputDirectory() + "schedule.xml";

		TransitScheduleFactory builder = new TransitScheduleFactoryImpl();
		TransitSchedule schedule = builder.createTransitSchedule();
		TransitLine line = builder.createTransitLine(Id.create(1, TransitLine.class));
		schedule.addTransitLine(line);

		TransitScheduleWriter writer = new TransitScheduleWriter(schedule);
		writer.writeFile(filename);

		TransitScheduleFactory builder2 = new TransitScheduleFactoryImpl();
		TransitSchedule schedule2 = builder2.createTransitSchedule();
		new TransitScheduleReaderV2(schedule2, new RouteFactories()).readFile(filename);
		Assertions.assertEquals(1, schedule2.getTransitLines().size());
	}

	@Test
	void testTransitLineName() {
		String filename = this.utils.getOutputDirectory() + "schedule.xml";

		TransitScheduleFactory builder = new TransitScheduleFactoryImpl();
		TransitSchedule schedule = builder.createTransitSchedule();
		TransitLine line = builder.createTransitLine(Id.create(1, TransitLine.class));
		line.setName("Blue line");
		schedule.addTransitLine(line);

		TransitScheduleWriter writer = new TransitScheduleWriter(schedule);
		writer.writeFile(filename);

		TransitScheduleFactory builder2 = new TransitScheduleFactoryImpl();
		TransitSchedule schedule2 = builder2.createTransitSchedule();
		new TransitScheduleReaderV1(schedule2, new RouteFactories()).readFile(filename);
		Assertions.assertEquals(1, schedule2.getTransitLines().size());
		Assertions.assertEquals("Blue line", schedule2.getTransitLines().get(Id.create(1, TransitLine.class)).getName());
	}
}
