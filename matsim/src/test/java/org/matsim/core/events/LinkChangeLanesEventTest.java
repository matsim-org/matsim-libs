/* *********************************************************************** *
 * project: org.matsim.*
 * LinkChangeLanesEventTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.testcases.MatsimTestCase;

/**
 * @author cdobler
 */
public class LinkChangeLanesEventTest extends MatsimTestCase {

	public void testWriteReadXml() {
		
		final NetworkChangeEvent.ChangeValue changeValue = new NetworkChangeEvent.ChangeValue(NetworkChangeEvent.ChangeType.ABSOLUTE, 100.0);
		final LinkChangeLanesEventImpl event1 = new LinkChangeLanesEventImpl(6823.8, new IdImpl("abcd"), changeValue);
		final LinkChangeLanesEventImpl event2 = XmlEventsTester.testWriteReadXml(getOutputDirectory() + "events.xml", event1);
		
		assertEquals(event1.getTime(), event2.getTime(), EPSILON);
		assertEquals(event1.getLinkId(), event2.getLinkId());
		assertEquals(event1.getChangeValue().getType(), event2.getChangeValue().getType());
		assertEquals(event1.getChangeValue().getValue(), event2.getChangeValue().getValue());
	}
}
