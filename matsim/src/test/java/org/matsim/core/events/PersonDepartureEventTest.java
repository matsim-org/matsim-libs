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

package org.matsim.core.events;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.testcases.MatsimTestCase;

/**
 * @author mrieser
 */
public class PersonDepartureEventTest extends MatsimTestCase {

	public void testWriteReadXml() {
		final PersonDepartureEvent event = XmlEventsTester.testWriteReadXml(getOutputDirectory() + "events.xml",
				new PersonDepartureEvent(25669.05, Id.create("921", Person.class), Id.create("390", Link.class), TransportMode.bike, "bikeRoutingMode"));
		assertEquals(25669.05, event.getTime(), EPSILON);
		assertEquals("921", event.getPersonId().toString());
		assertEquals("390", event.getLinkId().toString());
		assertEquals("bike", event.getLegMode());
		assertEquals("bikeRoutingMode", event.getRoutingMode());
	}
}
