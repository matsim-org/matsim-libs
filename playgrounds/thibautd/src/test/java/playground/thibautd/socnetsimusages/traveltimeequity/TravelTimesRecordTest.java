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
package playground.thibautd.socnetsimusages.traveltimeequity;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.core.utils.misc.MatsimTestUtils;
import org.matsim.facilities.ActivityFacility;

/**
 * @author thibautd
 */
public class TravelTimesRecordTest {

	@Test
	public void testTravelTimeSearch() {
		// TODO test with stages
		// TODO separate in several tests
		final TravelTimesRecord testee = new TravelTimesRecord( new StageActivityTypesImpl( "stage" ) );

		final Id<Person> id = Id.createPersonId( "Albator" );
		final Id<Link> link = Id.createLinkId("Random Planet");
		final String mode = "Atlantis";
		final Id<ActivityFacility> facility = Id.create( "Random Building" , ActivityFacility.class );
		final String activityType = "Save the Galaxy";

		testee.handleEvent(
				new PersonDepartureEvent(
						1d,
						id,
						link,
						mode ) );
		testee.handleEvent(
				new ActivityStartEvent(
						2d,
						id,
						link,
						facility,
						activityType ) );

		Assert.assertEquals(
				"Trip before unproperly found",
				1d,
				testee.getTravelTimeBefore(id, 2.1),
				MatsimTestUtils.EPSILON );

		Assert.assertFalse(
				"Should not find subsequent time!",
				testee.alreadyKnowsTravelTimeAfter( id , 2.1 ) );

		testee.handleEvent(
				new PersonDepartureEvent(
						3d,
						id,
						link,
						mode ) );
		testee.handleEvent(
				new ActivityStartEvent(
						4d,
						id,
						link,
						facility,
						activityType ) );

		Assert.assertTrue(
				"Should find subsequent time!",
				testee.alreadyKnowsTravelTimeAfter( id , 2.1 ) );

		Assert.assertEquals(
				"Trip after unproperly found",
				1d,
				testee.getTravelTimeAfter(id, 2.1),
				MatsimTestUtils.EPSILON );
	}
}
