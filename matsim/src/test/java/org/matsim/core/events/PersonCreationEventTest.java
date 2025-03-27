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
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonCreationEvent;
import org.matsim.testcases.MatsimTestUtils;

public class PersonCreationEventTest {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();


	@Test
	void testWriteReadXml() {
		final PersonCreationEvent event1 = new PersonCreationEvent(0, Id.createPersonId("testPerson"));
		final PersonCreationEvent event2 = XmlEventsTester.testWriteReadXml(utils.getOutputDirectory() + "events.xml", event1);
		assertEquals(event1.getTime(), event2.getTime(), MatsimTestUtils.EPSILON);
		assertEquals(event1.getPersonId(), event2.getPersonId());
		assertNull(event2.getCoord());
	}
	
	@Test
	void testWriteReadXmlWithCoord() {
		final PersonCreationEvent event1 = new PersonCreationEvent(0, Id.createPersonId("testPerson"), new Coord(12345, 67890));
		final PersonCreationEvent event2 = XmlEventsTester.testWriteReadXml(utils.getOutputDirectory() + "events.xml", event1);
		assertEquals(event1.getTime(), event2.getTime(), MatsimTestUtils.EPSILON);
		assertEquals(event1.getPersonId(), event2.getPersonId());
		assertEquals(event1.getCoord(), event2.getCoord());
	}

}
