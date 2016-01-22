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
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.config.Config;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.facilities.ActivityFacility;
import org.matsim.pt.router.MultiNodeDijkstra;
import org.matsim.pt.router.TransitRouter;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.router.TransitRouterImpl;
import org.matsim.testcases.MatsimTestUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * @author thibautd
 */
public class TransitRouterAStarTest {
	@Rule
	public final MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void compareAlgorithmsTest() {
		final Config config = utils.loadConfig( "test/scenarios/siouxfalls-2014-reduced/config_default.xml" );
		final Scenario sc = ScenarioUtils.loadScenario( config );

		final TransitRouterAStar router = new TransitRouterAStar( config , sc.getTransitSchedule() );

		final MultiNodeAStarLandmarks testee = router.getAStarAlgorithm();
		final MultiNodeDijkstra oracle = router.getEquivalentDijkstra();

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

			final Iterable<MultiNodeAStarLandmarks.InitialNode> from =
					router.locateWrappedNearestTransitNodes(
							null,
							orign.getCoord(),
							time );
			final Iterable<MultiNodeAStarLandmarks.InitialNode> to =
					router.locateWrappedNearestTransitNodes(
							null,
							destination.getCoord(),
							time );

			final LeastCostPathCalculator.Path testedPath = testee.calcLeastCostPath( from , to , null );
			final LeastCostPathCalculator.Path referencePath = oracle.calcLeastCostPath( toMap( from ) , toMap( to ) , null );

			if ( testedPath ==  null ) {
				Assert.assertNull(
						"could not find existing path",
						referencePath );
			}

			final double referencePathCost =
					referencePath.travelCost +
							toMap( from ).get( referencePath.nodes.get(0)).initialCost +
							toMap( to ).get( referencePath.nodes.get( referencePath.nodes.size() - 1)).initialCost;
			final double actualPathCost =
					testedPath.travelCost +
							toMap( from ).get( testedPath.nodes.get(0)).initialCost +
							toMap( to ).get( testedPath.nodes.get( testedPath.nodes.size() - 1)).initialCost;

			Assert.assertEquals(
					"unexpected path cost for "
							+"trip from "+orign.getId()+" to "+destination.getId()+" at "+ Time.writeTime( time )
							+"\n tested trip: "+testedPath
							+"\n reference trip: "+referencePath,
					referencePathCost,
					actualPathCost,
					1E-9 );
		}
	}

	@Test
	@Ignore( "might be OK that it fails, as long as routes are equivalent from the cost perspective...")
	public void compareRoutersTest() {
		final Config config = utils.loadConfig( "test/scenarios/siouxfalls-2014-reduced/config_default.xml" );
		final Scenario sc = ScenarioUtils.loadScenario( config );

		final TransitRouterConfig transitConfig = new TransitRouterConfig( config );

		final TransitRouterAStar testee = new TransitRouterAStar( config , sc.getTransitSchedule() );
		final TransitRouter oracle = new TransitRouterImpl( transitConfig , sc.getTransitSchedule() );

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

			final List<Leg> testedTrip = testee.calcRoute( orign.getCoord() , destination.getCoord() , time , null );
			final List<Leg> referenceTrip = oracle.calcRoute( orign.getCoord() , destination.getCoord() , time , null );

			assertEquals(
					"trip from "+orign.getId()+" to "+destination.getId()+" at "+ Time.writeTime( time )
						+"\n tested trip: "+testedTrip
						+"\n reference trip: "+referenceTrip,
					testedTrip,
					referenceTrip );
		}
	}

	private Map<Node, MultiNodeDijkstra.InitialNode> toMap( Iterable<MultiNodeAStarLandmarks.InitialNode> nodes ) {
		final Map<Node, MultiNodeDijkstra.InitialNode> map = new HashMap<>();
		for ( MultiNodeAStarLandmarks.InitialNode n : nodes) {
			map.put( n.node , new MultiNodeDijkstra.InitialNode( n.initialCost , n.initialTime ) );
		}
		return map;
	}

	private void assertEquals(
			final String description,
			final List<Leg> testedTrip,
			final List<Leg> referenceTrip ) {
		Assert.assertEquals(
				"wrong travel time for "+description,
				calcTravelTime( referenceTrip ),
				calcTravelTime( testedTrip ),
				1E-9 );

		Assert.assertEquals(
				"wrong number of elements for "+description,
				referenceTrip.size(),
				testedTrip.size() );

		for ( int i=0; i < testedTrip.size(); i++ ) {
			Assert.assertEquals(
					"unexpected mode for leg "+i+"/"+testedTrip.size()+" of "+description,
					referenceTrip.get( i ).getMode(),
					testedTrip.get( i ).getMode() );

			Assert.assertEquals(
					"unexpected duration for leg "+i+"/"+testedTrip.size()+" of "+description,
					referenceTrip.get( i ).getTravelTime(),
					testedTrip.get( i ).getTravelTime(),
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
					referenceRoute.getClass(),
					testedRoute.getClass() );

			Assert.assertEquals(
					"unexpected origin link for leg "+i+"/"+testedTrip.size()+" of "+description,
					referenceRoute.getStartLinkId(),
					testedRoute.getStartLinkId() );

			Assert.assertEquals(
					"unexpected destination link for leg "+i+"/"+testedTrip.size()+" of "+description,
					referenceRoute.getEndLinkId(),
					testedRoute.getEndLinkId() );
		}
	}

	private double calcTravelTime( List<Leg> trip ) {
		double tt = 0;
		for ( Leg l : trip ) tt += l.getTravelTime();
		return tt;
	}
}
