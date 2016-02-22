/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.ModeRouteFactory;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeAndDisutility;
import org.matsim.core.router.costcalculators.RandomizingTimeDistanceTravelDisutility;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.facilities.Facility;

public class NetworkRoutingModuleTest {

	@Test
	public void testRouteLeg() {
		Fixture f = new Fixture();
		ModeRouteFactory routeFactory = ((PopulationFactoryImpl) f.s.getPopulation().getFactory()).getModeRouteFactory();
		FreespeedTravelTimeAndDisutility freespeed = new FreespeedTravelTimeAndDisutility(-6.0/3600, +6.0/3600, 0.0);
		LeastCostPathCalculator routeAlgo = new Dijkstra(f.s.getNetwork(), freespeed, freespeed);

		Person person = PopulationUtils.createPerson(Id.create(1, Person.class));
		Activity fromAct = new ActivityImpl("h", new Coord((double) 0, (double) 0));
		((ActivityImpl) fromAct).setLinkId(Id.create("1", Link.class));
		Activity toAct = new ActivityImpl("h", new Coord((double) 0, (double) 3000));
		((ActivityImpl) toAct).setLinkId(Id.create("3", Link.class));

		final NetworkRoutingModule routingModule = new NetworkRoutingModule(
		            TransportMode.car,
		            f.s.getPopulation().getFactory(),
		            f.s.getNetwork(),
		            routeAlgo,
		            routeFactory);
		Facility fromFacility = new ActivityWrapperFacility( fromAct ) ;
		Facility toFacility = new ActivityWrapperFacility( toAct ) ;
		List<? extends PlanElement> result = routingModule.calcRoute(fromFacility, toFacility, 7.0*3600, person) ;
		Assert.assertEquals(1, result.size() );
		Leg leg = (Leg)result.get(0) ;
		Assert.assertEquals(100.0, leg.getTravelTime(), 1e-8);
		Assert.assertTrue(leg.getRoute() instanceof NetworkRoute);
	}

	@Test
	public void testRouteLegWithDistance() {
		Fixture f = new Fixture();

		Person person = PopulationUtils.createPerson(Id.create(1, Person.class));
		Activity fromAct = new ActivityImpl("h", new Coord((double) 0, (double) 0));
		((ActivityImpl) fromAct).setLinkId(Id.create("1", Link.class));
		Activity toAct = new ActivityImpl("h", new Coord((double) 0, (double) 3000));
		((ActivityImpl) toAct).setLinkId(Id.create("3", Link.class));
		
		ModeRouteFactory routeFactory = ((PopulationFactoryImpl) f.s.getPopulation().getFactory()).getModeRouteFactory();

		TravelTime timeObject = TravelTimeCalculator.create(f.s.getNetwork(), f.s.getConfig().travelTimeCalculator()).getLinkTravelTimes() ;

		{
			TravelDisutility costObject = new RandomizingTimeDistanceTravelDisutility.Builder( TransportMode.car, f.s.getConfig().planCalcScore() ).createTravelDisutility(timeObject);

			LeastCostPathCalculator routeAlgo = new Dijkstra(f.s.getNetwork(), costObject, timeObject );

			NetworkRoutingModule router =
                    new NetworkRoutingModule(
							TransportMode.car,
							f.s.getPopulation().getFactory(),
							f.s.getNetwork(),
							routeAlgo,
							routeFactory) ;

			List<? extends PlanElement> results = router.calcRoute(new ActivityWrapperFacility(fromAct), new ActivityWrapperFacility(toAct), 8.*3600, person) ;
			Assert.assertEquals( 1, results.size() );
			Leg leg = (Leg) results.get(0) ;
			
			Assert.assertEquals(100.0, leg.getTravelTime(), 1e-8);
			Assert.assertTrue(leg.getRoute() instanceof NetworkRoute);

			NetworkRoute route = (NetworkRoute) leg.getRoute() ;
			Assert.assertEquals(0.3333333333, route.getTravelCost(), 1e-8 ) ;
		}
		// and now with a monetary distance rate different from zero:
		
		{
			double monetaryDistanceRateCar = -1.;
			f.s.getConfig().planCalcScore().getModes().get(TransportMode.car).setMonetaryDistanceRate(monetaryDistanceRateCar);

			TravelDisutility costObject = new RandomizingTimeDistanceTravelDisutility.Builder( TransportMode.car, f.s.getConfig().planCalcScore() ).createTravelDisutility(timeObject);

			LeastCostPathCalculator routeAlgo = new Dijkstra(f.s.getNetwork(), costObject, timeObject );

			NetworkRoutingModule router =
					new NetworkRoutingModule(
							TransportMode.car,
							f.s.getPopulation().getFactory(),
							f.s.getNetwork(),
							routeAlgo,
							routeFactory) ;

			List<? extends PlanElement> result = router.calcRoute(new ActivityWrapperFacility(fromAct), new ActivityWrapperFacility(toAct), 7.*3600, person ) ;
			
			Assert.assertEquals( 1, result.size() ) ; 
			Leg leg = (Leg) result.get(0) ;
			
			Assert.assertEquals(100.0, leg.getTravelTime(), 1e-8);
			Assert.assertTrue(leg.getRoute() instanceof NetworkRoute);

			NetworkRoute route = (NetworkRoute) leg.getRoute() ;
			Assert.assertEquals(1000.3333333333, route.getTravelCost(), 1e-8 ) ;
		}
	}

	private static class Fixture {
		public final Scenario s = ScenarioUtils.createScenario(ConfigUtils.createConfig());

		public Fixture() {
			Network net = this.s.getNetwork();
			NetworkFactory nf = net.getFactory();
			Node n1 = nf.createNode(Id.create("1", Node.class), new Coord((double) 0, (double) 0));
			Node n2 = nf.createNode(Id.create("2", Node.class), new Coord((double) 0, (double) 1000));
			Node n3 = nf.createNode(Id.create("3", Node.class), new Coord((double) 0, (double) 2000));
			Node n4 = nf.createNode(Id.create("4", Node.class), new Coord((double) 0, (double) 3000));
			net.addNode(n1);
			net.addNode(n2);
			net.addNode(n3);
			net.addNode(n4);
			Link l1 = nf.createLink(Id.create("1", Link.class), n1, n2);
			Link l2 = nf.createLink(Id.create("2", Link.class), n2, n3);
			Link l3 = nf.createLink(Id.create("3", Link.class), n3, n4);
			l1.setFreespeed(10.0);
			l2.setFreespeed(10.0);
			l3.setFreespeed(10.0);
			l1.setLength(1000.0);
			l2.setLength(1000.0);
			l3.setLength(1000.0);
			net.addLink(l1);
			net.addLink(l2);
			net.addLink(l3);
		}
	}
}
