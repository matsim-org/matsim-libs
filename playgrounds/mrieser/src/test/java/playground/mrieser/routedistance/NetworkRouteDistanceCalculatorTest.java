/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.mrieser.routedistance;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author mrieser
 */
public class NetworkRouteDistanceCalculatorTest {

	@Test
	public void testCalcDistance() {
		Fixture f = new Fixture();
		NetworkRoute route = new LinkNetworkRouteImpl(f.ids[0], f.ids[5]);
		List<Id> linkIds = new ArrayList<Id>();
		Collections.addAll(linkIds, f.ids[1], f.ids[2], f.ids[3]);
		route.setLinkIds(f.ids[0], linkIds, f.ids[4]);
		Assert.assertEquals(900.0, f.distCalc.calcDistance(route), MatsimTestUtils.EPSILON);
		// modify the route
		linkIds.add(f.ids[4]);
		route.setLinkIds(f.ids[0], linkIds, f.ids[5]);
		Assert.assertEquals(1400.0, f.distCalc.calcDistance(route), MatsimTestUtils.EPSILON);
	}

	@Test
	public void testCalcDistance_sameStartEndRoute() {
		Fixture f = new Fixture();
		NetworkRoute route = new LinkNetworkRouteImpl(f.ids[3], f.ids[3]);
		List<Id> linkIds = new ArrayList<Id>();
		route.setLinkIds(f.ids[3], linkIds, f.ids[3]);
		Assert.assertEquals(0.0, f.distCalc.calcDistance(route), MatsimTestUtils.EPSILON);
	}

	@Test
	public void testCalcDistance_subsequentStartEndRoute() {
		Fixture f = new Fixture();
		NetworkRoute route = new LinkNetworkRouteImpl(f.ids[2], f.ids[3]);
		List<Id> linkIds = new ArrayList<Id>();
		route.setLinkIds(f.ids[2], linkIds, f.ids[3]);
		Assert.assertEquals(0.0, f.distCalc.calcDistance(route), MatsimTestUtils.EPSILON);
	}

	@Test
	public void testCalcDistance_oneLinkRoute() {
		Fixture f = new Fixture();
		NetworkRoute route = new LinkNetworkRouteImpl(f.ids[2], f.ids[4]);
		List<Id> linkIds = new ArrayList<Id>();
		linkIds.add(f.ids[3]);
		route.setLinkIds(f.ids[2], linkIds, f.ids[4]);
		Assert.assertEquals(400.0, f.distCalc.calcDistance(route), MatsimTestUtils.EPSILON);
	}

	private static class Fixture {
		protected final Scenario scenario;
		protected final Network network;
		protected final Id[] ids;
		protected final NetworkRouteDistanceCalculator distCalc;

		protected Fixture() {
			this.scenario = new ScenarioImpl();
			this.network = this.scenario.getNetwork();
			this.distCalc = new NetworkRouteDistanceCalculator(this.network);
			NetworkFactory nf = this.network.getFactory();

			this.ids = new Id[7];
			for (int i = 0; i < this.ids.length; i++) {
				this.ids[i] = this.scenario.createId(Integer.toString(i));
			}

			Node n0 = nf.createNode(this.ids[0], this.scenario.createCoord(0, 0));
			Node n1 = nf.createNode(this.ids[1], this.scenario.createCoord(100, 0));
			Node n2 = nf.createNode(this.ids[2], this.scenario.createCoord(200, 0));
			Node n3 = nf.createNode(this.ids[3], this.scenario.createCoord(300, 0));
			Node n4 = nf.createNode(this.ids[4], this.scenario.createCoord(400, 0));
			Node n5 = nf.createNode(this.ids[5], this.scenario.createCoord(500, 0));
			Node n6 = nf.createNode(this.ids[6], this.scenario.createCoord(600, 0));
			this.network.addNode(n0);
			this.network.addNode(n1);
			this.network.addNode(n2);
			this.network.addNode(n3);
			this.network.addNode(n4);
			this.network.addNode(n5);
			this.network.addNode(n6);
			this.network.addLink(nf.createLink(this.ids[0], n0, n1));
			this.network.addLink(nf.createLink(this.ids[1], n1, n2));
			this.network.addLink(nf.createLink(this.ids[2], n2, n3));
			this.network.addLink(nf.createLink(this.ids[3], n3, n4));
			this.network.addLink(nf.createLink(this.ids[4], n4, n5));
			this.network.addLink(nf.createLink(this.ids[5], n5, n6));

			this.network.getLinks().get(this.ids[0]).setLength(100.0);
			this.network.getLinks().get(this.ids[1]).setLength(200.0);
			this.network.getLinks().get(this.ids[2]).setLength(300.0);
			this.network.getLinks().get(this.ids[3]).setLength(400.0);
			this.network.getLinks().get(this.ids[4]).setLength(500.0);
			this.network.getLinks().get(this.ids[5]).setLength(600.0);
		}
	}
}
