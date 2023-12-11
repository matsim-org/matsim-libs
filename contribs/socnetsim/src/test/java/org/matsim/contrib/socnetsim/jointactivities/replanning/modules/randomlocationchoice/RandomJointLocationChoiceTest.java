/* *********************************************************************** *
 * project: org.matsim.*
 * RandomJointLocationChoiceTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package org.matsim.contrib.socnetsim.jointactivities.replanning.modules.randomlocationchoice;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.core.population.PopulationUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacilitiesImpl;
import org.matsim.facilities.ActivityFacility;
import org.matsim.testcases.MatsimTestUtils;

import org.matsim.contrib.socnetsim.framework.population.SocialNetworkImpl;

/**
 * @author thibautd
 */
public class RandomJointLocationChoiceTest {
	private final Random random = new Random(1234);

	// /////////////////////////////////////////////////////////////////////////
	// tests of individual critical methods
	// /////////////////////////////////////////////////////////////////////////
	@Test
	void testBarycenterCalculation() {
		final ActivityFacilities facilities = new ActivityFacilitiesImpl();
		final List<Activity> activities = new ArrayList<Activity>();

		addActivityAndFacility(
				activities,
				facilities,
				"type",
				Id.create( random.nextLong() , ActivityFacility.class ),
				new Coord((double) 0, (double) 0));
		addActivityAndFacility(
				activities,
				facilities,
				"type",
				Id.create( random.nextLong() , ActivityFacility.class ),
				new Coord((double) 1, (double) 0));
		addActivityAndFacility(
				activities,
				facilities,
				"type",
				Id.create( random.nextLong() , ActivityFacility.class ),
				new Coord((double) 0, (double) 1));
		addActivityAndFacility(
				activities,
				facilities,
				"type",
				Id.create( random.nextLong() , ActivityFacility.class ),
				new Coord((double) 1, (double) 1));

		final RandomJointLocationChoiceConfigGroup config =
			new RandomJointLocationChoiceConfigGroup();
		config.setTypes( Collections.singleton( "type" ) );
		final Coord bar = new RandomJointLocationChoiceAlgorithm(
				config,
				facilities,
				new SocialNetworkImpl() ).calcBarycenterCoord( activities );

		assertEquals(
				"wrong barycenter",
				new Coord(0.5, 0.5),
				bar );
	}

	private static void addActivityAndFacility(
			final List<Activity> activities,
			final ActivityFacilities facilities,
			final String type,
			final Id<ActivityFacility> facilityId,
			final Coord coord) {
		final Activity act = PopulationUtils.createActivityFromCoord(type, coord);

		act.setFacilityId( facilityId );
		activities.add( act );

		final ActivityFacility facility = facilities.getFactory().createActivityFacility( facilityId , coord );
		facility.addActivityOption( facilities.getFactory().createActivityOption( type ) );
		facilities.addActivityFacility( facility );
	}

	@Test
	void testFacilityRetrieval() {
		final ActivityFacilities facilities = new ActivityFacilitiesImpl();
		final List<Activity> activities = new ArrayList<Activity>();


		final Id<ActivityFacility> sw = Id.create( "sw" , ActivityFacility.class );
		final double x1 = -1;
		final double y1 = -1;
		addActivityAndFacility(
				activities,
				facilities,
				"type",
				sw,
				new Coord(x1, y1));

		final Id<ActivityFacility> se = Id.create( "se" , ActivityFacility.class );
		final double y = -1;
		addActivityAndFacility(
				activities,
				facilities,
				"type",
				se,
				new Coord((double) 1, y));

		final Id<ActivityFacility> nw = Id.create( "nw" , ActivityFacility.class );
		final double x = -1;
		addActivityAndFacility(
				activities,
				facilities,
				"type",
				nw,
				new Coord(x, (double) 1));

		final Id<ActivityFacility> ne = Id.create( "ne" , ActivityFacility.class );
		addActivityAndFacility(
				activities,
				facilities,
				"type",
				ne,
				new Coord((double) 1, (double) 1));

		final Id<ActivityFacility> other_ne = Id.create( "other_ne" , ActivityFacility.class );
		addActivityAndFacility(
				activities,
				facilities,
				"other_type",
				other_ne,
				new Coord((double) 2, (double) 2));

		final Coord center = new Coord((double) 0, (double) 0);

		final RandomJointLocationChoiceConfigGroup config =
			new RandomJointLocationChoiceConfigGroup();
		config.setTypes( Collections.singleton( "type" ) );
		final RandomJointLocationChoiceAlgorithm algo =
			new RandomJointLocationChoiceAlgorithm(
				config,
				facilities,
				new SocialNetworkImpl() );

		for ( FacilityFixture f : new FacilityFixture[]{
				// go 1 in the direction of each facility
				new FacilityFixture(
					Math.PI / 4d,
					1d,
					ne ),
				new FacilityFixture(
					3 * Math.PI / 4d,
					1d,
					nw ),
				new FacilityFixture(
					5 * Math.PI / 4d,
					1d,
					sw ),
				new FacilityFixture(
					7 * Math.PI / 4d,
					1d,
					se ),
				// negative distance
				new FacilityFixture(
					Math.PI / 4d,
					-1d,
					sw ),
				new FacilityFixture(
					3 * Math.PI / 4d,
					-1d,
					se ),
				new FacilityFixture(
					5 * Math.PI / 4d,
					-1d,
					ne ),
				new FacilityFixture(
					7 * Math.PI / 4d,
					-1d,
					nw ),
				// go close to unelectable facility (wrong type), to check if it is ignored
				new FacilityFixture(
					Math.PI / 4d,
					Math.sqrt( 8 ),
					ne ),
				} ) {
			final ActivityFacility fac =
				algo.getFacility(
					"type",
					center,
					f.angle,
					f.distance );

			Assertions.assertEquals(
					f.expectedFacility,
					fac.getId(),
					"wrong facility for fixture "+f );
		}

	}

	private static class FacilityFixture {
		public double angle;
		public double distance;
		public Id<ActivityFacility> expectedFacility;
		
		public FacilityFixture(
				final double angle,
				final double distance,
				final Id expectedFacility) {
			this.angle = angle;
			this.distance = distance;
			this.expectedFacility = expectedFacility;
		}

		@Override
		public String toString() {
			return "[angle="+( angle / Math.PI )+" * PI; distance="+distance+"]";
		}
	}

	private static void assertEquals(
			final String msg,
			final Coord expected,
			final Coord actual ) {
		if ( Math.abs( expected.getX() - actual.getX() ) > MatsimTestUtils.EPSILON ||
				Math.abs( expected.getY() - actual.getY() ) > MatsimTestUtils.EPSILON ) {
			throw new AssertionError( msg+": expected <"+expected+">, got <"+actual+">" );
		}
	}
}

