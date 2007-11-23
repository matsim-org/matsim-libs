/* *********************************************************************** *
 * project: org.matsim.*
 * VDSSignTest.java
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

package org.matsim.withinday.trafficmanagement;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.apache.log4j.Logger;
import org.matsim.gbl.Gbl;
import org.matsim.mobsim.SimulationTimer;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.network.Node;
import org.matsim.plans.Route;
import org.matsim.withinday.trafficmanagement.VDSSign;
import org.matsim.withinday.trafficmanagement.controltheorycontroler.BangBangControler;


/**
 * @author dgrether
 *
 */
public class VDSSignTest extends TestCase {

	private static final String networkFile = "./test/input/org/matsim/withinday/network.xml";

	private static final Logger log = Logger.getLogger(VDSSignTest.class);

	private NetworkLayer network;
	private Route route2;
	private Route route1;
	private EmptyControlInputImpl controlInput;

	private int systemTime;
	/**
	 * @see junit.framework.TestCase#setUp()
	 */
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.network = this.loadNetwork(networkFile);
		SimulationTimer.reset();
		this.systemTime = (int) SimulationTimer.getTime();
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
		sign.setSignLink(this.network.getLink("2"));
		sign.setDirectionLink(this.network.getLink("7"));
		sign.setBenefitControl(false);
		sign.setUpdateTime(1);
		sign.setControlEvents(1);
		sign.setDeadZoneSystemInput(0.0);
		sign.setDeadZoneSystemOutput(0.0);
		sign.setNominalSplitting(1);
		sign.setControler(new BangBangControler());
		sign.setCompliance(1.0);
		//create control input
		this.controlInput = new EmptyControlInputImpl();
		this.controlInput.setNashTime(0);

