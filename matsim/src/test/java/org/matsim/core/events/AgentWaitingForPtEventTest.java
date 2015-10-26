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

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.AgentWaitingForPtEvent;
import org.matsim.core.population.PersonImpl;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author mrieser / senozon
 */
public class AgentWaitingForPtEventTest {

	@Rule public MatsimTestUtils helper = new MatsimTestUtils();

	@Test
	public void testReadWriteXml() {
		Person person = PersonImpl.createPerson(Id.create(1, Person.class));
		Id<TransitStopFacility> waitStopId = Id.create("1980", TransitStopFacility.class);
		Id<TransitStopFacility> destinationStopId = Id.create("0511", TransitStopFacility.class);
		double time = 5.0 * 3600 + 11.0 + 60;
		AgentWaitingForPtEvent event = new AgentWaitingForPtEvent(time, person.getId(), waitStopId, destinationStopId);
		AgentWaitingForPtEvent event2 = XmlEventsTester.testWriteReadXml(helper.getOutputDirectory() + "events.xml", event);
		Assert.assertEquals("wrong time of event.", time, event2.getTime(), 1e-8);
		Assert.assertEquals("1", event2.getPersonId().toString());
		Assert.assertEquals("1980", event2.getWaitingAtStopId().toString());
		Assert.assertEquals("0511", event2.getDestinationStopId().toString());
	}

}
