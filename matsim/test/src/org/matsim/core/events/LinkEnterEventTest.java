/* *********************************************************************** *
 * project: org.matsim.*
 * LinkEnterEventTest.java
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
import org.matsim.core.events.LinkEnterEvent;
import org.matsim.testcases.MatsimTestCase;

/**
 * @author mrieser
 */
public class LinkEnterEventTest extends MatsimTestCase {

	public void testWriteReadXml() {
		final LinkEnterEvent event1 = new LinkEnterEvent(6823.8, new IdImpl("agfe"), new IdImpl("abcd"));
		final LinkEnterEvent event2 = XmlEventsTester.testWriteReadXml(getOutputDirectory() + "events.xml", event1);
		assertEquals(event1.getTime(), event2.getTime(), EPSILON);
		assertEquals(event1.getPersonId(), event2.getPersonId());
		assertEquals(event1.getLinkId(), event2.getLinkId());
	}
}
