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

package org.matsim.core.facilities;

import org.junit.Test;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.api.experimental.facilities.ActivityFacilitiesFactory;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.geometry.CoordImpl;

/**
 * @author mrieser / Senozon AG
 */
public class FacilitiesAPITest {

	/**
	 * A simple test to see how well the API can be used from a user's perspective.
	 */
	@Test
	public void testAPI() {
		ActivityFacilities facilities = new ActivityFacilitiesImpl();
		ActivityFacilitiesFactory factory = facilities.getFactory();
		ActivityFacility facility1 = factory.createActivityFacility(new IdImpl(1), new CoordImpl(200, 5000));
		facilities.addActivityFacility(facility1);

		ActivityOption ao = factory.createActivityOption("shop");
//		ao.addOpeningTime(openingTime);
	}

}
