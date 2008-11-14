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

package org.matsim.events;

import org.matsim.testcases.MatsimTestCase;

/**
 * @author mrieser
 */
public class ActStartEventTest extends MatsimTestCase {

	public void testWriteReadXml() {
		final ActStartEvent event = XmlEventsTester.testWriteReadXml(getOutputDirectory() + "events.xml",
				new ActStartEvent(5668.27, "a92", "l081", 1, "work"));
		assertEquals(5668.27, event.time, EPSILON);
		assertEquals("a92", event.agentId);
		assertEquals("l081", event.linkId);
		assertEquals("work", event.acttype);
	}
}
