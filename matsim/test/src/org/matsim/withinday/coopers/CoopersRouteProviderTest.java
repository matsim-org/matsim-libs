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

import junit.framework.TestCase;

import org.matsim.basic.v01.BasicLeg;
import org.matsim.gbl.Gbl;
import org.matsim.mobsim.queuesim.SimulationTimer;
import org.matsim.network.Link;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.network.Node;
import org.matsim.population.routes.CarRoute;
import org.matsim.withinday.coopers.routeprovider.CoopersRouteProvider;
import org.matsim.withinday.routeprovider.AStarLandmarksRouteProvider;
import org.matsim.withinday.trafficmanagement.EmptyControlInputImpl;
import org.matsim.withinday.trafficmanagement.VDSSign;
import org.matsim.withinday.trafficmanagement.feedbackcontroler.ConstantControler;


/**
 * @author dgrether
 */
public class CoopersRouteProviderTest extends TestCase {

	private static final String networkFile = "./test/input/org/matsim/withinday/network.xml";

	private NetworkLayer network;

	private CarRoute route1;

	private CarRoute route2;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.network = this.loadNetwork(networkFile);
	}

	@Override
	protected void tearDown() throws Exception {
		this.network = null;
		this.route1 = null;
		this.route2 = null;
		super.tearDown();
	}

	private NetworkLayer loadNetwork(final String filename) {
		Gbl.reset();
		NetworkLayer network = new NetworkLayer();
		Gbl.createConfig(null);
		MatsimNetworkReader parser = new MatsimNetworkReader(network);
		parser.readFile(filename);
		return network;
	}

	private VDSSign createSign() {
		VDSSign sign = new VDSSign();
		sign.setSignLink(this.network.getLink("1"));
		sign.setDirectionLink(this.network.getLink("7"));
		sign.setBenefitControl(false);
		sign.setMessageHoldTime(1);
		sign.setControlEvents(1);
		sign.setDeadZoneSystemInput(0.0);
		sign.setDeadZoneSystemOutput(0.0);
		sign.setNominalSplitting(0.5);
		sign.setControler(new ConstantControler(1.0));
		sign.setCompliance(1.0);
		//create control input
		EmptyControlInputImpl controlInput = new EmptyControlInputImpl();
		controlInput.setNashTime(0);

		this.route1 = (CarRoute) this.network.getFactory().createRoute(BasicLeg.Mode.car);
		ArrayList<Node> list = new ArrayList<Node>();
		list.add(this.network.getNode("3"));
		list.add(this.network.getNode("31"));
		list.add(this.network.getNode("4"));
		this.route1.setNodes(list);
		controlInput.setMainRoute(this.route1);
		this.route2 = (CarRoute) this.network.getFactory().createRoute(BasicLeg.Mode.car);
		list = new ArrayList<Node>();
		list.add(this.network.getNode("3"));
		list.add(this.network.getNode("32"));
		list.add(this.network.getNode("4"));
		this.route2.setNodes(list);
		controlInput.setAlternativeRoute(this.route2);
		//set control input
		sign.setControlInput(controlInput);
		sign.setupIteration();
		sign.simulationPrepared();
		sign.calculateOutput(SimulationTimer.getTime());
		return sign;
	}

	private CoopersRouteProvider createRouteProvider() {
		AStarLandmarksRouteProvider aStarProvider = new AStarLandmarksRouteProvider(this.network);
		List<VDSSign> signs = new LinkedList<VDSSign>();
		signs.add(createSign());
		return new CoopersRouteProvider(aStarProvider, signs);
	}


	/**
	 * Test method for {@link org.matsim.withinday.coopers.routeprovider.CoopersRouteProvider#providesRoute(org.matsim.network.Link, org.matsim.population.routes.CarRoute)}.
	 */
	public void testProvidesRoute() {
	  //create route which is driven by the person in the real simulated world
		CarRoute agentRoute = (CarRoute) this.network.getFactory().createRoute(BasicLeg.Mode.car);
	  ArrayList<Node> nodes = new ArrayList<Node>();
	  nodes.add(this.network.getNode("3"));
	  nodes.add(this.network.getNode("32"));
	  nodes.add(this.network.getNode("4"));
	  nodes.add(this.network.getNode("5"));
	  agentRoute.setNodes(nodes);
	  //test the provider
	  //first the cases in which it should not provide a route
	  CoopersRouteProvider provider = this.createRouteProvider();
	  assertFalse("As the sign is set to link no 1 the provider should not provide a route!", provider.providesRoute(this.network.getLink("2"), agentRoute));
	  assertFalse("As the sign is set to link no 1 the provider should not provide a route!", provider.providesRoute(this.network.getLink("3"), agentRoute));
	  assertFalse("As the sign is set to link no 1 the provider should not provide a route!", provider.providesRoute(this.network.getLink("4"), agentRoute));
	  assertFalse("As the sign is set to link no 1 the provider should not provide a route!", provider.providesRoute(this.network.getLink("5"), agentRoute));
	  assertFalse("As the sign is set to link no 1 the provider should not provide a route!", provider.providesRoute(this.network.getLink("6"), agentRoute));
	  assertFalse("As the sign is set to link no 1 the provider should not provide a route!", provider.providesRoute(this.network.getLink("7"), agentRoute));
	  assertFalse("As the sign is set to link no 1 the provider should not provide a route!", provider.providesRoute(this.network.getLink("8"), agentRoute));
	  //second: now a route should be provided
	  nodes.add(0, this.network.getNode("2"));
	  agentRoute.setNodes(nodes);
	  Link linkNo1 = this.network.getLink("1");
	  assertTrue(provider.providesRoute(linkNo1, agentRoute));
	  //add another node to the route
	  nodes.add(this.network.getNode("6"));
	  agentRoute.setNodes(nodes);
	  assertTrue(provider.providesRoute(linkNo1, agentRoute));
	  //test a shorter route
	  nodes.remove(nodes.size() -1);
	  nodes.remove(nodes.size() -1);
	  agentRoute.setNodes(nodes);
	  assertTrue(provider.providesRoute(linkNo1, agentRoute));

	}

	/**
	 * Test method for {@link org.matsim.withinday.coopers.routeprovider.CoopersRouteProvider#requestRoute(org.matsim.network.Link, org.matsim.network.Link, double)}.
	 */
	public void testRequestRouteLinkLinkDouble() {
	  //create route which is driven by the person in the real simulated world
		CarRoute agentRoute = (CarRoute) this.network.getFactory().createRoute(BasicLeg.Mode.car);
	  ArrayList<Node> agentRouteNodes = new ArrayList<Node>();
	  agentRouteNodes.add(this.network.getNode("2"));
	  agentRouteNodes.add(this.network.getNode("3"));
	  agentRouteNodes.add(this.network.getNode("32"));
	  agentRouteNodes.add(this.network.getNode("4"));
	  agentRoute.setNodes(agentRouteNodes);
	  //create the route which should be returned by the provider
	  CarRoute providerRoute = (CarRoute) this.network.getFactory().createRoute(BasicLeg.Mode.car);
	  ArrayList<Node> providerRouteNodes = new ArrayList<Node>();
	  providerRouteNodes.add(this.network.getNode("2"));
	  providerRouteNodes.add(this.network.getNode("3"));
	  providerRouteNodes.add(this.network.getNode("31"));
	  providerRouteNodes.add(this.network.getNode("4"));
	  providerRoute.setNodes(providerRouteNodes);
	  //finish the setup for this test
	  CoopersRouteProvider provider = this.createRouteProvider();
	  Link linkNo1 = this.network.getLink("1");
	  //has to be called first
	  provider.providesRoute(linkNo1, agentRoute);
	  //check routing
	  CarRoute r = provider.requestRoute(linkNo1, this.network.getLink("7"), SimulationTimer.getTime());
//	  System.out.println("Route is: " + LogRouteUtils.getNodeRoute(r));
		for (int i = 0; i < r.getNodes().size(); i++) {
			assertEquals(providerRouteNodes.get(i), r.getNodes().get(i));
		}

	  r = provider.requestRoute(linkNo1, this.network.getLink("8"), SimulationTimer.getTime());
	  providerRouteNodes.add(this.network.getNode("5"));
//	  System.out.println("Route is: " + LogRouteUtils.getNodeRoute(r));
		for (int i = 0; i < r.getNodes().size(); i++) {
			assertEquals(providerRouteNodes.get(i), r.getNodes().get(i));
		}
	}



}
