/* *********************************************************************** *
 * project: org.matsim.*
 * AgentStuckEventTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author mrieser
 */
public class PersonStuckEventTest {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();


	@Test
	void testWriteReadXml() {
		final PersonStuckEvent event1 = new PersonStuckEvent(81153.3, Id.create("a007", Person.class), Id.create("link1", Link.class), TransportMode.walk);
		final PersonStuckEvent event2 = XmlEventsTester.testWriteReadXml(utils.getOutputDirectory() + "events.xml", event1);
		assertEquals(event1.getTime(), event2.getTime(), MatsimTestUtils.EPSILON);
		assertEquals(event1.getPersonId(), event2.getPersonId());
		assertEquals(event1.getLinkId(), event2.getLinkId());
		assertEquals(event1.getLegMode(), event2.getLegMode());
	}

	@Test
	void testWriteReadXmlWithLinkIdNull() {
		final PersonStuckEvent event1 = new PersonStuckEvent(81153.3, Id.create("a007", Person.class), null, TransportMode.walk);
		final PersonStuckEvent event2 = XmlEventsTester.testWriteReadXml(utils.getOutputDirectory() + "events.xml", event1);
		assertEquals(event1.getTime(), event2.getTime(), MatsimTestUtils.EPSILON);
		assertEquals(event1.getPersonId(), event2.getPersonId());
		assertEquals(event1.getLinkId(), null);
		assertEquals(event1.getLegMode(), event2.getLegMode());
	}

}
