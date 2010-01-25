/* *********************************************************************** *
 * project: org.matsim.*
 * CoopersRouteProviderTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.withinday.coopers;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.routes.NetworkRouteWRefs;
import org.matsim.core.utils.misc.RouteUtils;
import org.matsim.ptproject.qsim.QSimTimer;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.withinday.coopers.routeprovider.CoopersRouteProvider;
import org.matsim.withinday.routeprovider.AStarLandmarksRouteProvider;
import org.matsim.withinday.trafficmanagement.EmptyControlInputImpl;
import org.matsim.withinday.trafficmanagement.VDSSign;
import org.matsim.withinday.trafficmanagement.feedbackcontroler.ConstantControler;

/**
 * @author dgrether
 */
public class CoopersRouteProviderTest extends MatsimTestCase {

	private static final String networkFile = "./test/input/org/matsim/withinday/network.xml";

	private NetworkLayer network;
	
	private NetworkRouteWRefs route1;

	private NetworkRouteWRefs route2;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		Config config = super.loadConfig(null);
		Scenario scenario = new ScenarioImpl(config);
		this.network = (NetworkLayer) scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(networkFile);
	}

	@Override
	protected void tearDown() throws Exception {
		this.network = null;
		this.route1 = null;
		this.route2 = null;
		super.tearDown();
	}

	private VDSSign createSign() {
		VDSSign sign = new VDSSign();
		sign.setSignLink(this.network.getLinks().get(new IdImpl("1")));
		sign.setDirectionLink(this.network.getLinks().get(new IdImpl("7")));
		sign.setBenefitControl(false);
		sign.setMessageHoldTime(1);
		sign.setControlEvents(1);
		sign.setDeadZoneSystemInput(0.0);
		sign.setDeadZoneSystemOutput(0.0);
		sign.setNominalSplitting(0.5);
		sign.setControler(new ConstantControler(1.0));
		sign.setCompliance(1.0);
		//create control input
		EmptyControlInputImpl controlInput = new EmptyControlInputImpl(network);
		controlInput.setNashTime(0);

		this.route1 = (NetworkRouteWRefs) this.network.getFactory().createRoute(TransportMode.car, null, null);
		ArrayList<Node> list = new ArrayList<Node>();
		list.add(this.network.getNodes().get(new IdImpl("3")));
		list.add(this.network.getNodes().get(new IdImpl("31")));
		list.add(this.network.getNodes().get(new IdImpl("4")));
		this.route1.setNodes(null, list, null);
		controlInput.setMainRoute(this.route1);
		this.route2 = (NetworkRouteWRefs) this.network.getFactory().createRoute(TransportMode.car, null, null);
		list = new ArrayList<Node>();
		list.add(this.network.getNodes().get(new IdImpl("3")));
		list.add(this.network.getNodes().get(new IdImpl("32")));
		list.add(this.network.getNodes().get(new IdImpl("4")));
		this.route2.setNodes(null, list, null);
		controlInput.setAlternativeRoute(this.route2);
		//set control input
		sign.setControlInput(controlInput);
		sign.setupIteration();
		sign.simulationPrepared();
		sign.calculateOutput(QSimTimer.getTime());
		return sign;
	}

	private CoopersRouteProvider createRouteProvider() {
		AStarLandmarksRouteProvider aStarProvider = new AStarLandmarksRouteProvider(this.network);
		List<VDSSign> signs = new LinkedList<VDSSign>();
		signs.add(createSign());
		return new CoopersRouteProvider(aStarProvider, this.network, signs);
	}


	/**
	 * Test method for {@link org.matsim.withinday.coopers.routeprovider.CoopersRouteProvider#providesRoute(org.matsim.core.network.LinkImpl, org.matsim.core.population.routes.NetworkRouteWRefs)}.
	 */
	public void testProvidesRoute() {
	  //create route which is driven by the person in the real simulated world
		NetworkRouteWRefs agentRoute = (NetworkRouteWRefs) this.network.getFactory().createRoute(TransportMode.car, null, null);
	  ArrayList<Node> nodes = new ArrayList<Node>();
	  nodes.add(this.network.getNodes().get(new IdImpl("3")));
	  nodes.add(this.network.getNodes().get(new IdImpl("32")));
	  nodes.add(this.network.getNodes().get(new IdImpl("4")));
	  nodes.add(this.network.getNodes().get(new IdImpl("5")));
	  agentRoute.setNodes(null, nodes, null);
	  //test the provider
	  //first the cases in which it should not provide a route
	  CoopersRouteProvider provider = this.createRouteProvider();
	  assertFalse("As the sign is set to link no 1 the provider should not provide a route!", provider.providesRoute(this.network.getLinks().get(new IdImpl("2")).getId(), agentRoute));
	  assertFalse("As the sign is set to link no 1 the provider should not provide a route!", provider.providesRoute(this.network.getLinks().get(new IdImpl("3")).getId(), agentRoute));
	  assertFalse("As the sign is set to link no 1 the provider should not provide a route!", provider.providesRoute(this.network.getLinks().get(new IdImpl("4")).getId(), agentRoute));
	  assertFalse("As the sign is set to link no 1 the provider should not provide a route!", provider.providesRoute(this.network.getLinks().get(new IdImpl("5")).getId(), agentRoute));
	  assertFalse("As the sign is set to link no 1 the provider should not provide a route!", provider.providesRoute(this.network.getLinks().get(new IdImpl("6")).getId(), agentRoute));
	  assertFalse("As the sign is set to link no 1 the provider should not provide a route!", provider.providesRoute(this.network.getLinks().get(new IdImpl("7")).getId(), agentRoute));
	  assertFalse("As the sign is set to link no 1 the provider should not provide a route!", provider.providesRoute(this.network.getLinks().get(new IdImpl("8")).getId(), agentRoute));
	  //second: now a route should be provided
	  nodes.add(0, this.network.getNodes().get(new IdImpl("2")));
	  agentRoute.setNodes(null, nodes, null);
	  LinkImpl linkNo1 = this.network.getLinks().get(new IdImpl("1"));
	  assertTrue(provider.providesRoute(linkNo1.getId(), agentRoute));
	  //add another node to the route
	  nodes.add(this.network.getNodes().get(new IdImpl("6")));
	  agentRoute.setNodes(null, nodes, null);
	  assertTrue(provider.providesRoute(linkNo1.getId(), agentRoute));
	  //test a shorter route
	  nodes.remove(nodes.size() -1);
	  nodes.remove(nodes.size() -1);
	  agentRoute.setNodes(null, nodes, null);
	  assertTrue(provider.providesRoute(linkNo1.getId(), agentRoute));

	}

	/**
	 * Test method for {@link org.matsim.withinday.coopers.routeprovider.CoopersRouteProvider#requestRoute(org.matsim.core.network.LinkImpl, org.matsim.core.network.LinkImpl, double)}.
	 */
	public void testRequestRouteLinkLinkDouble() {
	  //create route which is driven by the person in the real simulated world
		NetworkRouteWRefs agentRoute = (NetworkRouteWRefs) this.network.getFactory().createRoute(TransportMode.car, null, null);
	  ArrayList<Node> agentRouteNodes = new ArrayList<Node>();
	  agentRouteNodes.add(this.network.getNodes().get(new IdImpl("2")));
	  agentRouteNodes.add(this.network.getNodes().get(new IdImpl("3")));
	  agentRouteNodes.add(this.network.getNodes().get(new IdImpl("32")));
	  agentRouteNodes.add(this.network.getNodes().get(new IdImpl("4")));
	  agentRoute.setNodes(null, agentRouteNodes, null);
	  //create the route which should be returned by the provider
	  NetworkRouteWRefs providerRoute = (NetworkRouteWRefs) this.network.getFactory().createRoute(TransportMode.car, null, null);
	  ArrayList<Node> providerRouteNodes = new ArrayList<Node>();
	  providerRouteNodes.add(this.network.getNodes().get(new IdImpl("2")));
	  providerRouteNodes.add(this.network.getNodes().get(new IdImpl("3")));
	  providerRouteNodes.add(this.network.getNodes().get(new IdImpl("31")));
	  providerRouteNodes.add(this.network.getNodes().get(new IdImpl("4")));
	  providerRoute.setNodes(null, providerRouteNodes, null);
	  //finish the setup for this test
	  CoopersRouteProvider provider = this.createRouteProvider();
	  LinkImpl linkNo1 = this.network.getLinks().get(new IdImpl("1"));
	  //has to be called first
	  provider.providesRoute(linkNo1.getId(), agentRoute);
	  //check routing
	  NetworkRouteWRefs r = provider.requestRoute(linkNo1, this.network.getLinks().get(new IdImpl("7")), QSimTimer.getTime());
//	  System.out.println("Route is: " + LogRouteUtils.getNodeRoute(r));
	  List<Node> nodes = RouteUtils.getNodes(r, this.network);
		for (int i = 0; i < nodes.size(); i++) {
			assertEquals(providerRouteNodes.get(i), nodes.get(i));
		}

	  r = provider.requestRoute(linkNo1, this.network.getLinks().get(new IdImpl("8")), QSimTimer.getTime());
	  providerRouteNodes.add(this.network.getNodes().get(new IdImpl("5")));

	  nodes = RouteUtils.getNodes(r, this.network);
		for (int i = 0; i < nodes.size(); i++) {
			assertEquals(providerRouteNodes.get(i), nodes.get(i));
		}
	}

}
