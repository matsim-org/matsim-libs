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

import org.matsim.core.basic.v01.IdImpl;
import org.matsim.testcases.MatsimTestCase;

/**
 * @author mrieser
 */
public class ActEndEventTest extends MatsimTestCase {

	public void testWriteReadXml() {
		final ActivityEndEventImpl event = XmlEventsTester.testWriteReadXml(getOutputDirectory() + "events.xml",
				new ActivityEndEventImpl(7893.14, new IdImpl("143"), new IdImpl("293"), new IdImpl("f811"), "home"));
		assertEquals(7893.14, event.getTime(), EPSILON);
		assertEquals("143", event.getPersonId().toString());
		assertEquals(new IdImpl("293"), event.getLinkId());
		assertEquals(new IdImpl("f811"), event.getFacilityId());
		assertEquals("home", event.getActType());
	}
}
