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

import org.matsim.core.basic.v01.IdImpl;
import org.matsim.testcases.MatsimTestCase;

/**
 * @author mrieser
 */
public class ActStartEventTest extends MatsimTestCase {

	public void testWriteReadXml() {
		final ActivityStartEventImpl event = XmlEventsTester.testWriteReadXml(getOutputDirectory() + "events.xml",
				new ActivityStartEventImpl(5668.27, new IdImpl("a92"), new IdImpl("l081"), new IdImpl("f792"), "work"));
		assertEquals(5668.27, event.getTime(), EPSILON);
		assertEquals("a92", event.getPersonId().toString());
		assertEquals(new IdImpl("l081"), event.getLinkId());
		assertEquals(new IdImpl("f792"), event.getFacilityId());
		assertEquals("work", event.getActType());
	}
}
