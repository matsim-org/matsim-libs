/* *********************************************************************** *
 * project: org.matsim.*
 * ControlerConfigGroupTest.java
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

package org.matsim.core.config.groups;

import java.util.EnumSet;
import java.util.Set;

import junit.framework.TestCase;

import org.matsim.core.config.groups.ControlerConfigGroup.EventsFileFormat;

public class ControlerConfigGroupTest extends TestCase {

	/**
	 * Ensure that the events-file-format is correctly stored
	 * and returned with the different setters and getters.
	 *
	 * @author mrieser
	 */
	public void testEventsFileFormat() {
		ControlerConfigGroup cg = new ControlerConfigGroup();
		Set<EventsFileFormat> formats;
		// test initial value
		formats = cg.getEventsFileFormats();
		assertEquals(1, formats.size());
		assertTrue(formats.contains(EventsFileFormat.txt));
		assertEquals("txt", cg.getValue(ControlerConfigGroup.EVENTS_FILE_FORMAT));
		// test setting with setEventsFileFormat
		cg.setEventsFileFormats(EnumSet.of(EventsFileFormat.txt, EventsFileFormat.xml));
		formats = cg.getEventsFileFormats();
		assertEquals(2, formats.size());
		assertTrue(formats.contains(EventsFileFormat.txt));
		assertTrue(formats.contains(EventsFileFormat.xml));
		assertEquals("txt,xml", cg.getValue(ControlerConfigGroup.EVENTS_FILE_FORMAT));
		// test setting to none
		cg.setEventsFileFormats(EnumSet.noneOf(EventsFileFormat.class));
		formats = cg.getEventsFileFormats();
		assertEquals(0, formats.size());
		assertEquals("", cg.getValue(ControlerConfigGroup.EVENTS_FILE_FORMAT));
		// test setting with addParam
		cg.addParam(ControlerConfigGroup.EVENTS_FILE_FORMAT, "xml,txt");
		formats = cg.getEventsFileFormats();
		assertEquals(2, formats.size());
		assertTrue(formats.contains(EventsFileFormat.xml));
		assertTrue(formats.contains(EventsFileFormat.txt));
		assertEquals("txt,xml", cg.getValue(ControlerConfigGroup.EVENTS_FILE_FORMAT));
		// test setting to none
		cg.addParam(ControlerConfigGroup.EVENTS_FILE_FORMAT, "");
		formats = cg.getEventsFileFormats();
		assertEquals(0, formats.size());
		assertEquals("", cg.getValue(ControlerConfigGroup.EVENTS_FILE_FORMAT));
		// test setting with non-conform formatting
		cg.addParam(ControlerConfigGroup.EVENTS_FILE_FORMAT, " txt\t, xml\t\t  ");
		formats = cg.getEventsFileFormats();
		assertEquals(2, formats.size());
		assertTrue(formats.contains(EventsFileFormat.txt));
		assertTrue(formats.contains(EventsFileFormat.xml));
		assertEquals("txt,xml", cg.getValue(ControlerConfigGroup.EVENTS_FILE_FORMAT));
		// test setting to non-conform none
		cg.addParam(ControlerConfigGroup.EVENTS_FILE_FORMAT, "  \t ");
		formats = cg.getEventsFileFormats();
		assertEquals(0, formats.size());
		assertEquals("", cg.getValue(ControlerConfigGroup.EVENTS_FILE_FORMAT));
	}
}
