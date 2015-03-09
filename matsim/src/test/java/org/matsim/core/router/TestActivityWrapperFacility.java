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
package org.matsim.core.router;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.facilities.Facility;

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
				Id.create( "bouh", Link.class ));
		activities.add( act );

		act = new ActivityImpl(
				"another_type",
				new CoordImpl( 5 , 2 ),
				Id.create( "an_id", Link.class ));
		activities.add( act );

		act = new ActivityImpl(
				"h2g2",
				new CoordImpl( 42 , 42 ),
				Id.create( "42", Link.class ));
		activities.add( act );

		act = new ActivityImpl(
				"nothing",
				new CoordImpl( 0 , 0 ),
				Id.create( "0", Link.class ));
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

