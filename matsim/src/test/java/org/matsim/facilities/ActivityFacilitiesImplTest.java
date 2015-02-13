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

import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.api.experimental.facilities.ActivityFacilitiesFactory;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.facilities.ActivityFacilitiesImpl;

/**
 * @author mrieser / Senozon AG
 */
public class ActivityFacilitiesImplTest {

	@Test
	public void testAddActivityFacility() {
		ActivityFacilities facilities = new ActivityFacilitiesImpl();
		ActivityFacilitiesFactory factory = facilities.getFactory();
		ActivityFacility facility1 = factory.createActivityFacility(Id.create(1, ActivityFacility.class), new CoordImpl(200, 5000));

		Assert.assertEquals(0, facilities.getFacilities().size());

		facilities.addActivityFacility(facility1);

		Assert.assertEquals(1, facilities.getFacilities().size());

		ActivityFacility facility2 = factory.createActivityFacility(Id.create(2, ActivityFacility.class), new CoordImpl(300, 4000));
		facilities.addActivityFacility(facility2);

		Assert.assertEquals(2, facilities.getFacilities().size());
	}

	@Test
	public void testAddActivityFacility_addingTwice() {
		ActivityFacilities facilities = new ActivityFacilitiesImpl();
		ActivityFacilitiesFactory factory = facilities.getFactory();
		ActivityFacility facility1 = factory.createActivityFacility(Id.create(1, ActivityFacility.class), new CoordImpl(200, 5000));
		ActivityFacility facility2 = factory.createActivityFacility(Id.create(2, ActivityFacility.class), new CoordImpl(300, 4000));

		Assert.assertEquals(0, facilities.getFacilities().size());

		facilities.addActivityFacility(facility1);
		facilities.addActivityFacility(facility2);
		Assert.assertEquals(2, facilities.getFacilities().size());

		try {
			facilities.addActivityFacility(facility1);
			Assert.fail("Expected exception, got none.");
		} catch (IllegalArgumentException expected) {}

		Assert.assertEquals(2, facilities.getFacilities().size());
	}

	@Test
	public void testAddActivityFacility_sameId() {
		ActivityFacilities facilities = new ActivityFacilitiesImpl();
		ActivityFacilitiesFactory factory = facilities.getFactory();
		ActivityFacility facility1 = factory.createActivityFacility(Id.create(1, ActivityFacility.class), new CoordImpl(200, 5000));
		ActivityFacility facility2 = factory.createActivityFacility(Id.create(1, ActivityFacility.class), new CoordImpl(300, 4000));

		Assert.assertEquals(0, facilities.getFacilities().size());

		facilities.addActivityFacility(facility1);
		try {
			facilities.addActivityFacility(facility2);
			Assert.fail("Expected exception, got none.");
		} catch (IllegalArgumentException expected) {}

        Assert.assertEquals(1, facilities.getFacilities().size());
		Assert.assertEquals(facility1, facilities.getFacilities().get(Id.create(1, ActivityFacility.class)));
	}

	/**
	 * Yes, it's just a remove on a Map. But we don't know what kind of map
	 * is used internally, and if the map is modifiable at all...
	 */
	@Test
	public void testRemove() {
		ActivityFacilities facilities = new ActivityFacilitiesImpl();
		ActivityFacilitiesFactory factory = facilities.getFactory();
		ActivityFacility facility1 = factory.createActivityFacility(Id.create(1, ActivityFacility.class), new CoordImpl(200, 5000));
		ActivityFacility facility2 = factory.createActivityFacility(Id.create(2, ActivityFacility.class), new CoordImpl(300, 4000));
		facilities.addActivityFacility(facility1);
		facilities.addActivityFacility(facility2);
		Assert.assertEquals(2, facilities.getFacilities().size());

		Assert.assertEquals(facility1, facilities.getFacilities().remove(Id.create(1, ActivityFacility.class)));
		Assert.assertEquals(1, facilities.getFacilities().size());
	}

}
