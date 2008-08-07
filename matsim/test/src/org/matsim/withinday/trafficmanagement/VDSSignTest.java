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
import org.matsim.population.Route;
import org.matsim.withinday.trafficmanagement.feedbackcontroler.BangBangControler;
import org.matsim.withinday.trafficmanagement.feedbackcontroler.ConstantControler;


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
		sign.setMessageHoldTime(1);
		sign.setControlEvents(1);
		sign.setDeadZoneSystemInput(0.0);
		sign.setDeadZoneSystemOutput(0.0);
		sign.setNominalSplitting(0.5);
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
		sign.setupIteration();
		//test for side effects of init
		assertEquals("side effect in VDSSign init()", this.route1, this.controlInput.getMainRoute());
		assertEquals("side effect in VDSSign init()", this.route2, this.controlInput.getAlternativeRoute());
	}


	public void testBasicRouteCompletion() {
		System.out.println();
		log.debug("starting testBasicRouteCompletion...");
		VDSSign sign = this.createSign();
		sign.setupIteration();
		sign.simulationPrepared();
		log.debug("test's system time: " + this.systemTime);
		this.controlInput.setNashTime(-1);
		sign.calculateOutput(this.systemTime);
		Route r1 = sign.requestRoute();
//		log.debug("Route1 is: " + LogRouteUtils.getNodeRoute(r1));
		assertNotSame("routes should not be equal to those of ControlInput", this.route1, r1);
		//however with the setting the nodes should be the same:
		for (int i = 0; i < r1.getRoute().size(); i++) {
			assertEquals("nodes not the same!", r1.getRoute().get(i), this.route1.getRoute().get(i));
		}
		//same for route 2
		this.controlInput.setNashTime(1);
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
		System.out.println();
		log.debug("starting testRouteCompletion...");
		VDSSign sign = this.createSign();
		sign.setSignLink(this.network.getLink("1"));
		sign.setDirectionLink(this.network.getLink("8"));
		sign.setupIteration();
		sign.simulationPrepared();
		this.controlInput.setNashTime(-1);
		sign.calculateOutput(this.systemTime);
		Route r1 = sign.requestRoute();
//		System.out.println("Route1 is: " + LogRouteUtils.getNodeRoute(r1));
		assertNotSame("routes should not be equal to those of ControlInput", this.route1, r1);
		//same for route 2
		this.incrementSystemTime();
		this.controlInput.setNashTime(1);
		sign.calculateOutput(this.systemTime);
		Route r2 = sign.requestRoute();
//		System.out.println("Route2 is: " + LogRouteUtils.getNodeRoute(r2));
		assertNotSame("routes should not be equal to those of ControlInput", this.route2, r2);
		List<Node> rl1, rl2;
		rl1 = r1.getRoute();
		rl2 = r2.getRoute();
		assertEquals("nodes should be equal", sign.getSignLink().getToNode(), rl1.get(0));
		assertEquals("nodes should be equal", sign.getSignLink().getToNode(), rl2.get(0));

		assertEquals("nodes should be equal", sign.getDirectionLinks().getFromNode(), rl1.get(rl1.size() -1));
		assertEquals("nodes should be equal", sign.getDirectionLinks().getFromNode(), rl2.get(rl2.size() -1));
		for (int i = 1; i < rl1.size() -1; i++) {
			assertEquals("nodes should be equal", this.route1.getRoute().get(i-1), rl1.get(i));
			assertEquals("nodes should be equal", this.route2.getRoute().get(i-1), rl2.get(i));
		}
	}


	public void testRequestRoute() {
		System.out.println();
		log.debug("starting testRequestRoute...");
		SimulationTimer.reset();
		VDSSign sign = this.createSign();
		sign.setupIteration();
		sign.simulationPrepared();
		//test control
		this.controlInput.setNashTime(0);
		sign.calculateOutput(this.systemTime);
		Route r = sign.requestRoute();
//		log.debug("Route is: " + LogRouteUtils.getNodeRoute(r));
		assertNotSame("routes should not be equal to those of ControlInput", this.route1, r);
		assertNull("with a nash time of 0 the sign should be switched off!", r);

		//test control with nashTime < 0
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



	public void testRequestMultipleRoutesOneControlEvent() {
		System.out.println();
		log.debug("starting testRequestMultipleRoutesOneControlEvent...");
		SimulationTimer.reset();
		VDSSign sign = this.createSign();
		int messageHoldTime = 30;
		sign.setMessageHoldTime(messageHoldTime);
		sign.setupIteration();
		sign.simulationPrepared();
		//test control
		this.controlInput.setNashTime(0);
//		log.debug("Route is: " + LogRouteUtils.getNodeRoute(r));
		for (int i = 0; i < messageHoldTime; i++) {
			sign.calculateOutput(i);
			assertNotSame("routes should not be equal to those of ControlInput", this.route1, sign.requestRoute());
			assertNull("with the set values sign should be switched off!", sign.requestRoute());
			this.incrementSystemTime();
	}

		this.controlInput.setNashTime(-1);
		for (int i = messageHoldTime; i < 3*messageHoldTime; i++) {
			sign.calculateOutput(i);
			Route r = sign.requestRoute();
			assertNotSame("routes should not be equal to those of ControlInput", this.route1, sign.requestRoute());
			//however with the setting the nodes should be the same:
			for (int j = 0; j < r.getRoute().size(); j++) {
				assertEquals("nodes not the same!", r.getRoute().get(j), this.route1.getRoute().get(j));
			}
			this.incrementSystemTime();
		}

		this.controlInput.setNashTime(1);
		for (int i = 3*messageHoldTime; i < 5*messageHoldTime; i++) {
			sign.calculateOutput(i);
			Route r = sign.requestRoute();
			assertNotSame("routes should not be equal to those of ControlInput", this.route2, sign.requestRoute());
			//however with the setting the nodes should be the same:
			for (int j = 0; j < r.getRoute().size(); j++) {
				assertEquals("nodes not the same!", r.getRoute().get(j), this.route2.getRoute().get(j));
			}
			this.incrementSystemTime();
		}
	}

	public void testRequestMultipleRoutes4ControlEvents() {
		System.out.println();
		log.debug("starting testRequestMultipleRoutes4ControlEvents...");
		SimulationTimer.reset();
		VDSSign sign = this.createSign();
		int messageHoldTime = 30;
		int controlEvents = 4;
		sign.setMessageHoldTime(messageHoldTime);
		sign.setControlEvents(controlEvents);
		sign.setControler(new ConstantControler(1));
		sign.setupIteration();
		sign.simulationPrepared();
		//test control
//		log.debug("Route is: " + LogRouteUtils.getNodeRoute(r));
		for (int i = 0; i < controlEvents * messageHoldTime; i++) {
			sign.calculateOutput(i);
			Route r = sign.requestRoute();
			assertNotSame("routes should not be equal to those of ControlInput", this.route1, sign.requestRoute());
			//however with the setting the nodes should be the same:
			for (int j = 0; j < r.getRoute().size(); j++) {
				assertEquals("nodes not the same!", r.getRoute().get(j), this.route1.getRoute().get(j));
			}
			this.incrementSystemTime();
		}

		sign.setControler(new ConstantControler(-1));
		int start = this.systemTime;
		int stop = this.systemTime + controlEvents * messageHoldTime;
		for (int i = start; i < stop; i++) {
			sign.calculateOutput(i);
			Route r = sign.requestRoute();
			assertNotSame("routes should not be equal to those of ControlInput", this.route2, sign.requestRoute());
			//however with the setting the nodes should be the same:
			for (int j = 0; j < r.getRoute().size(); j++) {
				assertEquals("nodes not the same!", r.getRoute().get(j), this.route2.getRoute().get(j));
			}
			this.incrementSystemTime();
		}

		sign.setControler(new ConstantControler(0));
		start = this.systemTime;
		stop = this.systemTime + controlEvents * messageHoldTime;
		for (int i = start; i < stop; i++) {
			sign.calculateOutput(i);
			assertNotSame("routes should not be equal to those of ControlInput", this.route1, sign.requestRoute());
			assertNull("with the set values sign should be switched off!", sign.requestRoute());
			this.incrementSystemTime();
		}

		//the next tests depend on the values returned by Gbl.random.nextDouble()
		//we have to set the random seed now
		Gbl.random.setSeed(4711);
		sign.setControler(new ConstantControler(0.5));
		//now the sign should show the alternative route for the period of messageHoldTime
		//with the set random seed
		start = this.systemTime;
		stop = this.systemTime + messageHoldTime;
		for (int i = start; i < stop; i++) {
			sign.calculateOutput(i);
			Route r = sign.requestRoute();
			assertNotSame("routes should not be equal to those of ControlInput", this.route2, sign.requestRoute());
			//however with the setting the nodes should be the same:
			for (int j = 0; j < r.getRoute().size(); j++) {
				assertEquals("nodes not the same!", r.getRoute().get(j), this.route2.getRoute().get(j));
			}
			this.incrementSystemTime();
		}
		//as we expect 75% on the main route the sign should show this route for the next three loops
		//with the set random seed
		start = this.systemTime;
		stop = this.systemTime + 3 * messageHoldTime;
		for (int i = start; i < stop; i++) {
			sign.calculateOutput(i);
			Route r = sign.requestRoute();
			assertNotSame("routes should not be equal to those of ControlInput", this.route1, sign.requestRoute());
			//however with the setting the nodes should be the same:
			for (int j = 0; j < r.getRoute().size(); j++) {
				assertEquals("nodes not the same!", r.getRoute().get(j), this.route1.getRoute().get(j));
			}
			this.incrementSystemTime();
		}

		sign.setControler(new ConstantControler(-0.5));
		//now the sign should show the main route for the period of messageHoldTime
		//with the set random seed
		start = this.systemTime;
		stop = this.systemTime + messageHoldTime;
		for (int i = start; i < stop; i++) {
			sign.calculateOutput(i);
			Route r = sign.requestRoute();
			assertNotSame("routes should not be equal to those of ControlInput", this.route1, sign.requestRoute());
			//however with the setting the nodes should be the same:
			for (int j = 0; j < r.getRoute().size(); j++) {
				assertEquals("nodes not the same!", r.getRoute().get(j), this.route1.getRoute().get(j));
			}
			this.incrementSystemTime();
		}
		//as we expect 75% on the alternative route the sign should show this route for the next three loops
		//with the set random seed
		start = this.systemTime;
		stop = this.systemTime +  3 * messageHoldTime;
		for (int i = start; i < stop; i++) {
			sign.calculateOutput(i);
			Route r = sign.requestRoute();
			assertNotSame("routes should not be equal to those of ControlInput", this.route2, sign.requestRoute());
			//however with the setting the nodes should be the same:
			for (int j = 0; j < r.getRoute().size(); j++) {
				assertEquals("nodes not the same!", r.getRoute().get(j), this.route2.getRoute().get(j));
			}
			this.incrementSystemTime();
		}

	}

}
