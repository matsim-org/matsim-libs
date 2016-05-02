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

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.core.population.routes.RouteFactoryImpl;
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

	@Rule public MatsimTestUtils utils = new MatsimTestUtils();
	
	/**
	 * Tests that the default format written is in v1 format.
	 *
	 * @throws IOException
	 * @throws SAXException
	 * @throws ParserConfigurationException
	 */
	@Test
	public void testDefaultV1() throws IOException, SAXException, ParserConfigurationException {
		String filename = this.utils.getOutputDirectory() + "schedule.xml";

		TransitScheduleFactory builder = new TransitScheduleFactoryImpl();
		TransitSchedule schedule = builder.createTransitSchedule();
		TransitLine line = builder.createTransitLine(Id.create(1, TransitLine.class));
		schedule.addTransitLine(line);

		TransitScheduleWriter writer = new TransitScheduleWriter(schedule);
		writer.writeFile(filename);

		TransitScheduleFactory builder2 = new TransitScheduleFactoryImpl();
		TransitSchedule schedule2 = builder2.createTransitSchedule();
		new TransitScheduleReaderV1(schedule2, new RouteFactoryImpl()).readFile(filename);
		Assert.assertEquals(1, schedule2.getTransitLines().size());
	}
	
	@Test
	public void testTransitLineName() {
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
		new TransitScheduleReaderV1(schedule2, new RouteFactoryImpl()).readFile(filename);
		Assert.assertEquals(1, schedule2.getTransitLines().size());
		Assert.assertEquals("Blue line", schedule2.getTransitLines().get(Id.create(1, TransitLine.class)).getName());
	}
}
