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

package org.matsim.core.events;

import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.events.AgentArrivalEvent;
import org.matsim.testcases.MatsimTestCase;

/**
 * @author mrieser
 */
public class AgentArrivalEventTest extends MatsimTestCase {

	public void testWriteReadXml() {
		final AgentArrivalEvent event = XmlEventsTester.testWriteReadXml(getOutputDirectory() + "events.xml",
				new AgentArrivalEvent(68423.98, new IdImpl("443"), new IdImpl("78-3")));
		assertEquals(68423.98, event.getTime(), EPSILON);
		assertEquals(new IdImpl("443"), event.getPersonId());
		assertEquals(new IdImpl("78-3"), event.getLinkId());
	}
}
