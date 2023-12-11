/* *********************************************************************** *
 * project: org.matsim.*
 * PlanRouterWithVehicleRessourcesTest.java
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
package org.matsim.contrib.socnetsim.sharedvehicles;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.router.PlanRouter;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.RoutingRequest;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.Trip;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.timing.TimeInterpretation;
import org.matsim.facilities.Facility;
import org.matsim.vehicles.Vehicle;

/**
 * @author thibautd
 */
public class PlanRouterWithVehicleRessourcesTest {

	@Test
	void testVehicleIdsAreKeptIfSomething() throws Exception {
		final Config config = ConfigUtils.createConfig();
		final PopulationFactory factory = ScenarioUtils.createScenario(config).getPopulation().getFactory();

		final Id<Link> linkId = Id.create( "the_link" , Link.class );
		final Id<Person> personId = Id.create( "somebody" , Person.class );
		final Id<Vehicle> vehicleId = Id.create( "stolen_car" , Vehicle.class );
		final Person person = factory.createPerson( personId );
		final Plan plan = factory.createPlan();
		person.addPlan( plan );
		plan.setPerson( person );

		final Activity firstAct = factory.createActivityFromLinkId( "h" , linkId );
		plan.addActivity( firstAct );
		firstAct.setEndTime( 223 );

		final Leg leg = factory.createLeg( TransportMode.car );
		leg.setTravelTime(0.0);
		TripStructureUtils.setRoutingMode( leg, leg.getMode() );
		plan.addLeg( leg );
		final NetworkRoute route = RouteUtils.createLinkNetworkRouteImpl(linkId, Collections.<Id<Link>>emptyList(), linkId);
		route.setVehicleId( vehicleId );
		leg.setRoute( route );

		plan.addActivity( factory.createActivityFromLinkId( "h" , linkId ) );

		final TripRouter tripRouter = createTripRouter( factory, config);

		final PlanRouterWithVehicleRessources router =
			new PlanRouterWithVehicleRessources(
				new PlanRouter( tripRouter, TimeInterpretation.create(config) ) );

		router.run( plan );

		for ( Trip trip : TripStructureUtils.getTrips( plan ) ) {
			for (Leg l : trip.getLegsOnly()) {
				assertEquals(
					vehicleId,
					((NetworkRoute) l.getRoute()).getVehicleId(),
					"unexpected vehicle id");
			}
		}
	}

	private static TripRouter createTripRouter(final PopulationFactory factory, Config config) {
		// create some stages to check the behavior with that
		final String stage = "realize that actually, you did't forget to close the window, and go again interaction";
		final TripRouter.Builder builder = new TripRouter.Builder(config) ;
		builder.setRoutingModule(
				TransportMode.car,
				new RoutingModule() {
					@Override
					public List<? extends PlanElement> calcRoute(RoutingRequest request) {
						final Facility fromFacility = request.getFromFacility();
						final Facility toFacility = request.getToFacility();
						
						final List<PlanElement> legs = new ArrayList<PlanElement>();

						for (int i=0; i < 5; i++) {
							final Leg l = factory.createLeg( TransportMode.car );	
							l.setTravelTime(0.0);
							l.setRoute( RouteUtils.createLinkNetworkRouteImpl(fromFacility.getLinkId(), Collections.<Id<Link>>emptyList(),
									fromFacility.getLinkId()) );
							legs.add( l );
							legs.add( factory.createActivityFromLinkId( stage , fromFacility.getLinkId() ) );
							((Activity) legs.get(legs.size() - 1)).setMaximumDuration(0.0);
						}

						final Leg l = factory.createLeg( TransportMode.car );	
						l.setTravelTime(0.0);
						l.setRoute( RouteUtils.createLinkNetworkRouteImpl(fromFacility.getLinkId(), Collections.<Id<Link>>emptyList(), toFacility.getLinkId()) );
						legs.add( l );

						return legs ;
					}

				});
		return builder.build() ;
	}
}

