/* *********************************************************************** *
 * project: org.matsim.*
 * PersonLeavesVehicleEventTest.java
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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.population.PopulationUtils;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleImpl;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleTypeImpl;

/**
 * @author mrieser
 */
public class PersonLeavesVehicleEventTest extends MatsimTestCase {

	public void testWriteReadXml() {
		Person person = PopulationUtils.getFactory().createPerson(Id.create(1, Person.class));
		VehicleType vehicleType = new VehicleTypeImpl(Id.create("testVehType", VehicleType.class));
		Vehicle vehicle = new VehicleImpl(Id.create(80, Vehicle.class), vehicleType);
		PersonLeavesVehicleEvent event = new PersonLeavesVehicleEvent(5.0 * 3600 + 11.0 * 60, person.getId(), vehicle.getId());
		PersonLeavesVehicleEvent event2 = XmlEventsTester.testWriteReadXml(getOutputDirectory() + "events.xml", event);
		assertEquals("wrong time of event.", 5.0 * 3600 + 11.0 * 60, event2.getTime(), EPSILON);
		assertEquals("wrong vehicle id.", "80", event2.getVehicleId().toString());
	}
}
