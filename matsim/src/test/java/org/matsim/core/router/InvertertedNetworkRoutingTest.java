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

import org.junit.Test;
import org.matsim.api.core.v01.*;
import org.matsim.api.core.v01.network.*;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.algorithms.NetworkTurnInfoBuilder;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.costcalculators.*;
import org.matsim.core.router.util.*;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.Facility;

import org.junit.Assert;


/**
 * Tests the routing under consideration of different link to link travel times (turning moves)
 * @author dgrether
 *
 */
public class InvertertedNetworkRoutingTest {


	@Test
	public void testInvertedNetworkLegRouter() {
		Fixture f = new Fixture();
		LinkToLinkTravelTimeStub tt = new LinkToLinkTravelTimeStub();
		TravelDisutilityFactory tc = new RandomizingTimeDistanceTravelDisutilityFactory( TransportMode.car, f.s.getConfig().planCalcScore() );
		LeastCostPathCalculatorFactory lcpFactory = new DijkstraFactory();

		Person person = PopulationUtils.getFactory().createPerson(Id.create(1, Person.class));
        Facility fromFacility = new LinkWrapperFacility(//
                f.s.getNetwork().getLinks().get(Id.create("12", Link.class)));
        Facility toFacility = new LinkWrapperFacility(//
                f.s.getNetwork().getLinks().get(Id.create("78", Link.class)));

		LinkToLinkRoutingModule router =
				new LinkToLinkRoutingModule(
						"mode",
						f.s.getPopulation().getFactory(),
						f.s.getNetwork(), lcpFactory,tc, tt, new NetworkTurnInfoBuilder(f.s));
		//test 1
		tt.setTurningMoveCosts(0.0, 100.0, 50.0);
		
		NetworkRoute route = calcRoute(router, fromFacility, toFacility, person);
		Assert.assertNotNull(route);
		Assert.assertEquals(Id.create("12", Link.class), route.getStartLinkId());
		Assert.assertEquals(Id.create("78", Link.class), route.getEndLinkId());
		Assert.assertEquals(3, route.getLinkIds().size());
		Assert.assertEquals(Id.create("23", Link.class), route.getLinkIds().get(0));
		Assert.assertEquals(Id.create("34", Link.class), route.getLinkIds().get(1));
		Assert.assertEquals(Id.create("47", Link.class), route.getLinkIds().get(2));
		
		//test 2
		tt.setTurningMoveCosts(100.0, 0.0, 50.0);
        route = calcRoute(router, fromFacility, toFacility, person);
		Assert.assertNotNull(route);
		Assert.assertEquals(Id.create("12", Link.class), route.getStartLinkId());
		Assert.assertEquals(Id.create("78", Link.class), route.getEndLinkId());
		Assert.assertEquals(3, route.getLinkIds().size());
		Assert.assertEquals(Id.create("23", Link.class), route.getLinkIds().get(0));
		Assert.assertEquals(Id.create("35", Link.class), route.getLinkIds().get(1));
		Assert.assertEquals(Id.create("57", Link.class), route.getLinkIds().get(2));
		
		//test 3
		tt.setTurningMoveCosts(50.0, 100.0, 0.0);
		
        route = calcRoute(router, fromFacility, toFacility, person);
		Assert.assertNotNull(route);
		Assert.assertEquals(Id.create("12", Link.class), route.getStartLinkId());
		Assert.assertEquals(Id.create("78", Link.class), route.getEndLinkId());
		Assert.assertEquals(3, route.getLinkIds().size());
		Assert.assertEquals(Id.create("23", Link.class), route.getLinkIds().get(0));
		Assert.assertEquals(Id.create("36", Link.class), route.getLinkIds().get(1));
		Assert.assertEquals(Id.create("67", Link.class), route.getLinkIds().get(2));


		
	}

	private NetworkRoute calcRoute(LinkToLinkRoutingModule router, final Facility fromFacility,
            final Facility toFacility, final Person person)
	{
        Leg leg = (Leg)router.calcRoute(fromFacility, toFacility, 0.0, person).get(0);
        return (NetworkRoute) leg.getRoute();
	}
	
	
	private static class LinkToLinkTravelTimeStub implements LinkToLinkTravelTime {

		private double turningMoveCosts34;
		private double turningMoveCosts35;
		private double turningMoveCosts36;

		public void setTurningMoveCosts(double link34, double link35, double link36) {
			this.turningMoveCosts34 = link34;
			this.turningMoveCosts35 = link35;
			this.turningMoveCosts36 = link36;
		}

		@Override
		public double getLinkToLinkTravelTime(Link fromLink, Link toLink, double time) {
			double tt = fromLink.getLength() / fromLink.getFreespeed(time);
			if (Id.create("34", Link.class).equals(toLink.getId())){
				tt = tt + this.turningMoveCosts34;
			}
			else if (Id.create("35", Link.class).equals(toLink.getId())){
				tt = tt + this.turningMoveCosts35;
			}
			else if (Id.create("36", Link.class).equals(toLink.getId())){
				tt = tt + this.turningMoveCosts36;
			}
			return tt;
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
			Node n4 = nf.createNode(Id.create("4", Node.class), new Coord((double) 500, (double) 3000));
			Node n5 = nf.createNode(Id.create("5", Node.class), new Coord((double) 0, (double) 3000));
			double x = -500;
			Node n6 = nf.createNode(Id.create("6", Node.class), new Coord(x, (double) 3000));
			Node n7 = nf.createNode(Id.create("7", Node.class), new Coord((double) 0, (double) 4000));
			Node n8 = nf.createNode(Id.create("8", Node.class), new Coord((double) 0, (double) 5000));
			net.addNode(n1);
			net.addNode(n2);
			net.addNode(n3);
			net.addNode(n4);
			net.addNode(n5);
			net.addNode(n6);
			net.addNode(n7);
			net.addNode(n8);
			Link l12 = nf.createLink(Id.create("12", Link.class), n1, n2);
			Link l23 = nf.createLink(Id.create("23", Link.class), n2, n3);
			Link l34 = nf.createLink(Id.create("34", Link.class), n3, n4);
			Link l35 = nf.createLink(Id.create("35", Link.class), n3, n5);
			Link l36 = nf.createLink(Id.create("36", Link.class), n3, n6);
			Link l47 = nf.createLink(Id.create("47", Link.class), n4, n7);
			Link l57 = nf.createLink(Id.create("57", Link.class), n5, n7);
			Link l67 = nf.createLink(Id.create("67", Link.class), n6, n7);
			Link l78 = nf.createLink(Id.create("78", Link.class), n7, n8);
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
