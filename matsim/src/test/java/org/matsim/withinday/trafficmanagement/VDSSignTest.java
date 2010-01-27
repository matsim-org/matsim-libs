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

import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRouteWRefs;
import org.matsim.core.utils.misc.RouteUtils;
import org.matsim.ptproject.qsim.QSimTimer;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.withinday.trafficmanagement.feedbackcontroler.BangBangControler;
import org.matsim.withinday.trafficmanagement.feedbackcontroler.ConstantControler;


/**
 * @author dgrether
 *
 */
public class VDSSignTest extends MatsimTestCase {

	private static final String networkFile = "./test/input/org/matsim/withinday/network.xml";

	private static final Logger log = Logger.getLogger(VDSSignTest.class);

	private Network network;
	private NetworkRouteWRefs route2;
	private NetworkRouteWRefs route1;
	private EmptyControlInputImpl controlInput;

	private int systemTime;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		Scenario scenario = new ScenarioImpl(super.loadConfig(null));
		this.network = scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(networkFile);
		QSimTimer.reset();
		this.systemTime = (int) QSimTimer.getTime();
	}

	@Override
	protected void tearDown() throws Exception {
		this.controlInput = null;
		this.network = null;
		this.route1 = null;
		this.route2 = null;
		super.tearDown();
	}

	private VDSSign createSign() {
		VDSSign sign = new VDSSign(this.network);
		sign.setSignLink(this.network.getLinks().get(new IdImpl("2")));
		sign.setDirectionLink(this.network.getLinks().get(new IdImpl("7")));
		sign.setBenefitControl(false);
		sign.setMessageHoldTime(1);
		sign.setControlEvents(1);
		sign.setDeadZoneSystemInput(0.0);
		sign.setDeadZoneSystemOutput(0.0);
		sign.setNominalSplitting(0.5);
		sign.setControler(new BangBangControler());
		sign.setCompliance(1.0);
		//create control input
		this.controlInput = new EmptyControlInputImpl(this.network);
		this.controlInput.setNashTime(0);

		Link link3 = this.network.getLinks().get(new IdImpl("3"));
		Link link4 = this.network.getLinks().get(new IdImpl("4"));
		Link link5 = this.network.getLinks().get(new IdImpl("5"));
		Link link6 = this.network.getLinks().get(new IdImpl("6"));

		this.route1 = new LinkNetworkRouteImpl(link3, link5);
		this.controlInput.setMainRoute(this.route1);
		this.route2 = new LinkNetworkRouteImpl(link4, link6);
		this.controlInput.setAlternativeRoute(this.route2);
		//set control input
		sign.setControlInput(this.controlInput);
		return sign;
	}

	private void incrementSystemTime() {
		QSimTimer.incTime();
		this.systemTime = (int) QSimTimer.getTime();
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
		NetworkRouteWRefs r1 = sign.requestRoute();
//		log.debug("Route1 is: " + LogRouteUtils.getNodeRoute(r1));
		assertNotSame("routes should not be equal to those of ControlInput", this.route1, r1);
		//however with the setting the links should be the same:
		assertEquals("start link not the same!", r1.getStartLinkId(), this.route1.getStartLinkId());
		assertEquals("end link not the same!", r1.getEndLinkId(), this.route1.getEndLinkId());
		for (int i = 0; i < r1.getLinkIds().size(); i++) {
			assertEquals("links not the same!", r1.getLinkIds().get(i), this.route1.getLinkIds().get(i));
		}
		//same for route 2
		this.controlInput.setNashTime(1);
		this.incrementSystemTime();
		sign.calculateOutput(this.systemTime);
		NetworkRouteWRefs r2 = sign.requestRoute();
//		log.debug("Route2 is: " + LogRouteUtils.getNodeRoute(r2));
		assertNotSame("routes should not be equal to those of ControlInput", this.route2, r2);
		//however with the setting the link should be the same:
		assertEquals("start link not the same!", r2.getStartLinkId(), this.route2.getStartLinkId());
		assertEquals("end link not the same!", r2.getEndLinkId(), this.route2.getEndLinkId());
		for (int i = 0; i < r2.getLinkIds().size(); i++) {
			assertEquals("links not the same!", r2.getLinkIds().get(i), this.route2.getLinkIds().get(i));
		}
	}

	public void testRouteCompletion() {
		System.out.println();
		log.debug("starting testRouteCompletion...");
		VDSSign sign = this.createSign();
		sign.setSignLink(this.network.getLinks().get(new IdImpl("1")));
		sign.setDirectionLink(this.network.getLinks().get(new IdImpl("8")));
		sign.setupIteration();
		sign.simulationPrepared();
		this.controlInput.setNashTime(-1);
		sign.calculateOutput(this.systemTime);
		NetworkRouteWRefs r1 = sign.requestRoute();
//		System.out.println("Route1 is: " + LogRouteUtils.getNodeRoute(r1));
		assertNotSame("routes should not be equal to those of ControlInput", this.route1, r1);
		//same for route 2
		this.incrementSystemTime();
		this.controlInput.setNashTime(1);
		sign.calculateOutput(this.systemTime);
		NetworkRouteWRefs r2 = sign.requestRoute();
//		System.out.println("Route2 is: " + LogRouteUtils.getNodeRoute(r2));
		assertNotSame("routes should not be equal to those of ControlInput", this.route2, r2);
		List<Node> rl1, rl2;
		assertEquals("wrong start link.", new IdImpl("2"), r1.getStartLinkId());
		assertEquals("wrong start link.", new IdImpl("2"), r2.getStartLinkId());
		assertEquals("wrong end link.", new IdImpl("7"), r1.getEndLinkId());
		assertEquals("wrong end link.", new IdImpl("7"), r2.getEndLinkId());

		List<Id> linkIds1 = r1.getLinkIds();
		assertEquals("wrong link", this.route1.getStartLinkId(), linkIds1.get(0));
		for (int i = 2; i < linkIds1.size() - 1; i++) {
			assertEquals("wrong link", this.route1.getLinkIds().get(i-1), linkIds1.get(i));
		}
		assertEquals("wrong link", this.route1.getEndLinkId(), linkIds1.get(linkIds1.size() - 1));

		List<Id> linkIds2 = r2.getLinkIds();
		assertEquals("wrong link", this.route2.getStartLinkId(), linkIds2.get(0));
		for (int i = 1; i < linkIds2.size() - 1; i++) {
			assertEquals("wrong link", this.route2.getLinkIds().get(i-1), linkIds2.get(i));
		}
		assertEquals("wrong link", this.route2.getEndLinkId(), linkIds2.get(linkIds2.size() - 1));
	}


	public void testRequestRoute() {
		System.out.println();
		log.debug("starting testRequestRoute...");
		QSimTimer.reset();
		VDSSign sign = this.createSign();
		sign.setupIteration();
		sign.simulationPrepared();
		//test control
		this.controlInput.setNashTime(0);
		sign.calculateOutput(this.systemTime);
		NetworkRouteWRefs r = sign.requestRoute();
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
		//however with the setting the links should be the same:
		assertEquals("wrong start link", this.route1.getStartLinkId(), r.getStartLinkId());
		assertEquals("wrong end link", this.route1.getEndLinkId(), r.getEndLinkId());
		List<Id> linkIds = r.getLinkIds();
		for (int i = 0; i < linkIds.size() - 1; i++) {
			assertEquals("wrong link", this.route1.getLinkIds().get(i), linkIds.get(i));
		}

		//test control with nashTime > 0
        this.controlInput.setNashTime(1000);
		this.incrementSystemTime();

		sign.calculateOutput(this.systemTime);
		r = sign.requestRoute();
//		log.debug("Route is: " + LogRouteUtils.getNodeRoute(r));
		assertNotSame("routes should not be equal to those of ControlInput", this.route2, r);
		//however with the setting the links should be the same:
		assertEquals("wrong start link", this.route2.getStartLinkId(), r.getStartLinkId());
		assertEquals("wrong end link", this.route2.getEndLinkId(), r.getEndLinkId());
		linkIds = r.getLinkIds();
		for (int i = 0; i < linkIds.size() - 1; i++) {
			assertEquals("wrong link", this.route2.getLinkIds().get(i), linkIds.get(i));
		}
	}


	public void testRequestMultipleRoutesOneControlEvent() {
		System.out.println();
		log.debug("starting testRequestMultipleRoutesOneControlEvent...");
		QSimTimer.reset();
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
			NetworkRouteWRefs r = sign.requestRoute();
			assertNotSame("routes should not be equal to those of ControlInput", this.route1, sign.requestRoute());
			//however with the setting the nodes should be the same:
			List<Node> nodes = RouteUtils.getNodes(r, this.network);
			for (int j = 0; j < nodes.size(); j++) {
				assertEquals("nodes not the same!", nodes.get(j), RouteUtils.getNodes(this.route1, this.network).get(j));
			}
			this.incrementSystemTime();
		}

		this.controlInput.setNashTime(1);
		for (int i = 3*messageHoldTime; i < 5*messageHoldTime; i++) {
			sign.calculateOutput(i);
			NetworkRouteWRefs r = sign.requestRoute();
			assertNotSame("routes should not be equal to those of ControlInput", this.route2, sign.requestRoute());
			//however with the setting the nodes should be the same:
			List<Node> nodes = RouteUtils.getNodes(r, this.network);
			for (int j = 0; j < nodes.size(); j++) {
				assertEquals("nodes not the same!", nodes.get(j), RouteUtils.getNodes(route2, this.network).get(j));
			}
			this.incrementSystemTime();
		}
	}

	public void testRequestMultipleRoutes4ControlEvents() {
		System.out.println();
		log.debug("starting testRequestMultipleRoutes4ControlEvents...");
		QSimTimer.reset();
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
			NetworkRouteWRefs r = sign.requestRoute();
			assertNotSame("routes should not be equal to those of ControlInput", this.route1, sign.requestRoute());
			//however with the setting the links should be the same:
			List<Id> linkIds = r.getLinkIds();
			List<Id> linkIds1 = this.route1.getLinkIds();
			assertEquals("routes have different length!", linkIds1.size(), linkIds.size());
			for (int j = 0; j < linkIds1.size(); j++) {
				assertEquals("different link at position " + j + ". ", linkIds1.get(j), linkIds.get(j));
			}
			this.incrementSystemTime();
		}

		sign.setControler(new ConstantControler(-1));
		int start = this.systemTime;
		int stop = this.systemTime + controlEvents * messageHoldTime;
		for (int i = start; i < stop; i++) {
			sign.calculateOutput(i);
			NetworkRouteWRefs r = sign.requestRoute();
			assertNotSame("routes should not be equal to those of ControlInput", this.route2, sign.requestRoute());
			//however with the setting the nodes should be the same:
			List<Node> nodes = RouteUtils.getNodes(r, this.network);
			List<Node> nodes2 = RouteUtils.getNodes(route2, this.network);
			for (int j = 0; j < nodes.size(); j++) {
				assertEquals("nodes not the same!", nodes.get(j), nodes2.get(j));
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
		MatsimRandom.reset();
		sign.setControler(new ConstantControler(0.5));
		//now the sign should show the alternative route for the period of messageHoldTime
		//with the set random seed
		start = this.systemTime;
		stop = this.systemTime + messageHoldTime;
		for (int i = start; i < stop; i++) {
			sign.calculateOutput(i);
			NetworkRouteWRefs r = sign.requestRoute();
			assertNotSame("routes should not be equal to those of ControlInput", this.route2, sign.requestRoute());
			//however with the setting the nodes should be the same:
			List<Node> nodes = RouteUtils.getNodes(r, this.network);
			for (int j = 0; j < nodes.size(); j++) {
				assertEquals("nodes not the same!", nodes.get(j), RouteUtils.getNodes(route2, this.network).get(j));
			}
			this.incrementSystemTime();
		}
		//as we expect 75% on the main route the sign should show this route for the next three loops
		//with the set random seed
		start = this.systemTime;
		stop = this.systemTime + 3 * messageHoldTime;
		for (int i = start; i < stop; i++) {
			sign.calculateOutput(i);
			NetworkRouteWRefs r = sign.requestRoute();
			assertNotSame("routes should not be equal to those of ControlInput", this.route1, sign.requestRoute());
			//however with the setting the nodes should be the same:
			List<Node> nodes = RouteUtils.getNodes(r, this.network);
			for (int j = 0; j < nodes.size(); j++) {
				assertEquals("nodes not the same!", nodes.get(j), RouteUtils.getNodes(route1, this.network).get(j));
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
			NetworkRouteWRefs r = sign.requestRoute();
			assertNotSame("routes should not be equal to those of ControlInput", this.route1, sign.requestRoute());
			//however with the setting the nodes should be the same:
			List<Node> nodes = RouteUtils.getNodes(r, this.network);
			for (int j = 0; j < nodes.size(); j++) {
				assertEquals("nodes not the same!", nodes.get(j), RouteUtils.getNodes(route1, this.network).get(j));
			}
			this.incrementSystemTime();
		}
		//as we expect 75% on the alternative route the sign should show this route for the next three loops
		//with the set random seed
		start = this.systemTime;
		stop = this.systemTime +  3 * messageHoldTime;
		for (int i = start; i < stop; i++) {
			sign.calculateOutput(i);
			NetworkRouteWRefs r = sign.requestRoute();
			assertNotSame("routes should not be equal to those of ControlInput", this.route2, sign.requestRoute());
			//however with the setting the nodes should be the same:
			List<Node> nodes = RouteUtils.getNodes(r, this.network);
			for (int j = 0; j < nodes.size(); j++) {
				assertEquals("nodes not the same!", nodes.get(j), RouteUtils.getNodes(route2, this.network).get(j));
			}
			this.incrementSystemTime();
		}

	}

}
