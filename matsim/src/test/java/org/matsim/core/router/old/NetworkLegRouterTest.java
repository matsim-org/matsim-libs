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

package org.matsim.core.router.old;

import org.junit.Assert;
import org.junit.Test;
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
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.population.routes.ModeRouteFactory;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeAndDisutility;
import org.matsim.core.router.costcalculators.RandomizingTimeDistanceTravelDisutility;
import org.matsim.core.router.old.NetworkLegRouter;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.TravelTimeCalculatorFactory;
import org.matsim.core.trafficmonitoring.TravelTimeCalculatorFactoryImpl;
import org.matsim.core.utils.geometry.CoordImpl;

public class NetworkLegRouterTest {

	@Test
	public void testRouteLeg() {
		Fixture f = new Fixture();
		ModeRouteFactory routeFactory = ((PopulationFactoryImpl) f.s.getPopulation().getFactory()).getModeRouteFactory();
		FreespeedTravelTimeAndDisutility freespeed = new FreespeedTravelTimeAndDisutility(-6.0/3600, +6.0/3600, 0.0);
		LeastCostPathCalculator routeAlgo = new Dijkstra(f.s.getNetwork(), freespeed, freespeed);

		Person person = new PersonImpl(Id.create(1, Person.class));
		Leg leg = new LegImpl(TransportMode.car);
		Activity fromAct = new ActivityImpl("h", new CoordImpl(0, 0));
		((ActivityImpl) fromAct).setLinkId(Id.create("1", Link.class));
		Activity toAct = new ActivityImpl("h", new CoordImpl(0, 3000));
		((ActivityImpl) toAct).setLinkId(Id.create("3", Link.class));

		double tt = new NetworkLegRouter(f.s.getNetwork(), routeAlgo, routeFactory).routeLeg(person, leg, fromAct, toAct, 7.0*3600);
		Assert.assertEquals(100.0, tt, 1e-8);
		Assert.assertEquals(100.0, leg.getTravelTime(), 1e-8);
		Assert.assertTrue(leg.getRoute() instanceof NetworkRoute);
	}

	@Test
	public void testRouteLegWithDistance() {
		Fixture f = new Fixture();

		Person person = new PersonImpl(Id.create(1, Person.class));
		Leg leg = new LegImpl(TransportMode.car);
		Activity fromAct = new ActivityImpl("h", new CoordImpl(0, 0));
		((ActivityImpl) fromAct).setLinkId(Id.create("1", Link.class));
		Activity toAct = new ActivityImpl("h", new CoordImpl(0, 3000));
		((ActivityImpl) toAct).setLinkId(Id.create("3", Link.class));
		
		ModeRouteFactory routeFactory = ((PopulationFactoryImpl) f.s.getPopulation().getFactory()).getModeRouteFactory();

		TravelTimeCalculatorFactory ttCalcFactory = new TravelTimeCalculatorFactoryImpl()  ;
		TravelTime timeObject = ttCalcFactory.createTravelTimeCalculator(f.s.getNetwork(), f.s.getConfig().travelTimeCalculator()).getLinkTravelTimes() ;

		{
			TravelDisutility costObject = new RandomizingTimeDistanceTravelDisutility(timeObject, f.s.getConfig().planCalcScore() ) ;

			LeastCostPathCalculator routeAlgo = new Dijkstra(f.s.getNetwork(), costObject, timeObject );

			NetworkLegRouter router = new NetworkLegRouter(f.s.getNetwork(), routeAlgo, routeFactory) ;

			double tt = router.routeLeg(person, leg, fromAct, toAct, 7.0*3600);
			Assert.assertEquals(100.0, tt, 1e-8);
			Assert.assertEquals(100.0, leg.getTravelTime(), 1e-8);
			Assert.assertTrue(leg.getRoute() instanceof NetworkRoute);

			NetworkRoute route = (NetworkRoute) leg.getRoute() ;
			Assert.assertEquals(0.3333333333, route.getTravelCost(), 1e-8 ) ;
		}
		// and now with a monetary distance rate different from zero:
		
		{
			f.s.getConfig().planCalcScore().setMonetaryDistanceCostRateCar(-1.) ;
			// yyyyyy the above should be positive
			
			TravelDisutility costObject = new RandomizingTimeDistanceTravelDisutility(timeObject, f.s.getConfig().planCalcScore() ) ;

			LeastCostPathCalculator routeAlgo = new Dijkstra(f.s.getNetwork(), costObject, timeObject );

			NetworkLegRouter router = new NetworkLegRouter(f.s.getNetwork(), routeAlgo, routeFactory) ;

			double tt = router.routeLeg(person, leg, fromAct, toAct, 7.0*3600);
			Assert.assertEquals(100.0, tt, 1e-8);
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
			Node n1 = nf.createNode(Id.create("1", Node.class), this.s.createCoord(0, 0));
			Node n2 = nf.createNode(Id.create("2", Node.class), this.s.createCoord(0, 1000));
			Node n3 = nf.createNode(Id.create("3", Node.class), this.s.createCoord(0, 2000));
			Node n4 = nf.createNode(Id.create("4", Node.class), this.s.createCoord(0, 3000));
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
