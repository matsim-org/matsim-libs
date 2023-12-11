/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package org.matsim.facilities;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;

/**
 * @author mrieser / Senozon AG
 */
public class ActivityFacilitiesImplTest {

	@Test
	void testAddActivityFacility() {
		ActivityFacilities facilities = new ActivityFacilitiesImpl();
		ActivityFacilitiesFactory factory = facilities.getFactory();
		ActivityFacility facility1 = factory.createActivityFacility(Id.create(1, ActivityFacility.class), new Coord((double) 200, (double) 5000));

		Assertions.assertEquals(0, facilities.getFacilities().size());

		facilities.addActivityFacility(facility1);

		Assertions.assertEquals(1, facilities.getFacilities().size());

		ActivityFacility facility2 = factory.createActivityFacility(Id.create(2, ActivityFacility.class), new Coord((double) 300, (double) 4000));
		facilities.addActivityFacility(facility2);

		Assertions.assertEquals(2, facilities.getFacilities().size());
	}

	@Test
	void testAddActivityFacility_addingTwice() {
		ActivityFacilities facilities = new ActivityFacilitiesImpl();
		ActivityFacilitiesFactory factory = facilities.getFactory();
		ActivityFacility facility1 = factory.createActivityFacility(Id.create(1, ActivityFacility.class), new Coord((double) 200, (double) 5000));
		ActivityFacility facility2 = factory.createActivityFacility(Id.create(2, ActivityFacility.class), new Coord((double) 300, (double) 4000));

		Assertions.assertEquals(0, facilities.getFacilities().size());

		facilities.addActivityFacility(facility1);
		facilities.addActivityFacility(facility2);
		Assertions.assertEquals(2, facilities.getFacilities().size());

		try {
			facilities.addActivityFacility(facility1);
			Assertions.fail("Expected exception, got none.");
		} catch (IllegalArgumentException expected) {}

		Assertions.assertEquals(2, facilities.getFacilities().size());
	}

	@Test
	void testAddActivityFacility_sameId() {
		ActivityFacilities facilities = new ActivityFacilitiesImpl();
		ActivityFacilitiesFactory factory = facilities.getFactory();
		ActivityFacility facility1 = factory.createActivityFacility(Id.create(1, ActivityFacility.class), new Coord((double) 200, (double) 5000));
		ActivityFacility facility2 = factory.createActivityFacility(Id.create(1, ActivityFacility.class), new Coord((double) 300, (double) 4000));

		Assertions.assertEquals(0, facilities.getFacilities().size());

		facilities.addActivityFacility(facility1);
		try {
			facilities.addActivityFacility(facility2);
			Assertions.fail("Expected exception, got none.");
		} catch (IllegalArgumentException expected) {}

        Assertions.assertEquals(1, facilities.getFacilities().size());
		Assertions.assertEquals(facility1, facilities.getFacilities().get(Id.create(1, ActivityFacility.class)));
	}

	/**
	 * Yes, it's just a remove on a Map. But we don't know what kind of map
	 * is used internally, and if the map is modifiable at all...
	 */
	@Test
	void testRemove() {
		ActivityFacilities facilities = new ActivityFacilitiesImpl();
		ActivityFacilitiesFactory factory = facilities.getFactory();
		ActivityFacility facility1 = factory.createActivityFacility(Id.create(1, ActivityFacility.class), new Coord((double) 200, (double) 5000));
		ActivityFacility facility2 = factory.createActivityFacility(Id.create(2, ActivityFacility.class), new Coord((double) 300, (double) 4000));
		facilities.addActivityFacility(facility1);
		facilities.addActivityFacility(facility2);
		Assertions.assertEquals(2, facilities.getFacilities().size());

		Assertions.assertEquals(facility1, facilities.getFacilities().remove(Id.create(1, ActivityFacility.class)));
		Assertions.assertEquals(1, facilities.getFacilities().size());
	}

}
