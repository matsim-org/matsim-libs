/* *********************************************************************** *
 * project: org.matsim.*
 * Plansgenerator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.events.GenericEvent;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author droeder
 *
 */
public class GenericEventTest {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();


	@Test
	void testWriteReadXml() {
		final String TYPE = "GenericEvent";
		final String KEY1 = "k1";
		final String VALUE1 = "v1";
		final double time = 3601;

		GenericEvent writeEvent = new GenericEvent(TYPE, time);
		writeEvent.getAttributes().put(KEY1, VALUE1);

		GenericEvent readEvent = XmlEventsTester.testWriteReadXml(utils.getOutputDirectory() + "events.xml", writeEvent);

		assertEquals(TYPE, readEvent.getAttributes().get("type"));
		assertEquals(VALUE1, readEvent.getAttributes().get(KEY1));
		assertEquals(String.valueOf(time), readEvent.getAttributes().get("time"));
		assertEquals(time, readEvent.getTime(), 0);

	}
}
