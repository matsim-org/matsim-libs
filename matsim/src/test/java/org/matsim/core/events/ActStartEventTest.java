/* *********************************************************************** *
 * project: org.matsim.*
 * ActStartEventTest.java
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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.facilities.ActivityFacility;
import org.matsim.testcases.MatsimTestCase;

/**
 * @author mrieser
 */
public class ActStartEventTest extends MatsimTestCase {

	public void testWriteReadXml() {
		final ActivityStartEvent event = XmlEventsTester.testWriteReadXml(getOutputDirectory() + "events.xml",
				new ActivityStartEvent(5668.27, Id.create("a92", Person.class), Id.create("l081", Link.class), Id.create("f792", org.matsim.facilities.Facility.class), "work"));
		assertEquals(5668.27, event.getTime(), EPSILON);
		assertEquals("a92", event.getPersonId().toString());
		assertEquals("l081", event.getLinkId().toString());
		assertEquals("f792", event.getFacilityId().toString());
		assertEquals("work", event.getActType());
	}
}
