/* *********************************************************************** *
 * project: org.matsim.*
 * ActEndEventTest.java
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
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.facilities.ActivityFacility;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author mrieser
 */
public class ActEndEventTest {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();


	@Test
	void testWriteReadXml() {
		final ActivityEndEvent event = XmlEventsTester.testWriteReadXml(utils.getOutputDirectory() + "events.xml",
				new ActivityEndEvent(7893.14, Id.create("143", Person.class), Id.create("293", Link.class), Id.create("f811", ActivityFacility.class),
						"home", new Coord( 234., 5.67 )));
		assertEquals(7893.14, event.getTime(), MatsimTestUtils.EPSILON);
		assertEquals("143", event.getPersonId().toString());
		assertEquals("293", event.getLinkId().toString());
		assertEquals("f811", event.getFacilityId().toString());
		assertEquals("home", event.getActType());
	}
}
