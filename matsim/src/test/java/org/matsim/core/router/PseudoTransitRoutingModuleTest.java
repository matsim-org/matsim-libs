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
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.population.routes.RouteFactoryImpl;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeAndDisutility;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.scenario.ScenarioUtils;

public class PseudoTransitRoutingModuleTest {

	@Test
	public void testRouteLeg() {
		Fixture f = new Fixture();
		RouteFactoryImpl routeFactory = ((PopulationFactoryImpl) f.s.getPopulation().getFactory()).getRouteFactory();
		FreespeedTravelTimeAndDisutility freespeed = new FreespeedTravelTimeAndDisutility(-6.0/3600, +6.0/3600, 0.0);
		LeastCostPathCalculator routeAlgo = new Dijkstra(f.s.getNetwork(), freespeed, freespeed);

		Person person = PopulationUtils.createPerson(Id.create(1, Person.class));
		Leg leg = new LegImpl(TransportMode.pt);
		Activity fromAct = new ActivityImpl("h", new Coord(0, 0));
		((ActivityImpl) fromAct).setLinkId(Id.create("1", Link.class));
		Activity toAct = new ActivityImpl("h", new Coord(0, 3000));
		((ActivityImpl) toAct).setLinkId(Id.create("3", Link.class));

		double tt = new PseudoTransitRoutingModule(
				"mode", f.s.getPopulation().getFactory(),
				f.s.getNetwork(), routeAlgo, 2.0, 1.0, routeFactory).routeLeg(person, leg, fromAct, toAct, 7.0*3600);
		Assert.assertEquals(400.0, tt, 1e-8);
		Assert.assertEquals(400.0, leg.getTravelTime(), 1e-8);
		Assert.assertTrue(leg.getRoute() instanceof GenericRouteImpl);
		Assert.assertEquals(3000.0, leg.getRoute().getDistance(), 1e-8);
		tt = new PseudoTransitRoutingModule(
				"mode", f.s.getPopulation().getFactory(),
				f.s.getNetwork(), routeAlgo, 3.0, 2.0, routeFactory).routeLeg(person, leg, fromAct, toAct, 7.0*3600);
		Assert.assertEquals(600.0, tt, 1e-8);
		Assert.assertEquals(600.0, leg.getTravelTime(), 1e-8);
		Assert.assertEquals(6000.0, leg.getRoute().getDistance(), 1e-8);
	}

	private static class Fixture {
		public final Scenario s = ScenarioUtils.createScenario(ConfigUtils.createConfig());

		public Fixture() {
			Network net = this.s.getNetwork();
			NetworkFactory nf = net.getFactory();
			Node n1 = nf.createNode(Id.create("1", Node.class), new Coord(0, 0));
			Node n2 = nf.createNode(Id.create("2", Node.class), new Coord(0, 1000));
			Node n3 = nf.createNode(Id.create("3", Node.class), new Coord(0, 2000));
			Node n4 = nf.createNode(Id.create("4", Node.class), new Coord(0, 3000));
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
