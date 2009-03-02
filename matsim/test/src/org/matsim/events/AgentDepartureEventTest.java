/* *********************************************************************** *
 * project: org.matsim.*
 * AgentDepartureEventTest.java
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
public class AgentDepartureEventTest extends MatsimTestCase {

	public void testWriteReadXml() {
		final AgentDepartureEvent event = XmlEventsTester.testWriteReadXml(getOutputDirectory() + "events.xml",
				new AgentDepartureEvent(25669.05, "921", "390", 1));
		assertEquals(25669.05, event.time, EPSILON);
		assertEquals("921", event.agentId);
		assertEquals("390", event.linkId);
	}
}
