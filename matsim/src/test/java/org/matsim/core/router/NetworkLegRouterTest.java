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
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.LinkNetworkRoute;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.NetworkFactoryImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.utils.geometry.CoordImpl;

public class NetworkLegRouterTest {

	@Test
	public void testRouteLeg() {
		Fixture f = new Fixture();
		NetworkFactoryImpl routeFactory = ((NetworkImpl) f.s.getNetwork()).getFactory();
		FreespeedTravelTimeCost freespeed = new FreespeedTravelTimeCost(-6.0/3600, +6.0/3600, 0.0);
		LeastCostPathCalculator routeAlgo = new Dijkstra(f.s.getNetwork(), freespeed, freespeed);

		Person person = new PersonImpl(new IdImpl(1));
		Leg leg = new LegImpl(TransportMode.car);
		Activity fromAct = new ActivityImpl("h", new CoordImpl(0, 0));
		((ActivityImpl) fromAct).setLinkId(f.s.createId("1"));
		Activity toAct = new ActivityImpl("h", new CoordImpl(0, 3000));
		((ActivityImpl) toAct).setLinkId(f.s.createId("3"));

		double tt = new NetworkLegRouter(f.s.getNetwork(), routeAlgo, routeFactory).routeLeg(person, leg, fromAct, toAct, 7.0*3600);
		Assert.assertEquals(100.0, tt, 1e-8);
		Assert.assertEquals(100.0, leg.getTravelTime(), 1e-8);
		Assert.assertTrue(leg.getRoute() instanceof LinkNetworkRoute);
	}

	private static class Fixture {
		public final Scenario s = new ScenarioImpl();

		public Fixture() {
			Network net = this.s.getNetwork();
			NetworkFactory nf = net.getFactory();
			Node n1 = nf.createNode(this.s.createId("1"), this.s.createCoord(0, 0));
			Node n2 = nf.createNode(this.s.createId("2"), this.s.createCoord(0, 1000));
			Node n3 = nf.createNode(this.s.createId("3"), this.s.createCoord(0, 2000));
			Node n4 = nf.createNode(this.s.createId("4"), this.s.createCoord(0, 3000));
			net.addNode(n1);
			net.addNode(n2);
			net.addNode(n3);
			net.addNode(n4);
			Link l1 = nf.createLink(this.s.createId("1"), n1, n2);
			Link l2 = nf.createLink(this.s.createId("2"), n2, n3);
			Link l3 = nf.createLink(this.s.createId("3"), n3, n4);
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
