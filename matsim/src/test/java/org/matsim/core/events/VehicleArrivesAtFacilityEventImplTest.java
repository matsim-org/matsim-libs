/* *********************************************************************** *
 * project: org.matsim.*
 * BasicVehicleArrivesAtFacilityEventTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.Vehicle;

public class VehicleArrivesAtFacilityEventImplTest {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();


	@Test
	void testWriteReadXml() {
		VehicleArrivesAtFacilityEvent event = new VehicleArrivesAtFacilityEvent(Time.parseTime("10:55:00"),
				Id.create(5, Vehicle.class),
				Id.create(11, TransitStopFacility.class),
				-1.2);
		VehicleArrivesAtFacilityEvent event2 = XmlEventsTester.testWriteReadXml(utils.getOutputDirectory() + "events.xml", event);
		assertEquals(Time.parseTime("10:55:00"), event2.getTime(), MatsimTestUtils.EPSILON);
		assertEquals(Id.create(5, Vehicle.class), event2.getVehicleId());
		assertEquals(Id.create(11, TransitStopFacility.class), event2.getFacilityId());
		assertEquals(Double.valueOf(-1.2), Double.valueOf(event2.getDelay()));
	}
}
