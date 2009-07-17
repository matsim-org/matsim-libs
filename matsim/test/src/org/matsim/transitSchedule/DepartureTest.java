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

package org.matsim.transitSchedule;

import org.matsim.api.basic.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.transitSchedule.api.Departure;
import org.matsim.vehicles.BasicVehicle;
import org.matsim.vehicles.BasicVehicleImpl;

/**
 * @author mrieser
 */
public class DepartureTest extends MatsimTestCase {

	/**
	 * In case we once should have more than one implementation of
	 * {@link Departure}, simply inherit from this test and overwrite
	 * this method to return your own implementation.
	 *
	 * @param id
	 * @param time
	 * @return a new instance of a Departure with the given attributes
	 */
	protected Departure createDeparture(final Id id, final double time) {
		return new DepartureImpl(id, time);
	}

	public void testInitialization() {
		Id id = new IdImpl(1591);
		double time = 11.0 * 3600;
		Departure dep = createDeparture(id, time);
		assertEquals(id, dep.getId());
		assertEquals(time, dep.getDepartureTime(), EPSILON);
	}

	public void testVehicle() {
		Departure dep = createDeparture(new IdImpl(6791), 7.0*3600);
		assertNull(dep.getVehicle());
		BasicVehicle veh = new BasicVehicleImpl(new IdImpl(2491), null);
		dep.setVehicle(veh);
		assertEquals(veh, dep.getVehicle());
	}

}
