/*
 *   *********************************************************************** *
 *   project: org.matsim.*
 *   *********************************************************************** *
 *                                                                           *
 *   copyright       : (C)  by the members listed in the COPYING,        *
 *                     LICENSE and WARRANTY file.                            *
 *   email           : info at matsim dot org                                *
 *                                                                           *
 *   *********************************************************************** *
 *                                                                           *
 *     This program is free software; you can redistribute it and/or modify  *
 *     it under the terms of the GNU General Public License as published by  *
 *     the Free Software Foundation; either version 2 of the License, or     *
 *     (at your option) any later version.                                   *
 *     See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                           *
 *   ***********************************************************************
 *
 */

package org.matsim.contrib.freight.events;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.core.events.XmlEventsTester;
import org.matsim.testcases.MatsimTestCase;

/**
 * @author Kai Martins-Turner (kturner)
 */
public class LSPTourStartEventTest extends MatsimTestCase {

	public void testWriteReadXml() {
		final var actEndEvent = new ActivityEndEvent(7893.14, Id.create("143", Person.class), Id.create("293", Link.class), null,
				"start", null);
		final LSPTourStartEvent event = (LSPTourStartEvent) XmlEventsTester.testWriteReadXml(getOutputDirectory() + "events.xml",
				new LSPTourStartEvent(actEndEvent, Id.create("carrier5", Carrier.class), Id.createVehicleId("veh7")));
		assertEquals(7893.14, event.getTime(), EPSILON);
		assertEquals("143", event.getPersonId().toString());
		assertEquals("293", event.getLinkId().toString());
		assertEquals("starts", event.getEventType());
		assertEquals("carrier5", event.getCarrierId().toString());
		assertEquals("veh7", event.getVehicleId());
	}
}
