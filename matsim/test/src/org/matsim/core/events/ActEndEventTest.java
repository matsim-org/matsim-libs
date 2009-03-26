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
import org.matsim.core.events.ActEndEvent;
import org.matsim.testcases.MatsimTestCase;

/**
 * @author mrieser
 */
public class ActEndEventTest extends MatsimTestCase {

	public void testWriteReadXml() {
		final ActEndEvent event = XmlEventsTester.testWriteReadXml(getOutputDirectory() + "events.xml",
				new ActEndEvent(7893.14, new IdImpl("143"), new IdImpl("293"), "home"));
		assertEquals(7893.14, event.getTime(), EPSILON);
		assertEquals("143", event.getPersonId().toString());
		assertEquals(new IdImpl("293"), event.getLinkId());
		assertEquals("home", event.getActType());
	}
}
