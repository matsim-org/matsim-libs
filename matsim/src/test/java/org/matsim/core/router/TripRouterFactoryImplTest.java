/* *********************************************************************** *
 * project: org.matsim.*
 * TripRouterFactoryImplTest.java
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

import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Injector;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeAndDisutility;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelDisutilityFactory;
import org.matsim.core.scenario.ScenarioByInstanceModule;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.Facility;

import javax.inject.Provider;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Tests the router returned by the default factory.
 * @author thibautd
 */
public class TripRouterFactoryImplTest {

	/**
	 * When not using PT but using a multimodal network, car should not be routed on links restricted to pt modes,
	 * such as railways.
	 */
	@Test
	public void testRestrictedNetworkNoPt() throws Exception {
		Config config = ConfigUtils.createConfig();
		config.transit().setUseTransit( false );

		testRestrictedNetwork( config );
	}

	private static void testRestrictedNetwork(final Config config) throws Exception {
		// create a simple scenario, with two parallel links,
		// a long one for cars, a short one for pt.
		final Scenario scenario = ScenarioUtils.createScenario( config );
		Network net = scenario.getNetwork();

		Node n1 = net.getFactory().createNode( Id.create( 1, Node.class ) , new Coord((double) 0, (double) 0));
		Node n2 = net.getFactory().createNode( Id.create( 2, Node.class ) , new Coord((double) 0, (double) 0));
		Node n3 = net.getFactory().createNode( Id.create( 3, Node.class ) , new Coord((double) 0, (double) 0));
		Node n4 = net.getFactory().createNode( Id.create( 4, Node.class ) , new Coord((double) 0, (double) 0));

		Link l1 = net.getFactory().createLink( Id.create( "l1", Link.class ) , n1 , n2 );
		Link l2c = net.getFactory().createLink( Id.create( "l2c", Link.class ) , n2 , n3 );
		Link l2pt = net.getFactory().createLink( Id.create( "l2pt", Link.class ) , n2 , n3 );
		Link l3 = net.getFactory().createLink( Id.create( "l3", Link.class ) , n3 , n4 );

		l2c.setAllowedModes( Collections.singleton( TransportMode.car ) );
		l2c.setLength( 1000 );
		l2pt.setAllowedModes( Collections.singleton( TransportMode.pt ) );
		l2pt.setLength( 10 );

		net.addNode( n1 );
		net.addNode( n2 );
		net.addNode( n3 );
		net.addNode( n4 );

		net.addLink( l1 );
		net.addLink( l2c );
		net.addLink( l2pt );
		net.addLink( l3 );

		com.google.inject.Injector injector = Injector.createInjector(scenario.getConfig(), new AbstractModule() {
			@Override
			public void install() {
				install(AbstractModule.override(Arrays.asList(new TripRouterModule()), new AbstractModule() {
					@Override
					public void install() {
						install(new ScenarioByInstanceModule(scenario));
						addTravelTimeBinding("car").toInstance(new FreespeedTravelTimeAndDisutility( config.planCalcScore() ));
						addTravelDisutilityFactoryBinding("car").toInstance(new OnlyTimeDependentTravelDisutilityFactory());
					}
				}));
			}
		});

		// create the factory, get a router, route.
		Provider<TripRouter> factory = injector.getProvider(TripRouter.class);

		TripRouter router = factory.get();

		List<? extends PlanElement> trip = router.calcRoute(
				TransportMode.car,
				new LinkFacility( l1 ),
				new LinkFacility( l3 ),
				0,
				PopulationUtils.createPerson(Id.create("toto", Person.class)));

		Leg l = (Leg) trip.get( 0 );
		if ( scenario.getConfig().plansCalcRoute().isInsertingAccessEgressWalk() ) {
			l = (Leg) trip.get(2) ;
		}

		// actual test
		NetworkRoute r = (NetworkRoute) l.getRoute();

		Assert.assertEquals(
				"unexpected route length "+r.getLinkIds(),
				1,
				r.getLinkIds().size() );

		Assert.assertEquals(
				"unexpected link",
				l2c.getId(),
				r.getLinkIds().get( 0 ));
	}

	/**
	 * Checks that routes are found when using a monomodal network (ie modes are not restricted)
	 */
	@Test
	public void testMonomodalNetwork() throws Exception {
		final Config config = ConfigUtils.createConfig();
		final Scenario scenario = ScenarioUtils.createScenario( config );
		Network net = scenario.getNetwork();

		Node n1 = net.getFactory().createNode( Id.create( 1, Node.class) , new Coord((double) 0, (double) 0));
		Node n2 = net.getFactory().createNode( Id.create( 2, Node.class) , new Coord((double) 0, (double) 0));
		Node n3 = net.getFactory().createNode( Id.create( 3, Node.class) , new Coord((double) 0, (double) 0));
		Node n4 = net.getFactory().createNode( Id.create( 4, Node.class) , new Coord((double) 0, (double) 0));

		Link l1 = net.getFactory().createLink( Id.create( "l1", Link.class ) , n1 , n2 );
		Link l2long = net.getFactory().createLink( Id.create( "l2long", Link.class ) , n2 , n3 );
		Link l2short = net.getFactory().createLink( Id.create( "l2short", Link.class ) , n2 , n3 );
		Link l3 = net.getFactory().createLink( Id.create( "l3", Link.class ) , n3 , n4 );

		l2long.setLength( 1000 );
		l2short.setLength( 10 );

		net.addNode( n1 );
		net.addNode( n2 );
		net.addNode( n3 );
		net.addNode( n4 );

		net.addLink( l1 );
		net.addLink( l2long );
		net.addLink( l2short );
		net.addLink( l3 );

		// create the factory, get a router, route.
		com.google.inject.Injector injector = Injector.createInjector(scenario.getConfig(), new AbstractModule() {
			@Override
			public void install() {
				install(new ScenarioByInstanceModule(scenario));
				install(AbstractModule.override(Arrays.asList(new TripRouterModule()), new AbstractModule() {
					@Override
					public void install() {
						addTravelTimeBinding("car").toInstance(new FreespeedTravelTimeAndDisutility( config.planCalcScore() ));
						addTravelDisutilityFactoryBinding("car").toInstance(new OnlyTimeDependentTravelDisutilityFactory());
					}
				}));
			}
		});

		TripRouter router = injector.getInstance(TripRouter.class);

		List<? extends PlanElement> trip = router.calcRoute(
				TransportMode.car,
				new LinkFacility( l1 ),
				new LinkFacility( l3 ),
				0,
				PopulationUtils.createPerson(Id.create("toto", Person.class)));

		Leg l = (Leg) trip.get( 0 );
		if ( scenario.getConfig().plansCalcRoute().isInsertingAccessEgressWalk() ) {
			l = (Leg) trip.get(2) ;
		}

		// actual test
		NetworkRoute r = (NetworkRoute) l.getRoute();

		Assert.assertEquals(
				"unexpected route length "+r.getLinkIds(),
				1,
				r.getLinkIds().size() );

		Assert.assertEquals(
				"unexpected link",
				l2short.getId(),
				r.getLinkIds().get( 0 ));
	}

	private static class LinkFacility implements Facility {
		private final Link l;

		public LinkFacility(final Link l) {
			this.l = l;
		}

		@Override
		public Coord getCoord() {
			return l.getCoord();
		}

		@Override
		public Id<Link> getId() {
			return l.getId();
		}

		@Override
		public Map<String, Object> getCustomAttributes() {
			return null;
		}

		@Override
		public Id<Link> getLinkId() {
			return l.getId();
		}
	}
}

