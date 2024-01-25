/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.AgentWaitingForPtEvent;
import org.matsim.core.population.PopulationUtils;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author mrieser / senozon
 */
public class AgentWaitingForPtEventTest {

	@RegisterExtension private MatsimTestUtils helper = new MatsimTestUtils();

	@Test
	void testReadWriteXml() {
		Person person = PopulationUtils.getFactory().createPerson(Id.create(1, Person.class));
		Id<TransitStopFacility> waitStopId = Id.create("1980", TransitStopFacility.class);
		Id<TransitStopFacility> destinationStopId = Id.create("0511", TransitStopFacility.class);
		double time = 5.0 * 3600 + 11.0 + 60;
		AgentWaitingForPtEvent event = new AgentWaitingForPtEvent(time, person.getId(), waitStopId, destinationStopId);
		AgentWaitingForPtEvent event2 = XmlEventsTester.testWriteReadXml(helper.getOutputDirectory() + "events.xml", event);
		Assertions.assertEquals(time, event2.getTime(), 1e-8, "wrong time of event.");
		Assertions.assertEquals("1", event2.getPersonId().toString());
		Assertions.assertEquals("1980", event2.getWaitingAtStopId().toString());
		Assertions.assertEquals("0511", event2.getDestinationStopId().toString());
	}

}
