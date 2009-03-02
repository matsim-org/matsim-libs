/* *********************************************************************** *
 * project: org.matsim.*
 * AgentArrivalEventTest.java
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
public class AgentArrivalEventTest extends MatsimTestCase {

	public void testWriteReadXml() {
		final AgentArrivalEvent event = XmlEventsTester.testWriteReadXml(getOutputDirectory() + "events.xml",
				new AgentArrivalEvent(68423.98, "443", "78-3"));
		assertEquals(68423.98, event.time, EPSILON);
		assertEquals("443", event.agentId);
		assertEquals("78-3", event.linkId);
	}
}
