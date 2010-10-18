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

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author mrieser
 */
public class TransitDriverStartsEventTest {

	@Rule public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void testWriteReadXml() {
		final TransitDriverStartsEvent event1 = new TransitDriverStartsEvent(36095.2, new IdImpl("ptDrvr-1"), new IdImpl("vehicle-bus5"), new IdImpl("line L-1"), new IdImpl("route-R1"), new IdImpl("departure-D-1"));
		final TransitDriverStartsEvent event2 = XmlEventsTester.testWriteReadXml(this.utils.getOutputDirectory() + "events.xml", event1);
		Assert.assertEquals(event1.getTime(), event2.getTime(), 1.0e-9);
		Assert.assertEquals(event1.getDriverId(), event2.getDriverId());
		Assert.assertEquals(event1.getVehicleId(), event2.getVehicleId());
		Assert.assertEquals(event1.getTransitRouteId(), event2.getTransitRouteId());
		Assert.assertEquals(event1.getTransitLineId(), event2.getTransitLineId());
		Assert.assertEquals(event1.getDepartureId(), event2.getDepartureId());
	}
}
