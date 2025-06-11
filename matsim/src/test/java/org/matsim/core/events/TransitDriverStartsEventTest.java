/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.population.Person;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.Vehicle;

/**
 * @author mrieser
 */
public class TransitDriverStartsEventTest {

	@RegisterExtension private MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void testWriteReadXml() {
		final TransitDriverStartsEvent event1 = new TransitDriverStartsEvent(36095.2,
				Id.create("ptDrvr-1", Person.class),
				Id.create("vehicle-bus5", Vehicle.class),
				Id.create("line L-1", TransitLine.class),
				Id.create("route-R1", TransitRoute.class),
				Id.create("departure-D-1", Departure.class));
		final TransitDriverStartsEvent event2 = XmlEventsTester.testWriteReadXml(this.utils.getOutputDirectory() + "events.xml", event1);
		Assertions.assertEquals(event1.getTime(), event2.getTime(), 1.0e-9);
		Assertions.assertEquals(event1.getDriverId(), event2.getDriverId());
		Assertions.assertEquals(event1.getVehicleId(), event2.getVehicleId());
		Assertions.assertEquals(event1.getTransitRouteId(), event2.getTransitRouteId());
		Assertions.assertEquals(event1.getTransitLineId(), event2.getTransitLineId());
		Assertions.assertEquals(event1.getDepartureId(), event2.getDepartureId());
	}
}
