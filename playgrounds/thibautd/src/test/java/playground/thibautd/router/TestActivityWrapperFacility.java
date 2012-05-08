/* *********************************************************************** *
 * project: org.matsim.*
 * TestActivityWrapperFacility.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.thibautd.router;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.core.api.experimental.facilities.Facility;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.utils.geometry.CoordImpl;

/**
 * @author thibautd
 */
public class TestActivityWrapperFacility {
	private List<Activity> activities;

	@Before
	public void init() {
		activities = new ArrayList<Activity>();

		Activity act = new ActivityImpl(
				"type",
				new CoordImpl( 1 , 2 ),
				new IdImpl( "bouh" ));
		activities.add( act );

		act = new ActivityImpl(
				"another_type",
				new CoordImpl( 5 , 2 ),
				new IdImpl( "an_id" ));
		activities.add( act );

		act = new ActivityImpl(
				"h2g2",
				new CoordImpl( 42 , 42 ),
				new IdImpl( "42" ));
		activities.add( act );

		act = new ActivityImpl(
				"nothing",
				new CoordImpl( 0 , 0 ),
				new IdImpl( "0" ));
		activities.add( act );

	}

	@Test
	public void testWrapper() {
		for (Activity activity : activities) {
			Facility wrapper = new ActivityWrapperFacility( activity );

			Assert.assertEquals(
					"wrapped activity returns incorrect coordinate!",
					activity.getCoord(),
					wrapper.getCoord());

			Assert.assertEquals(
					"wrapped activity returns incorrect link id!",
					activity.getLinkId(),
					wrapper.getLinkId());
		}
	}
}

