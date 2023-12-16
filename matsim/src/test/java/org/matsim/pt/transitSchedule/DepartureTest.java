/* *********************************************************************** *
 * project: org.matsim.*
 * DepartureTest.java
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

package org.matsim.pt.transitSchedule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.Vehicle;

/**
 * @author mrieser
 */
public class DepartureTest {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();


	/**
	 * In case we once should have more than one implementation of
	 * {@link Departure}, simply inherit from this test and overwrite
	 * this method to return your own implementation.
	 *
	 * @param id
	 * @param time
	 * @return a new instance of a Departure with the given attributes
	 */
	protected Departure createDeparture(final Id<Departure> id, final double time) {
		return new DepartureImpl(id, time);
	}

	@Test
	void testInitialization() {
		Id<Departure> id = Id.create(1591, Departure.class);
		double time = 11.0 * 3600;
		Departure dep = createDeparture(id, time);
		assertEquals(id, dep.getId());
		assertEquals(time, dep.getDepartureTime(), MatsimTestUtils.EPSILON);
	}

	@Test
	void testVehicleId() {
		Departure dep = createDeparture(Id.create(6791, Departure.class), 7.0*3600);
		assertNull(dep.getVehicleId());
		Id<Vehicle> vehId = Id.create(2491, Vehicle.class);
		dep.setVehicleId(vehId);
		assertEquals(vehId, dep.getVehicleId());
	}
}
