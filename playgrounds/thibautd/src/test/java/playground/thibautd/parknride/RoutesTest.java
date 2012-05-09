/* *********************************************************************** *
 * project: org.matsim.*
 * RoutesTest.java
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
package playground.thibautd.parknride;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.api.experimental.facilities.Facility;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.routes.GenericRoute;
import org.matsim.core.population.routes.ModeRouteFactory;
import org.matsim.core.router.TeleportationLegRouter;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeAndDisutility;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.router.TransitRouterImpl;
import org.matsim.pt.router.TransitRouterNetworkTravelTimeAndDisutility;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.testcases.MatsimTestUtils;

import playground.thibautd.parknride.routingapproach.RoutingParkAndRideIncluder;
import playground.thibautd.parknride.routingapproach.ParkAndRideRoutingModule;
import playground.thibautd.parknride.routingapproach.ParkAndRideTravelTimeCost;
import playground.thibautd.parknride.routingapproach.ParkAndRideUtils;
import playground.thibautd.router.LegRouterWrapper;
import playground.thibautd.router.RoutingModule;
import playground.thibautd.router.TransitRouterWrapper;

/**
 * @author thibautd
 */
public class RoutesTest {
	@Rule
	public final MatsimTestUtils utils = new MatsimTestUtils();

	private static final double DEPARTURE = 0;
	private static final Person PERSON = new PersonImpl( new IdImpl( "Tintin" ) );

	private static final String CONFIG_FILE = "config.xml";
	private RoutingModule transitRouter;
	private ParkAndRideRoutingModule pnrRouter;
	private List<Tuple<Facility, Facility>> ods;
	private ParkAndRideFacilities facilities;

	@Before
	public void init() {
		Config config = ConfigUtils.createConfig();
		ParkAndRideUtils.setConfigGroup( config );
		ConfigUtils.loadConfig( config , utils.getPackageInputDirectory() + "/" + CONFIG_FILE );
		ScenarioImpl scenario = (ScenarioImpl) ParkAndRideUtils.loadScenario( config );

		Network network = scenario.getNetwork();
		TransitSchedule schedule = scenario.getTransitSchedule();
		facilities = ParkAndRideUtils.getParkAndRideFacilities( scenario );
		TransitRouterConfig transitConfig =
			new TransitRouterConfig( 
					config.planCalcScore(),
					config.plansCalcRoute(),
					config.transitRouter(),
					config.vspExperimental());

		ModeRouteFactory routeFactory = new ModeRouteFactory();
		PopulationFactory popFactory = scenario.getPopulation().getFactory();

		transitRouter =
			new TransitRouterWrapper(
					new TransitRouterImpl(
						transitConfig,
						schedule),
					schedule,
					new LegRouterWrapper(
						TransportMode.transit_walk,
						popFactory,
						new TeleportationLegRouter(
							routeFactory,
							config.plansCalcRoute().getWalkSpeed(),
							config.plansCalcRoute().getBeelineDistanceFactor()),
						null,
						null));

		FreespeedTravelTimeAndDisutility timeCost = new FreespeedTravelTimeAndDisutility( -1 , 1 , -1 );
		ParkAndRideTravelTimeCost pnrTimeCost = new ParkAndRideTravelTimeCost( transitConfig , config.planCalcScore() );
		pnrRouter =
			new ParkAndRideRoutingModule(
					routeFactory,
					popFactory,
					network,
					schedule,
					transitConfig.beelineWalkConnectionDistance,
					transitConfig.searchRadius,
					facilities,
					transitConfig,
					timeCost,
					timeCost,
					new TransitRouterNetworkTravelTimeAndDisutility( transitConfig ),
					pnrTimeCost,
					pnrTimeCost);

		ods = new ArrayList< Tuple<Facility, Facility> >();

		Collection<? extends Link> links = network.getLinks().values();
		for (Link or : links) {
			if (or.getAllowedModes().contains( TransportMode.car )) {
				Facility origin = new LinkFacility( or );
				for (Link de : links) {
					if (de.getAllowedModes().contains( TransportMode.car )) {
						Facility destination = new LinkFacility( de );
						ods.add( new Tuple< Facility , Facility >(
									origin,
									destination) );
					}
				}
			}
		}
	}

