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
package playground.thibautd.router.transitastarlandmarks;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.config.Config;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.facilities.ActivityFacility;
import org.matsim.pt.router.TransitRouter;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.router.TransitRouterImpl;
import org.matsim.testcases.MatsimTestUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * @author thibautd
 */
public class TransitRouterAStarTest {
	@Rule
	public final MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void compareWithTransitRouterForSiouxfallsTest() {
		final Config config = utils.loadConfig( "test/scenarios/siouxfalls-2014-reduced/config_default.xml" );
		final Scenario sc = ScenarioUtils.loadScenario( config );

		final TransitRouterConfig transitConfig = new TransitRouterConfig( config );

		final TransitRouterAStar testee = new TransitRouterAStar( transitConfig , sc.getTransitSchedule() );
		final TransitRouter reference = new TransitRouterImpl( transitConfig , sc.getTransitSchedule() );

		final List<Id<ActivityFacility>> facilityIds = new ArrayList<>( sc.getActivityFacilities().getFacilities().keySet() );
		Collections.sort( facilityIds );

		final Random random = new Random( 20151210 );
		for ( int i = 0; i < 100; i++ ) {
			final ActivityFacility orign =
					sc.getActivityFacilities().getFacilities().get(
							facilityIds.get(
									random.nextInt( facilityIds.size() ) ) );

			final ActivityFacility destination =
					sc.getActivityFacilities().getFacilities().get(
							facilityIds.get(
									random.nextInt( facilityIds.size() ) ) );

			final double time = random.nextDouble() * 24 * 3600;
			final List<Leg> testedTrip =
					testee.calcRoute( orign.getCoord(), destination.getCoord(), time, null );
			final List<Leg> referenceTrip =
					reference.calcRoute( orign.getCoord(), destination.getCoord(), time, null );

			assertEquals(
					"trip from "+orign.getId()+" to "+destination.getId()+" at "+ Time.writeTime( time )
						+"\n tested trip: "+testedTrip
						+"\n reference trip: "+referenceTrip,
					testedTrip,
					referenceTrip );
		}
	}

	private void assertEquals(
			final String description,
			final List<Leg> testedTrip,
			final List<Leg> referenceTrip ) {
		Assert.assertEquals(
				"wrong number of elements for "+description,
				testedTrip.size(),
				referenceTrip.size() );

		for ( int i=0; i < testedTrip.size(); i++ ) {
			Assert.assertEquals(
					"unexpected mode for leg "+i+"/"+testedTrip.size()+" of "+description,
					testedTrip.get( i ).getMode(),
					referenceTrip.get( i ).getMode() );

			Assert.assertEquals(
					"unexpected duration for leg "+i+"/"+testedTrip.size()+" of "+description,
					testedTrip.get( i ).getTravelTime(),
					referenceTrip.get( i ).getTravelTime(),
					1E-9 );

			final Route testedRoute = testedTrip.get( i ).getRoute();
			final Route referenceRoute = testedTrip.get( i ).getRoute();

			if ( testedRoute == null ) {
				Assert.assertNull(
						"got null route for leg "+i+"/"+testedTrip.size()+" of "+description,
						referenceRoute );
				continue;
			}

			Assert.assertEquals(
					"unexpected route type for leg "+i+"/"+testedTrip.size()+" of "+description,
					testedRoute.getClass(),
					referenceRoute.getClass() );

			Assert.assertEquals(
					"unexpected origin link for leg "+i+"/"+testedTrip.size()+" of "+description,
					testedRoute.getStartLinkId(),
					referenceRoute.getStartLinkId() );

			Assert.assertEquals(
					"unexpected destination link for leg "+i+"/"+testedTrip.size()+" of "+description,
					testedRoute.getEndLinkId(),
					referenceRoute.getEndLinkId() );
		}
	}
}
