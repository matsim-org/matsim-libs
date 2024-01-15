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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.core.population.PopulationUtils;
import org.matsim.facilities.FacilitiesUtils;
import org.matsim.facilities.Facility;

/**
 * @author thibautd
 */
public class TestActivityWrapperFacility {
	private List<Activity> activities;

	@BeforeEach
	public void init() {
		activities = new ArrayList<Activity>();

		Activity act = PopulationUtils.createActivityFromCoordAndLinkId("type", new Coord((double) 1, (double) 2), Id.create( "bouh", Link.class ));
		activities.add( act );

		act = PopulationUtils.createActivityFromCoordAndLinkId("another_type", new Coord((double) 5, (double) 2), Id.create( "an_id", Link.class ));
		activities.add( act );

		act = PopulationUtils.createActivityFromCoordAndLinkId("h2g2", new Coord((double) 42, (double) 42), Id.create( "42", Link.class ));
		activities.add( act );

		act = PopulationUtils.createActivityFromCoordAndLinkId("nothing", new Coord((double) 0, (double) 0), Id.create( "0", Link.class ));
		activities.add( act );

	}

	@Test
	void testWrapper() {
		for (Activity activity : activities) {
			Facility wrapper = FacilitiesUtils.toFacility( activity, null );

			Assertions.assertEquals(
					activity.getCoord(),
					wrapper.getCoord(),
					"wrapped activity returns incorrect coordinate!");

			Assertions.assertEquals(
					activity.getLinkId(),
					wrapper.getLinkId(),
					"wrapped activity returns incorrect link id!");
		}
	}
}

