/* *********************************************************************** *
 * project: org.matsim.*
 * InvertertedNetworkLegRouterTest
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

import junit.framework.Assert;

import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
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
import org.matsim.core.router.costcalculators.TravelCostCalculatorFactory;
import org.matsim.core.router.costcalculators.TravelCostCalculatorFactoryImpl;
import org.matsim.core.router.util.DijkstraFactory;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.PersonalizableLinkToLinkTravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;



/**
 * Tests the routing under consideration of different link to link travel times (turning moves)
 * @author dgrether
 *
 */
public class InvertertedNetworkLegRouterTest {

	@Test
	public void testInvertedNetworkLegRouter(){
		Fixture f = new Fixture();
		NetworkFactoryImpl routeFactory = ((NetworkImpl) f.s.getNetwork()).getFactory();

		LinkToLinkTravelTimeStub tt = new LinkToLinkTravelTimeStub();
		TravelCostCalculatorFactory tc = new TravelCostCalculatorFactoryImpl();
		LeastCostPathCalculatorFactory lcpFactory = new DijkstraFactory();

		Person person = new PersonImpl(new IdImpl(1));
		Leg leg = new LegImpl(TransportMode.car);
		Activity fromAct = new ActivityImpl("h", new IdImpl("12"));
		Activity toAct = new ActivityImpl("h", new IdImpl("78"));

		InvertedNetworkLegRouter router = new InvertedNetworkLegRouter(f.s.getNetwork(), routeFactory, lcpFactory, 
				tc, f.s.getConfig().planCalcScore(), tt);
		//test 1
		tt.setTurningMoveCosts(0.0, 100.0, 50.0);
		
		router.routeLeg(person, leg, fromAct, toAct, 0.0);
		LinkNetworkRoute route = (LinkNetworkRoute) leg.getRoute();
		Assert.assertNotNull(route);
		Assert.assertEquals(new IdImpl("12"), route.getStartLinkId());
		Assert.assertEquals(new IdImpl("78"), route.getEndLinkId());
		Assert.assertEquals(3, route.getLinkIds().size());
		Assert.assertEquals(new  IdImpl("23"), route.getLinkIds().get(0));
		Assert.assertEquals(new  IdImpl("34"), route.getLinkIds().get(1));
		Assert.assertEquals(new  IdImpl("47"), route.getLinkIds().get(2));
		
		//test 2
		tt.setTurningMoveCosts(100.0, 0.0, 50.0);
		router.routeLeg(person, leg, fromAct, toAct, 0.0);
		route = (LinkNetworkRoute) leg.getRoute();
		Assert.assertNotNull(route);
		Assert.assertEquals(new IdImpl("12"), route.getStartLinkId());
		Assert.assertEquals(new IdImpl("78"), route.getEndLinkId());
		Assert.assertEquals(3, route.getLinkIds().size());
		Assert.assertEquals(new  IdImpl("23"), route.getLinkIds().get(0));
		Assert.assertEquals(new  IdImpl("35"), route.getLinkIds().get(1));
		Assert.assertEquals(new  IdImpl("57"), route.getLinkIds().get(2));
		
		//test 3
		tt.setTurningMoveCosts(50.0, 100.0, 0.0);
		
		router.routeLeg(person, leg, fromAct, toAct, 0.0);
		route = (LinkNetworkRoute) leg.getRoute();
		Assert.assertNotNull(route);
		Assert.assertEquals(new IdImpl("12"), route.getStartLinkId());
		Assert.assertEquals(new IdImpl("78"), route.getEndLinkId());
		Assert.assertEquals(3, route.getLinkIds().size());
		Assert.assertEquals(new  IdImpl("23"), route.getLinkIds().get(0));
		Assert.assertEquals(new  IdImpl("36"), route.getLinkIds().get(1));
		Assert.assertEquals(new  IdImpl("67"), route.getLinkIds().get(2));


		
	}

	private static class LinkToLinkTravelTimeStub implements PersonalizableLinkToLinkTravelTime {

