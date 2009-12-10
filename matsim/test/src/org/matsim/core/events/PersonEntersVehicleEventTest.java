/* *********************************************************************** *
 * project: org.matsim.*
 * PersonEntersVehicleEventTest.java
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

import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.vehicles.BasicVehicle;
import org.matsim.vehicles.BasicVehicleImpl;
import org.matsim.vehicles.BasicVehicleType;
import org.matsim.vehicles.BasicVehicleTypeImpl;

/**
 * @author mrieser
 */
public class PersonEntersVehicleEventTest extends MatsimTestCase {

	public void testReadWriteXml() {
		PersonImpl person = new PersonImpl(new IdImpl(1));
		BasicVehicleType vehicleType = new BasicVehicleTypeImpl(new IdImpl("testVehType"));
		BasicVehicle vehicle = new BasicVehicleImpl(new IdImpl(80), vehicleType);
		PersonEntersVehicleEventImpl event = new PersonEntersVehicleEventImpl(5.0 * 3600 + 11.0 * 60, person, vehicle, new IdImpl("testRouteId"));
		PersonEntersVehicleEventImpl event2 = XmlEventsTester.testWriteReadXml(getOutputDirectory() + "events.xml", event);
		assertEquals("wrong time of event.", 5.0 * 3600 + 11.0 * 60, event2.getTime(), EPSILON);
		assertEquals("wrong vehicle id.", "80", event2.getVehicleId().toString());
	}
}
