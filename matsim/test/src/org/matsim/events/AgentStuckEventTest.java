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

package org.matsim.events;

import org.matsim.basic.v01.IdImpl;
import org.matsim.testcases.MatsimTestCase;

/**
 * @author mrieser
 */
public class AgentStuckEventTest extends MatsimTestCase {

	public void testWriteReadXml() {
		final AgentStuckEvent event1 = new AgentStuckEvent(81153.3, new IdImpl("a007"), new IdImpl("link1"));
		final AgentStuckEvent event2 = XmlEventsTester.testWriteReadXml(getOutputDirectory() + "events.xml", event1);
		assertEquals(event1.getTime(), event2.getTime(), EPSILON);
		assertEquals(event1.getPersonId(), event2.getPersonId());
		assertEquals(event1.getLinkId(), event2.getLinkId());
	}
}