		private double turningMoveCosts34;
		private double turningMoveCosts35;
		private double turningMoveCosts36;
		@Override
		public double getLinkTravelTime(Link link, double time) {
			throw new RuntimeException();
//			return link.getLength() / link.getFreespeed(time);
		}

		public void setTurningMoveCosts(double link34, double link35, double link36) {
			this.turningMoveCosts34 = link34;
			this.turningMoveCosts35 = link35;
			this.turningMoveCosts36 = link36;
		}

		@Override
		public double getLinkToLinkTravelTime(Link fromLink, Link toLink, double time) {
			double tt = fromLink.getLength() / fromLink.getFreespeed(time);
			if (new IdImpl("34").equals(toLink.getId())){
				tt = tt + this.turningMoveCosts34;
			}
			else if (new IdImpl("35").equals(toLink.getId())){
				tt = tt + this.turningMoveCosts35;
			}
			else if (new IdImpl("36").equals(toLink.getId())){
				tt = tt + this.turningMoveCosts36;
			}
			return tt;
		}

		@Override
		public void setPerson(Person person) {
		}
	}

	
	private static class Fixture {
		public final Scenario s = ScenarioUtils.createScenario(ConfigUtils.createConfig());

		public Fixture() {
			Network net = this.s.getNetwork();
			NetworkFactory nf = net.getFactory();
			Node n1 = nf.createNode(this.s.createId("1"), this.s.createCoord(0, 0));
			Node n2 = nf.createNode(this.s.createId("2"), this.s.createCoord(0, 1000));
			Node n3 = nf.createNode(this.s.createId("3"), this.s.createCoord(0, 2000));
			Node n4 = nf.createNode(this.s.createId("4"), this.s.createCoord(500, 3000));
			Node n5 = nf.createNode(this.s.createId("5"), this.s.createCoord(0, 3000));
			Node n6 = nf.createNode(this.s.createId("6"), this.s.createCoord(-500, 3000));
			Node n7 = nf.createNode(this.s.createId("7"), this.s.createCoord(0, 4000));
			Node n8 = nf.createNode(this.s.createId("8"), this.s.createCoord(0, 5000));
			net.addNode(n1);
			net.addNode(n2);
			net.addNode(n3);
			net.addNode(n4);
			net.addNode(n5);
			net.addNode(n6);
			net.addNode(n7);
			net.addNode(n8);
			Link l12 = nf.createLink(this.s.createId("12"), n1, n2);
			Link l23 = nf.createLink(this.s.createId("23"), n2, n3);
			Link l34 = nf.createLink(this.s.createId("34"), n3, n4);
			Link l35 = nf.createLink(this.s.createId("35"), n3, n5);
			Link l36 = nf.createLink(this.s.createId("36"), n3, n6);
			Link l47 = nf.createLink(this.s.createId("47"), n4, n7);
			Link l57 = nf.createLink(this.s.createId("57"), n5, n7);
			Link l67 = nf.createLink(this.s.createId("67"), n6, n7);
			Link l78 = nf.createLink(this.s.createId("78"), n7, n8);
			l12.setFreespeed(10.0);
			l12.setLength(1000.0);
			l23.setFreespeed(10.0);
			l23.setLength(1000.0);
			l34.setFreespeed(10.0);
			l34.setLength(1000.0);
			l35.setFreespeed(10.0);
			l35.setLength(1000.0);
			l36.setFreespeed(10.0);
			l36.setLength(1000.0);
			l47.setFreespeed(10.0);
			l47.setLength(1000.0);
			l57.setFreespeed(10.0);
			l57.setLength(1000.0);
			l67.setFreespeed(10.0);
			l67.setLength(1000.0);
			l78.setFreespeed(10.0);
			l78.setLength(1000.0);
			net.addLink(l12);
			net.addLink(l23);
			net.addLink(l34);
			net.addLink(l35);
			net.addLink(l36);
			net.addLink(l47);
			net.addLink(l57);
			net.addLink(l67);
			net.addLink(l78);
		}
	}
	
}
