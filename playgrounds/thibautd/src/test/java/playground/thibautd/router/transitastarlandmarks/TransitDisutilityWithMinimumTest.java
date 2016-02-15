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
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacility;
import org.matsim.pt.router.PreparedTransitSchedule;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.router.TransitRouterNetwork;
import org.matsim.testcases.MatsimTestUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * @author thibautd
 */
public class TransitDisutilityWithMinimumTest {
	@Rule
	public final MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void testMinimumLowerThanActual() {
		final Config config = utils.loadConfig( "test/scenarios/siouxfalls-2014-reduced/config_default.xml" );
		final Scenario sc = ScenarioUtils.loadScenario( config );

		final TransitRouterConfig routerConfig = new TransitRouterConfig( config );
		final TransitDisutilityWithMinimum testee =
				new TransitDisutilityWithMinimum(
						routerConfig,
						new PreparedTransitSchedule( sc.getTransitSchedule() ) );

		final Network network = TransitRouterNetwork.createFromSchedule(
				sc.getTransitSchedule(),
				routerConfig.getBeelineWalkConnectionDistance());
		final List<Id<Link>> linkIds = new ArrayList<>( network.getLinks().keySet() );
		Collections.sort( linkIds );

		final Random random = new Random( 20151210 );
		for ( int i = 0; i < 10000; i++ ) {
			final Link l = network.getLinks().get( linkIds.get( random.nextInt( linkIds.size() ) ) );
			final double time = random.nextDouble() * 24 * 3600;

			Assert.assertTrue(
					"minimum travel time greater than actual for "+l,
					testee.getLinkMinimumTravelTime( l ) <=
							testee.getLinkTravelTime( l , time , null , null ) );

			Assert.assertTrue(
					"minimum travel disutility greater than actual for "+l,
					testee.getLinkMinimumTravelDisutility( l ) <=
							testee.getLinkTravelDisutility( l , time , null , null ) );
		}
	}
}
