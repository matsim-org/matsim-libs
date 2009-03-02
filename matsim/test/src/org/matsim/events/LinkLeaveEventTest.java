/* *********************************************************************** *
 * project: org.matsim.*
 * LinkLeaveEventTest.java
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
public class LinkLeaveEventTest extends MatsimTestCase {

	public void testWriteReadXml() {
		final LinkLeaveEvent event1 = new LinkLeaveEvent(68423.98, "648", ".235");
		final LinkLeaveEvent event2 = XmlEventsTester.testWriteReadXml(getOutputDirectory() + "events.xml", event1);
		assertEquals(event1.time, event2.time, EPSILON);
		assertEquals(event1.agentId, event2.agentId);
		assertEquals(event1.linkId, event2.linkId);
	}
}