	@Test
	public void testPtTrip() throws Exception {
		int testCount = 0;
		int longCount = 0;
		for (Tuple<Facility, Facility> od : ods) {
			List<? extends PlanElement> pnrTrip =
				pnrRouter.calcRoute(
						od.getFirst(),
						od.getSecond(),
						DEPARTURE,
						PERSON);

			if (pnrTrip == null) {
				continue;
			}

			Facility pnrFacility =
				pnrTrip.size() > 1 ?
				RoutingParkAndRideIncluder.identifyPnrFacility(
						pnrTrip,
						facilities ) :
				od.getFirst();

			List<? extends PlanElement> pnrPtPortion =
				pnrTrip.size() > 1 ?
				pnrTrip.subList( 2 , pnrTrip.size() ) :
				pnrTrip;

			List<? extends PlanElement> ptTrip =
				transitRouter.calcRoute(
						pnrFacility,
						od.getSecond(),
						((Leg) pnrPtPortion.get( 0 )).getDepartureTime(),
						PERSON);

			compareTrips( pnrPtPortion , ptTrip );

			testCount++;
			if (pnrTrip.size() > 1) longCount++;
		}

		// this is the observed number of valid trips
		// used to test that the test actually performs something useful
		if (testCount != 2304 || longCount != 1748) {
			throw new RuntimeException( "only "+testCount+" trips were compared, "+longCount+" trips of more than one leg" );
		}
	}

	private static void compareTrips(
			final List<? extends PlanElement> pnrPtPortion,
			final List<? extends PlanElement> ptTrip) {
		String suffix = " in pnr="+pnrPtPortion+" and pt="+ptTrip;
		Assert.assertEquals(
				"trips have different size! "+suffix,
				ptTrip.size(),
				pnrPtPortion.size());

		Iterator<? extends PlanElement> ptIter = ptTrip.iterator();
		Iterator<? extends PlanElement> pnrIter = pnrPtPortion.iterator();

		while (ptIter.hasNext()) {
			PlanElement ptPe = ptIter.next();
			PlanElement pnrPe = pnrIter.next();

			if (ptPe instanceof Leg) {
				Assert.assertEquals(
						"different routes! "+suffix,
						new ComparableRoute( ((Leg) ptPe).getRoute() ),
						new ComparableRoute( ((Leg) pnrPe).getRoute()) );
			}
			else {
				Assert.assertEquals(
						"different coordinates for interaction!"+suffix,
						((Activity) ptPe).getCoord(),
						((Activity) pnrPe).getCoord());

				Assert.assertEquals(
						"different links for interaction!"+suffix,
						((Activity) ptPe).getLinkId(),
						((Activity) pnrPe).getLinkId());
			}
		}
	}
}

class LinkFacility implements Facility {
	private final Link link;

	public LinkFacility(final Link link) {
		this.link = link;
	}

	@Override
	public Coord getCoord() {
		return link.getCoord();
	}

	@Override
	public Id getId() {
		return link.getId();
	}

	@Override
	public Map<String, Object> getCustomAttributes() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Id getLinkId() {
		return link.getId();
	}
}

/**
 * wrapps routes to provide equals and toString methods: to use in test
 */
class ComparableRoute {
	private final String repr;

	public ComparableRoute(final Route route) {
		repr = route instanceof GenericRoute ?
			toString( (GenericRoute) route ) :
			route.toString();
	}

	private static String toString(final GenericRoute route) {
		return route.getClass()+", "+route.getRouteType()+" ("+route.getRouteDescription()+"): "+route.getStartLinkId()+" -> "+route.getEndLinkId()+" in "+(int) route.getTravelTime();//+" for "+route.getDistance();
	}

	public boolean equals(final Object other) {
		if (other instanceof ComparableRoute) {
			// all pertinent values are already used in the toString
			return toString().equals( other.toString() );
		}
		return false;
	}

	public String toString() {
		return repr;
	}
}