		this.route1 = new Route();
		ArrayList<Node> list = new ArrayList<Node>();
		list.add(this.network.getNode("3"));
		list.add(this.network.getNode("31"));
		list.add(this.network.getNode("4"));
		this.route1.setRoute(list);
		this.controlInput.setMainRoute(this.route1);
		this.route2 = new Route();
		list = new ArrayList<Node>();
		list.add(this.network.getNode("3"));
		list.add(this.network.getNode("32"));
		list.add(this.network.getNode("4"));
		this.route2.setRoute(list);
		this.controlInput.setAlternativeRoute(this.route2);
		//set control input
		sign.setControlInput(this.controlInput);
		return sign;
	}

	private void incrementSystemTime() {
		SimulationTimer.incTime();
		this.systemTime = (int) SimulationTimer.getTime();
	}

	public void testSignInitialization() {
		VDSSign sign = this.createSign();
		sign.init();
		//test for side effects of init
		assertEquals("side effect in VDSSign init()", this.route1, this.controlInput.getMainRoute());
		assertEquals("side effect in VDSSign init()", this.route2, this.controlInput.getAlternativeRoute());
	}


	public void testBasicRouteCompletion() {
		log.debug("");
		VDSSign sign = this.createSign();
		sign.init();
		sign.open();
		log.debug("test's system time: " + this.systemTime);
		sign.calculateOutput(this.systemTime);
		Route r1 = sign.requestRoute();
//		log.debug("Route1 is: " + LogRouteUtils.getNodeRoute(r1));
		assertNotSame("routes should not be equal to those of ControlInput", this.route1, r1);
		//however with the setting the nodes should be the same:
		for (int i = 0; i < r1.getRoute().size(); i++) {
			assertEquals("nodes not the same!", r1.getRoute().get(i), this.route1.getRoute().get(i));
		}
		//same for route 2
		sign.setNominalSplitting(0);
		this.incrementSystemTime();
		sign.calculateOutput(this.systemTime);
		Route r2 = sign.requestRoute();
//		log.debug("Route2 is: " + LogRouteUtils.getNodeRoute(r2));
		assertNotSame("routes should not be equal to those of ControlInput", this.route2, r2);
		//however with the setting the nodes should be the same:
		for (int i = 0; i < r1.getRoute().size(); i++) {
			assertEquals("nodes not the same!", r2.getRoute().get(i), this.route2.getRoute().get(i));
		}
	}

	public void testRouteCompletion() {
		VDSSign sign = this.createSign();
		sign.setSignLink(this.network.getLink("1"));
		sign.setDirectionLink(this.network.getLink("8"));
		sign.init();
		sign.open();
		sign.calculateOutput(this.systemTime);
		Route r1 = sign.requestRoute();
//		System.out.println("Route1 is: " + LogRouteUtils.getNodeRoute(r1));
		assertNotSame("routes should not be equal to those of ControlInput", this.route1, r1);
		//same for route 2
		sign.setNominalSplitting(0);
		this.incrementSystemTime();
		sign.calculateOutput(this.systemTime);
		Route r2 = sign.requestRoute();
//		System.out.println("Route2 is: " + LogRouteUtils.getNodeRoute(r2));
		assertNotSame("routes should not be equal to those of ControlInput", this.route2, r2);
		List rl1, rl2;
		rl1 = r1.getRoute();
		rl2 = r2.getRoute();
		assertEquals("nodes should be equal", sign.getSignLink().getToNode(), rl1.get(0));
		assertEquals("nodes should be equal", sign.getSignLink().getToNode(), rl2.get(0));

		assertEquals("nodes should be equal", sign.getDirectionLinks().getFromNode(), rl1.get(rl1.size() -1));
		assertEquals("", sign.getDirectionLinks().getFromNode(), rl2.get(rl2.size() -1));
		for (int i = 1; i < rl1.size() -1; i++) {
			assertEquals("nodes should be equal", this.route1.getRoute().get(i-1), rl1.get(i));
			assertEquals("nodes should be equal", this.route2.getRoute().get(i-1), rl2.get(i));
		}
	}


	public void testRequestRoute() {
		SimulationTimer.reset();
		VDSSign sign = this.createSign();
		sign.init();
		sign.open();
		//test control
		this.controlInput.setNashTime(0);
		sign.calculateOutput(this.systemTime);
		Route r = sign.requestRoute();
//		log.debug("Route is: " + LogRouteUtils.getNodeRoute(r));
		assertNotSame("routes should not be equal to those of ControlInput", this.route1, r);
		//however with the setting the nodes should be the same:
		for (int i = 0; i < r.getRoute().size(); i++) {
			assertEquals("nodes not the same!", r.getRoute().get(i), this.route1.getRoute().get(i));
		}

		//test control with nashTime < 0
		System.out.println();
    this.controlInput.setNashTime(-1000);
		this.incrementSystemTime();

		sign.calculateOutput(this.systemTime);
		r = sign.requestRoute();
//		log.debug("Route is: " + LogRouteUtils.getNodeRoute(r));
		assertNotSame("routes should not be equal to those of ControlInput", this.route1, r);
		//however with the setting the nodes should be the same:
		for (int i = 0; i < r.getRoute().size(); i++) {
			assertEquals("nodes not the same!", r.getRoute().get(i), this.route1.getRoute().get(i));
		}

		//test control with nashTime > 0
		System.out.println();
    this.controlInput.setNashTime(1000);
		this.incrementSystemTime();

		sign.calculateOutput(this.systemTime);
		r = sign.requestRoute();
//		log.debug("Route is: " + LogRouteUtils.getNodeRoute(r));
		assertNotSame("routes should not be equal to those of ControlInput", this.route2, r);
		//however with the setting the nodes should be the same:
		for (int i = 0; i < r.getRoute().size(); i++) {
			assertEquals("nodes not the same!", r.getRoute().get(i), this.route2.getRoute().get(i));
		}
	}



	public void estRequestMultipleRoutes() {
		int updateTime = 30;
		VDSSign sign = new VDSSign();
		sign.setSignLink(this.network.getLink("2"));
		sign.setDirectionLink(this.network.getLink("7"));
		sign.setBenefitControl(false);
		sign.setControlEvents(3);
		sign.setUpdateTime(updateTime);
		sign.setDeadZoneSystemInput(0.2);
		sign.setDeadZoneSystemOutput(0.0);
		sign.setNominalSplitting(0.5);
		sign.setControler(new BangBangControler());

		//create control input
		EmptyControlInputImpl  controlInput = new EmptyControlInputImpl();

		Route route1 = new Route();
		ArrayList<Node> list = new ArrayList<Node>();
		list.add(this.network.getNode("3"));
		list.add(this.network.getNode("31"));
		list.add(this.network.getNode("4"));
		route1.setRoute(list);
		controlInput.setMainRoute(route1);
		Route route2 = new Route();
		list = new ArrayList<Node>();
		list.add(this.network.getNode("3"));
		list.add(this.network.getNode("32"));
		list.add(this.network.getNode("4"));
		route2.setRoute(list);
		controlInput.setAlternativeRoute(route2);
		//set control input
		sign.setControlInput(controlInput);
		sign.init();
		//test for side effects
		assertEquals("side effect in VDSSign init()", route1, controlInput.getMainRoute());
		assertEquals("side effect in VDSSign init()", route2, controlInput.getAlternativeRoute());
		//set simulation timer
		SimulationTimer.reset();
		sign.open();
		//test control
		controlInput.setNashTime(0);
		for (int i = 0; i < updateTime; i++) {
			sign.calculateOutput(i);
			assertNull("with the set values sign should be switched off!", sign.requestRoute());
			SimulationTimer.incTime();
		}
		//test control with nashTime > 0
		controlInput.setNashTime(-1000);
		int currentTime = (int) SimulationTimer.getTime();
		for (int i = currentTime; i < currentTime + updateTime; i++) {
			System.out.println();
			sign.calculateOutput(i);
			sign.requestRoute();
			SimulationTimer.incTime();
		}





	}
}
