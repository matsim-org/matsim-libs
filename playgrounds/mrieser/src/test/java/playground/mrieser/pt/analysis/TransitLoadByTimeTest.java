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

package playground.mrieser.pt.analysis;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.events.PersonEntersVehicleEventImpl;
import org.matsim.core.events.PersonLeavesVehicleEventImpl;

public class TransitLoadByTimeTest {

	@Test
	public void testTransitLoad_singleLine() {
		Id[] id = {new IdImpl(0), new IdImpl(1), new IdImpl(2), new IdImpl(3)};

		Id routeId1 = id[1];
		Id vehicleIdDep1 = id[0];
		Id vehicleIdDep2 = id[3];

		TransitLoadByTime tl = new TransitLoadByTime();
		tl.handleEvent(new PersonEntersVehicleEventImpl(7.0*3600-5, id[0], vehicleIdDep1, routeId1));

		tl.handleEvent(new PersonLeavesVehicleEventImpl(7.1*3600-5, id[0], vehicleIdDep1, routeId1));
		tl.handleEvent(new PersonEntersVehicleEventImpl(7.1*3600, id[1], vehicleIdDep1, routeId1));
		tl.handleEvent(new PersonEntersVehicleEventImpl(7.1*3600+5, id[2], vehicleIdDep1, routeId1));

		tl.handleEvent(new PersonLeavesVehicleEventImpl(7.2*3600-5, id[2], vehicleIdDep1, routeId1));
		tl.handleEvent(new PersonEntersVehicleEventImpl(7.2*3600, id[3], vehicleIdDep1, routeId1));

		tl.handleEvent(new PersonLeavesVehicleEventImpl(7.3*3600-5, id[1], vehicleIdDep1, routeId1));
		tl.handleEvent(new PersonLeavesVehicleEventImpl(7.3*3600, id[3], vehicleIdDep1, routeId1));

		Assert.assertEquals(0, tl.getVehicleLoad(vehicleIdDep1, 7.0*3600 - 10));
		Assert.assertEquals(1, tl.getVehicleLoad(vehicleIdDep1, 7.0*3600 - 5));
		Assert.assertEquals(1, tl.getVehicleLoad(vehicleIdDep1, 7.1*3600 - 6));
		Assert.assertEquals(0, tl.getVehicleLoad(vehicleIdDep1, 7.1*3600 - 5));
		Assert.assertEquals(1, tl.getVehicleLoad(vehicleIdDep1, 7.1*3600));
		Assert.assertEquals(2, tl.getVehicleLoad(vehicleIdDep1, 7.1*3600 + 5));
		Assert.assertEquals(1, tl.getVehicleLoad(vehicleIdDep1, 7.2*3600 - 5));
		Assert.assertEquals(2, tl.getVehicleLoad(vehicleIdDep1, 7.2*3600));
		Assert.assertEquals(1, tl.getVehicleLoad(vehicleIdDep1, 7.3*3600 - 5));
		Assert.assertEquals(0, tl.getVehicleLoad(vehicleIdDep1, 7.3*3600));
		Assert.assertEquals(0, tl.getVehicleLoad(vehicleIdDep1, 7.3*3600 + 1));

		Assert.assertEquals(0, tl.getVehicleLoad(vehicleIdDep2, 7.0*3600));
		Assert.assertEquals(0, tl.getVehicleLoad(vehicleIdDep2, 7.3*3600));
	}
}
