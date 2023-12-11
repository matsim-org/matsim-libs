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

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author mrieser
 */
public class PersonArrivalEventTest {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();


	@Test
	void testWriteReadXml() {
		final PersonArrivalEvent event = XmlEventsTester.testWriteReadXml(utils.getOutputDirectory() + "events.xml",
				new PersonArrivalEvent(68423.98, Id.create("443", Person.class), Id.create("78-3", Link.class), TransportMode.bike));
		assertEquals(68423.98, event.getTime(), MatsimTestUtils.EPSILON);
		assertEquals(Id.create("443", Person.class), event.getPersonId());
		assertEquals(Id.create("78-3", Link.class), event.getLinkId());
		assertEquals(TransportMode.bike, event.getLegMode());
	}
}
