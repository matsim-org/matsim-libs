/* *********************************************************************** *
 * project: org.matsim.*
 * TestCANetworkDynamic.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.gregor.casim.simulation.physics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestCase;

import playground.gregor.casim.events.CASimAgentConstructEvent;
import playground.gregor.casim.simulation.physics.CAEvent.CAEventType;

public class TestCANetworkDynamic extends MatsimTestCase {

	private static final double CA_TT_EPSILON = 0.05; //due to integer inaccuracy EPSILON needs to be relaxed

	@Test
	public void testCANetworkDynamicTimeGapTest() {

		//resulting time gap at the end of a link must be independent of link length
		double gap10 = timeGap2AgentsOnLinkWithLength(0.61,10.);

		double gap1000 = timeGap2AgentsOnLinkWithLength(0.61,1000.);

		assertEquals(gap10, gap1000,0.01);
	}





//	@Test
//	public void testCANetworkDynamicOncommingTrafficTravelTimes(){
//
//		//		
//		double diff2Agents = oncommingTrafficTravelTimeTest2AgentsDiff(1);
//		assertEquals(0, diff2Agents, EPSILON);
//
//		double diff4Agents1st = oncommingTrafficTravelTimeTest4AgentsDiff1st(1);
//		assertEquals(0, diff4Agents1st, EPSILON);
//
//		double diff4Agents2nd = oncommingTrafficTravelTimeTest4AgentsDiff2nd(1);
//		assertEquals(0, diff4Agents2nd, EPSILON);
//
//		double tt2Agents = oncommingTrafficTravelTimeTest2AgentsTT(1);
//		double t1 = freespeedForLinkOfXmWidth(1);
//		assertEquals(true, tt2Agents>t1);
//
//
//
//	}



	@Test
	public void testCANetworkDynamicFreespeedOnLinksOfDifferentWidth() {

		//free speed travel time must be independent of link width

		//tt should be:
		int numOfCells = (int) (100*CANetworkDynamic.RHO_HAT+0.5);
		double freeSpeedCellTravelTime = 1/(CANetworkDynamic.RHO_HAT*CANetworkDynamic.V_HAT);
		double travelTime = numOfCells * freeSpeedCellTravelTime;

		double t1 = freespeedForLinkOfXmWidth(1);
		assertEquals(travelTime, t1, EPSILON);

		double t061 = freespeedForLinkOfXmWidth(0.61);
		assertEquals(travelTime, t061, CA_TT_EPSILON);

		double t2 = freespeedForLinkOfXmWidth(2);
		assertEquals(travelTime, t2, CA_TT_EPSILON);

		double t5 = freespeedForLinkOfXmWidth(5);
		assertEquals(travelTime, t5, CA_TT_EPSILON);

		double t1Rev = freespeedForLinkOfXmWidthRev(1);
		assertEquals(t1, t1Rev, EPSILON);

		double t061Rev = freespeedForLinkOfXmWidthRev(0.61);
		assertEquals(t061, t061Rev, EPSILON);

		double t2Rev = freespeedForLinkOfXmWidthRev(2);
		assertEquals(t2, t2Rev, EPSILON);

		double t5Rev = freespeedForLinkOfXmWidthRev(5);
		assertEquals(t5, t5Rev, EPSILON);
	}

	private double timeGap2AgentsOnLinkWithLength(double width, double length) {
		Config c = ConfigUtils.createConfig();
		Scenario sc = ScenarioUtils.createScenario(c);
		Network net = sc.getNetwork();
		NetworkFactory fac = net.getFactory();

		Node n0 = fac.createNode(sc.createId("0"), sc.createCoord(-10, 0));
		Node n1 = fac.createNode(sc.createId("1"), sc.createCoord(0, 0));
		Node n2 = fac.createNode(sc.createId("2"), sc.createCoord(10, 0));
		Node n3 = fac.createNode(sc.createId("3"), sc.createCoord(20, 0));
		net.addNode(n0);
		net.addNode(n1);
		net.addNode(n2);
		net.addNode(n3);

		Link l0 = fac.createLink(sc.createId("0"), n0, n1);
		Link l1 = fac.createLink(sc.createId("1"), n1, n0);
		Link l2 = fac.createLink(sc.createId("2"), n1, n2);
		Link l3 = fac.createLink(sc.createId("3"), n2, n1);
		Link l4 = fac.createLink(sc.createId("4"), n2, n3);
		Link l5 = fac.createLink(sc.createId("5"), n3, n2);

		net.addLink(l0);
		net.addLink(l1);
		net.addLink(l2);
		net.addLink(l3);
		net.addLink(l4);
		net.addLink(l5);

		for (Link l : net.getLinks().values()) {
			l.setLength(10);
			l.setCapacity(width);
		}
		l2.setLength(length);
		l3.setLength(length);

		EventsManager em = new EventsManagerImpl();
		Monitor m = new Monitor();
		em.addHandler(m);
		CANetworkDynamic caNet = new CANetworkDynamic(net, em);
		List<Link> links = new ArrayList<Link>();
		links.add(l0);links.add(l2);links.add(l4);

		CAAgent a0;
		{	

			CALink caLink = caNet.getCALink(links.get(0).getId());
			CAAgent[] particles = caLink.getParticles();
			a0 = new CASimpleDynamicAgent(links, 1, sc.createId("0"), caLink);
			CASimAgentConstructEvent ee = new CASimAgentConstructEvent(0, a0);
			em.processEvent(ee);
			a0.materialize(0, 1);
			particles[0] = a0;
			CAEvent e = new CAEvent(0, a0,caLink, CAEventType.TTA);
			caNet.pushEvent(e);
		}
		CAAgent a1;
		{	

			CALink caLink = caNet.getCALink(links.get(0).getId());
			CAAgent[] particles = caLink.getParticles();
			a1 = new CASimpleDynamicAgent(links, 1, sc.createId("1"), caLink);
			CASimAgentConstructEvent ee = new CASimAgentConstructEvent(0, a1);
			em.processEvent(ee);
			a1.materialize(1, 1);
			particles[1] = a1;
			CAEvent e = new CAEvent(0, a1,caLink, CAEventType.TTA);
			caNet.pushEvent(e);
		}


		caNet.runUntil(3*length);

		double tt1 = m.getAgentLinkExitTime(a0.getId(), l2.getId());
		double tt2 = m.getAgentLinkExitTime(a1.getId(), l2.getId());
		return tt1-tt2;
	}

	private double oncommingTrafficTravelTimeTest2AgentsDiff(double width) {
		Config c = ConfigUtils.createConfig();
		Scenario sc = ScenarioUtils.createScenario(c);
		Network net = sc.getNetwork();
		NetworkFactory fac = net.getFactory();

		Node n0 = fac.createNode(sc.createId("0"), sc.createCoord(-10, 0));
		Node n1 = fac.createNode(sc.createId("1"), sc.createCoord(0, 0));
		Node n2 = fac.createNode(sc.createId("2"), sc.createCoord(10, 0));
		Node n3 = fac.createNode(sc.createId("3"), sc.createCoord(20, 0));
		net.addNode(n0);
		net.addNode(n1);
		net.addNode(n2);
		net.addNode(n3);

		Link l0 = fac.createLink(sc.createId("0"), n0, n1);
		Link l1 = fac.createLink(sc.createId("1"), n1, n0);
		Link l2 = fac.createLink(sc.createId("2"), n1, n2);
		Link l3 = fac.createLink(sc.createId("3"), n2, n1);
		Link l4 = fac.createLink(sc.createId("4"), n2, n3);
		Link l5 = fac.createLink(sc.createId("5"), n3, n2);

		net.addLink(l0);
		net.addLink(l1);
		net.addLink(l2);
		net.addLink(l3);
		net.addLink(l4);
		net.addLink(l5);

		for (Link l : net.getLinks().values()) {
			l.setLength(10);
			l.setCapacity(width);
		}
		l2.setLength(10);
		l3.setLength(10);

		EventsManager em = new EventsManagerImpl();
		Monitor m = new Monitor();
		em.addHandler(m);
		CANetworkDynamic caNet = new CANetworkDynamic(net, em);

		CAAgent a0;
		{	
			List<Link> links = new ArrayList<Link>();
			links.add(l4);links.add(l2);links.add(l0);

			CALink caLink = caNet.getCALink(links.get(0).getId());
			CAAgent[] particles = caLink.getParticles();
			a0 = new CASimpleDynamicAgent(links, 1, sc.createId("0"), caLink);
			CASimAgentConstructEvent ee = new CASimAgentConstructEvent(0, a0);
			em.processEvent(ee);
			a0.materialize(particles.length-1, -1);
			particles[particles.length-1] = a0;
			CAEvent e = new CAEvent(1, a0,caLink, CAEventType.TTA);
			caNet.pushEvent(e);
		}
		CAAgent a1;
		{	
			List<Link> links = new ArrayList<Link>();
			links.add(l0);links.add(l2);links.add(l4);

			CALink caLink = caNet.getCALink(links.get(0).getId());
			CAAgent[] particles = caLink.getParticles();
			a1 = new CASimpleDynamicAgent(links, 1, sc.createId("1"), caLink);
			CASimAgentConstructEvent ee = new CASimAgentConstructEvent(0, a1);
			em.processEvent(ee);
			a1.materialize(0, 1);
			particles[0] = a1;
			CAEvent e = new CAEvent(1, a1,caLink, CAEventType.TTA);
			caNet.pushEvent(e);
		}


//		//VIS only
//		c.global().setCoordinateSystem("EPSG:3395");
//		Sim2DConfig conf2d = Sim2DConfigUtils.createConfig();
//		Sim2DScenario sc2d = Sim2DScenarioUtils.createSim2dScenario(conf2d);
//		sc.addScenarioElement(Sim2DScenario.ELEMENT_NAME,sc2d);
//		EventBasedVisDebuggerEngine vis = new EventBasedVisDebuggerEngine(sc);
//		em.addHandler(vis);
//		QSimDensityDrawer qDbg = new QSimDensityDrawer(sc);
//		em.addHandler(qDbg);
//		vis.addAdditionalDrawer(new InfoBox(vis, sc));
//		vis.addAdditionalDrawer(qDbg);
//		{CASimAgentConstructEvent ee = new CASimAgentConstructEvent(0, a0);
//		em.processEvent(ee);}{CASimAgentConstructEvent ee = new CASimAgentConstructEvent(0, a1);
//		em.processEvent(ee);}

		caNet.runUntil(3*100);

		double tt1 = m.getAgentTravelTimeOnLink(a0.getId(), l3.getId());
		double tt2 = m.getAgentTravelTimeOnLink(a1.getId(), l2.getId());
		return tt1-tt2;
	}

	private double oncommingTrafficTravelTimeTest4AgentsDiff1st(double width) {
		Config c = ConfigUtils.createConfig();
		Scenario sc = ScenarioUtils.createScenario(c);
		Network net = sc.getNetwork();
		NetworkFactory fac = net.getFactory();

		Node n0 = fac.createNode(sc.createId("0"), sc.createCoord(-10, 0));
		Node n1 = fac.createNode(sc.createId("1"), sc.createCoord(0, 0));
		Node n2 = fac.createNode(sc.createId("2"), sc.createCoord(10, 0));
		Node n3 = fac.createNode(sc.createId("3"), sc.createCoord(20, 0));
		net.addNode(n0);
		net.addNode(n1);
		net.addNode(n2);
		net.addNode(n3);

		Link l0 = fac.createLink(sc.createId("0"), n0, n1);
		Link l1 = fac.createLink(sc.createId("1"), n1, n0);
		Link l2 = fac.createLink(sc.createId("2"), n1, n2);
		Link l3 = fac.createLink(sc.createId("3"), n2, n1);
		Link l4 = fac.createLink(sc.createId("4"), n2, n3);
		Link l5 = fac.createLink(sc.createId("5"), n3, n2);

		net.addLink(l0);
		net.addLink(l1);
		net.addLink(l2);
		net.addLink(l3);
		net.addLink(l4);
		net.addLink(l5);

		for (Link l : net.getLinks().values()) {
			l.setLength(10);
			l.setCapacity(width);
		}
		l2.setLength(100);
		l3.setLength(100);

		EventsManager em = new EventsManagerImpl();
		Monitor m = new Monitor();
		em.addHandler(m);
		CANetworkDynamic caNet = new CANetworkDynamic(net, em);

		CAAgent a0;
		{	
			List<Link> links = new ArrayList<Link>();
			links.add(l4);links.add(l2);links.add(l0);

			CALink caLink = caNet.getCALink(links.get(0).getId());
			CAAgent[] particles = caLink.getParticles();
			a0 = new CASimpleDynamicAgent(links, 1, sc.createId("0"), caLink);
			CASimAgentConstructEvent ee = new CASimAgentConstructEvent(0, a0);
			em.processEvent(ee);
			a0.materialize(particles.length-1, -1);
			particles[particles.length-1] = a0;
			CAEvent e = new CAEvent(0, a0,caLink, CAEventType.TTA);
			caNet.pushEvent(e);
		}
		CAAgent a1;
		{	
			List<Link> links = new ArrayList<Link>();
			links.add(l4);links.add(l2);links.add(l0);

			CALink caLink = caNet.getCALink(links.get(0).getId());
			CAAgent[] particles = caLink.getParticles();
			a1 = new CASimpleDynamicAgent(links, 1, sc.createId("0"), caLink);
			CASimAgentConstructEvent ee = new CASimAgentConstructEvent(0, a1);
			em.processEvent(ee);
			a1.materialize(particles.length-2, -1);
			particles[particles.length-2] = a1;
			CAEvent e = new CAEvent(0, a1,caLink, CAEventType.TTA);
			caNet.pushEvent(e);
		}
		CAAgent a2;
		{	
			List<Link> links = new ArrayList<Link>();
			links.add(l0);links.add(l2);links.add(l4);

			CALink caLink = caNet.getCALink(links.get(0).getId());
			CAAgent[] particles = caLink.getParticles();
			a2 = new CASimpleDynamicAgent(links, 1, sc.createId("1"), caLink);
			CASimAgentConstructEvent ee = new CASimAgentConstructEvent(0, a2);
			em.processEvent(ee);
			a2.materialize(0, 1);
			particles[0] = a2;
			CAEvent e = new CAEvent(0, a2,caLink, CAEventType.TTA);
			caNet.pushEvent(e);
		}
		CAAgent a3;
		{	
			List<Link> links = new ArrayList<Link>();
			links.add(l0);links.add(l2);links.add(l4);

			CALink caLink = caNet.getCALink(links.get(0).getId());
			CAAgent[] particles = caLink.getParticles();
			a3 = new CASimpleDynamicAgent(links, 1, sc.createId("1"), caLink);
			CASimAgentConstructEvent ee = new CASimAgentConstructEvent(0, a3);
			em.processEvent(ee);
			a3.materialize(1, 1);
			particles[1] = a3;
			CAEvent e = new CAEvent(0, a3,caLink, CAEventType.TTA);
			caNet.pushEvent(e);
		}

		caNet.runUntil(3*100);

		double tt1 = m.getAgentTravelTimeOnLink(a1.getId(), l3.getId());
		double tt2 = m.getAgentTravelTimeOnLink(a3.getId(), l2.getId());
		return tt1-tt2;
	}

	private double oncommingTrafficTravelTimeTest4AgentsDiff2nd(double width) {
		Config c = ConfigUtils.createConfig();
		Scenario sc = ScenarioUtils.createScenario(c);
		Network net = sc.getNetwork();
		NetworkFactory fac = net.getFactory();

		Node n0 = fac.createNode(sc.createId("0"), sc.createCoord(-10, 0));
		Node n1 = fac.createNode(sc.createId("1"), sc.createCoord(0, 0));
		Node n2 = fac.createNode(sc.createId("2"), sc.createCoord(10, 0));
		Node n3 = fac.createNode(sc.createId("3"), sc.createCoord(20, 0));
		net.addNode(n0);
		net.addNode(n1);
		net.addNode(n2);
		net.addNode(n3);

		Link l0 = fac.createLink(sc.createId("0"), n0, n1);
		Link l1 = fac.createLink(sc.createId("1"), n1, n0);
		Link l2 = fac.createLink(sc.createId("2"), n1, n2);
		Link l3 = fac.createLink(sc.createId("3"), n2, n1);
		Link l4 = fac.createLink(sc.createId("4"), n2, n3);
		Link l5 = fac.createLink(sc.createId("5"), n3, n2);

		net.addLink(l0);
		net.addLink(l1);
		net.addLink(l2);
		net.addLink(l3);
		net.addLink(l4);
		net.addLink(l5);

		for (Link l : net.getLinks().values()) {
			l.setLength(10);
			l.setCapacity(width);
		}
		l2.setLength(100);
		l3.setLength(100);

		EventsManager em = new EventsManagerImpl();
		Monitor m = new Monitor();
		em.addHandler(m);
		CANetworkDynamic caNet = new CANetworkDynamic(net, em);

		CAAgent a0;
		{	
			List<Link> links = new ArrayList<Link>();
			links.add(l4);links.add(l2);links.add(l0);

			CALink caLink = caNet.getCALink(links.get(0).getId());
			CAAgent[] particles = caLink.getParticles();
			a0 = new CASimpleDynamicAgent(links, 1, sc.createId("0"), caLink);
			CASimAgentConstructEvent ee = new CASimAgentConstructEvent(0, a0);
			em.processEvent(ee);
			a0.materialize(particles.length-1, -1);
			particles[particles.length-1] = a0;
			CAEvent e = new CAEvent(0, a0,caLink, CAEventType.TTA);
			caNet.pushEvent(e);
		}
		CAAgent a1;
		{	
			List<Link> links = new ArrayList<Link>();
			links.add(l4);links.add(l2);links.add(l0);

			CALink caLink = caNet.getCALink(links.get(0).getId());
			CAAgent[] particles = caLink.getParticles();
			a1 = new CASimpleDynamicAgent(links, 1, sc.createId("0"), caLink);
			CASimAgentConstructEvent ee = new CASimAgentConstructEvent(0, a1);
			em.processEvent(ee);
			a1.materialize(particles.length-2, -1);
			particles[particles.length-2] = a1;
			CAEvent e = new CAEvent(0, a1,caLink, CAEventType.TTA);
			caNet.pushEvent(e);
		}
		CAAgent a2;
		{	
			List<Link> links = new ArrayList<Link>();
			links.add(l0);links.add(l2);links.add(l4);

			CALink caLink = caNet.getCALink(links.get(0).getId());
			CAAgent[] particles = caLink.getParticles();
			a2 = new CASimpleDynamicAgent(links, 1, sc.createId("1"), caLink);
			CASimAgentConstructEvent ee = new CASimAgentConstructEvent(0, a2);
			em.processEvent(ee);
			a2.materialize(0, 1);
			particles[0] = a2;
			CAEvent e = new CAEvent(0, a2,caLink, CAEventType.TTA);
			caNet.pushEvent(e);
		}
		CAAgent a3;
		{	
			List<Link> links = new ArrayList<Link>();
			links.add(l0);links.add(l2);links.add(l4);

			CALink caLink = caNet.getCALink(links.get(0).getId());
			CAAgent[] particles = caLink.getParticles();
			a3 = new CASimpleDynamicAgent(links, 1, sc.createId("1"), caLink);
			CASimAgentConstructEvent ee = new CASimAgentConstructEvent(0, a3);
			em.processEvent(ee);
			a3.materialize(1, 1);
			particles[1] = a3;
			CAEvent e = new CAEvent(0, a3,caLink, CAEventType.TTA);
			caNet.pushEvent(e);
		}

		caNet.runUntil(3*100);

		double tt1 = m.getAgentTravelTimeOnLink(a0.getId(), l3.getId());
		double tt2 = m.getAgentTravelTimeOnLink(a2.getId(), l2.getId());
		return tt1-tt2;
	}

	private double oncommingTrafficTravelTimeTest2AgentsTT(double width) {
		Config c = ConfigUtils.createConfig();
		Scenario sc = ScenarioUtils.createScenario(c);
		Network net = sc.getNetwork();
		NetworkFactory fac = net.getFactory();

		Node n0 = fac.createNode(sc.createId("0"), sc.createCoord(-10, 0));
		Node n1 = fac.createNode(sc.createId("1"), sc.createCoord(0, 0));
		Node n2 = fac.createNode(sc.createId("2"), sc.createCoord(10, 0));
		Node n3 = fac.createNode(sc.createId("3"), sc.createCoord(20, 0));
		net.addNode(n0);
		net.addNode(n1);
		net.addNode(n2);
		net.addNode(n3);

		Link l0 = fac.createLink(sc.createId("0"), n0, n1);
		Link l1 = fac.createLink(sc.createId("1"), n1, n0);
		Link l2 = fac.createLink(sc.createId("2"), n1, n2);
		Link l3 = fac.createLink(sc.createId("3"), n2, n1);
		Link l4 = fac.createLink(sc.createId("4"), n2, n3);
		Link l5 = fac.createLink(sc.createId("5"), n3, n2);

		net.addLink(l0);
		net.addLink(l1);
		net.addLink(l2);
		net.addLink(l3);
		net.addLink(l4);
		net.addLink(l5);

		for (Link l : net.getLinks().values()) {
			l.setLength(10);
			l.setCapacity(width);
		}
		l2.setLength(100);
		l3.setLength(100);

		EventsManager em = new EventsManagerImpl();
		Monitor m = new Monitor();
		em.addHandler(m);
		CANetworkDynamic caNet = new CANetworkDynamic(net, em);

		CAAgent a0;
		{	
			List<Link> links = new ArrayList<Link>();
			links.add(l4);links.add(l2);links.add(l0);

			CALink caLink = caNet.getCALink(links.get(0).getId());
			CAAgent[] particles = caLink.getParticles();
			a0 = new CASimpleDynamicAgent(links, 1, sc.createId("0"), caLink);
			CASimAgentConstructEvent ee = new CASimAgentConstructEvent(0, a0);
			em.processEvent(ee);
			a0.materialize(particles.length-1, -1);
			particles[particles.length-1] = a0;
			CAEvent e = new CAEvent(0, a0,caLink, CAEventType.TTA);
			caNet.pushEvent(e);
		}
		CAAgent a1;
		{	
			List<Link> links = new ArrayList<Link>();
			links.add(l0);links.add(l2);links.add(l4);

			CALink caLink = caNet.getCALink(links.get(0).getId());
			CAAgent[] particles = caLink.getParticles();
			a1 = new CASimpleDynamicAgent(links, 1, sc.createId("1"), caLink);
			CASimAgentConstructEvent ee = new CASimAgentConstructEvent(0, a1);
			em.processEvent(ee);
			a1.materialize(0, 1);
			particles[0] = a1;
			CAEvent e = new CAEvent(0, a1,caLink, CAEventType.TTA);
			caNet.pushEvent(e);
		}


		caNet.runUntil(3*100);

		double tt1 = m.getAgentTravelTimeOnLink(a0.getId(), l3.getId());
		return tt1;
	}

	private double freespeedForLinkOfXmWidthRev(double width) {
		Config c = ConfigUtils.createConfig();
		Scenario sc = ScenarioUtils.createScenario(c);
		Network net = sc.getNetwork();
		NetworkFactory fac = net.getFactory();

		Node n0 = fac.createNode(sc.createId("0"), sc.createCoord(-10, 0));
		Node n1 = fac.createNode(sc.createId("1"), sc.createCoord(0, 0));
		Node n2 = fac.createNode(sc.createId("2"), sc.createCoord(10, 0));
		Node n3 = fac.createNode(sc.createId("3"), sc.createCoord(20, 0));
		net.addNode(n0);
		net.addNode(n1);
		net.addNode(n2);
		net.addNode(n3);

		Link l0 = fac.createLink(sc.createId("0"), n0, n1);
		Link l1 = fac.createLink(sc.createId("1"), n1, n0);
		Link l2 = fac.createLink(sc.createId("2"), n1, n2);
		Link l3 = fac.createLink(sc.createId("3"), n2, n1);
		Link l4 = fac.createLink(sc.createId("4"), n2, n3);
		Link l5 = fac.createLink(sc.createId("5"), n3, n2);

		net.addLink(l0);
		net.addLink(l1);
		net.addLink(l2);
		net.addLink(l3);
		net.addLink(l4);
		net.addLink(l5);

		for (Link l : net.getLinks().values()) {
			l.setLength(10);
			l.setCapacity(width);
		}
		l2.setLength(100);
		l3.setLength(100);

		EventsManager em = new EventsManagerImpl();
		Monitor m = new Monitor();
		em.addHandler(m);
		CANetworkDynamic caNet = new CANetworkDynamic(net, em);

		List<Link> links = new ArrayList<Link>();
		links.add(l4);links.add(l2);links.add(l0);

		CALink caLink = caNet.getCALink(links.get(0).getId());
		CAAgent[] particles = caLink.getParticles();
		CAAgent a = new CASimpleDynamicAgent(links, 1, sc.createId("0"), caLink);
		CASimAgentConstructEvent ee = new CASimAgentConstructEvent(0, a);
		em.processEvent(ee);
		a.materialize(particles.length-1, -1);
		particles[particles.length-1] = a;
		CAEvent e = new CAEvent(0, a,caLink, CAEventType.TTA);
		caNet.pushEvent(e);
		caNet.runUntil(3*100);

		double tt = m.getAgentTravelTimeOnLink(a.getId(), l3.getId());
		return tt;
	}

	private double freespeedForLinkOfXmWidth(double width) {
		Config c = ConfigUtils.createConfig();
		Scenario sc = ScenarioUtils.createScenario(c);
		Network net = sc.getNetwork();
		NetworkFactory fac = net.getFactory();

		Node n0 = fac.createNode(sc.createId("0"), sc.createCoord(-10, 0));
		Node n1 = fac.createNode(sc.createId("1"), sc.createCoord(0, 0));
		Node n2 = fac.createNode(sc.createId("2"), sc.createCoord(10, 0));
		Node n3 = fac.createNode(sc.createId("3"), sc.createCoord(20, 0));
		net.addNode(n0);
		net.addNode(n1);
		net.addNode(n2);
		net.addNode(n3);

		Link l0 = fac.createLink(sc.createId("0"), n0, n1);
		Link l1 = fac.createLink(sc.createId("1"), n1, n0);
		Link l2 = fac.createLink(sc.createId("2"), n1, n2);
		Link l3 = fac.createLink(sc.createId("3"), n2, n1);
		Link l4 = fac.createLink(sc.createId("4"), n2, n3);
		Link l5 = fac.createLink(sc.createId("5"), n3, n2);

		net.addLink(l0);
		net.addLink(l1);
		net.addLink(l2);
		net.addLink(l3);
		net.addLink(l4);
		net.addLink(l5);

		for (Link l : net.getLinks().values()) {
			l.setLength(10);
			l.setCapacity(width);
		}
		l2.setLength(100);
		l3.setLength(100);

		EventsManager em = new EventsManagerImpl();
		Monitor m = new Monitor();
		em.addHandler(m);
		CANetworkDynamic caNet = new CANetworkDynamic(net, em);

		List<Link> links = new ArrayList<Link>();
		links.add(l0);links.add(l2);links.add(l4);

		CALink caLink = caNet.getCALink(links.get(0).getId());
		CAAgent[] particles = caLink.getParticles();
		CAAgent a = new CASimpleDynamicAgent(links, 1, sc.createId("0"), caLink);
		CASimAgentConstructEvent ee = new CASimAgentConstructEvent(0, a);
		em.processEvent(ee);
		a.materialize(0, 1);
		particles[0] = a;
		CAEvent e = new CAEvent(0, a,caLink, CAEventType.TTA);
		caNet.pushEvent(e);
		caNet.runUntil(3*100);

		double tt = m.getAgentTravelTimeOnLink(a.getId(), l2.getId());
		return tt;
	}


	//	@Test
	//	public void testCANetworkDynamicFreeSpeedLink(){
	//		Config c = ConfigUtils.createConfig();
	//		Scenario sc = ScenarioUtils.createScenario(c);
	//		Network net = sc.getNetwork();
	//		NetworkFactory fac = net.getFactory();
	//		
	//		Node n0 = fac.createNode(sc.createId("0"), sc.createCoord(-10, 0));
	//		Node n1 = fac.createNode(sc.createId("1"), sc.createCoord(0, 0));
	//		Node n2 = fac.createNode(sc.createId("2"), sc.createCoord(10, 0));
	//		Node n3 = fac.createNode(sc.createId("3"), sc.createCoord(20, 0));
	//		net.addNode(n0);
	//		net.addNode(n1);
	//		net.addNode(n2);
	//		net.addNode(n3);
	//		
	//		Link l0 = fac.createLink(sc.createId("0"), n0, n1);
	//		Link l1 = fac.createLink(sc.createId("1"), n1, n0);
	//		Link l2 = fac.createLink(sc.createId("2"), n1, n2);
	//		Link l3 = fac.createLink(sc.createId("3"), n2, n1);
	//		Link l4 = fac.createLink(sc.createId("4"), n2, n3);
	//		Link l5 = fac.createLink(sc.createId("5"), n3, n2);
	//
	//		net.addLink(l0);
	//		net.addLink(l1);
	//		net.addLink(l2);
	//		net.addLink(l3);
	//		net.addLink(l4);
	//		net.addLink(l5);
	//
	//		for (Link l : net.getLinks().values()) {
	//			l.setLength(10);
	//			l.setCapacity(1);
	//		}
	//		
	//		EventsManager em = new EventsManagerImpl();
	//		Monitor m = new Monitor();
	//		em.addHandler(m);
	//		CANetworkDynamic caNet = new CANetworkDynamic(net, em);
	//		
	//		List<Link> links = new ArrayList<Link>();
	//		links.add(l0);links.add(l2);links.add(l4);
	//		
	//		CALink caLink = caNet.getCALink(links.get(0).getId());
	//		CAAgent[] particles = caLink.getParticles();
	//		CAAgent a = new CASimpleDynamicAgent(links, 1, sc.createId("0"), caLink);
	//		CASimAgentConstructEvent ee = new CASimAgentConstructEvent(0, a);
	//		em.processEvent(ee);
	//		a.materialize(0, 1);
	//		particles[0] = a;
	//		CAEvent e = new CAEvent(0, a,caLink, CAEventType.TTA);
	//		caNet.pushEvent(e);
	//		
	////		//VIS only
	////		c.global().setCoordinateSystem("EPSG:3395");
	////		Sim2DConfig conf2d = Sim2DConfigUtils.createConfig();
	////		Sim2DScenario sc2d = Sim2DScenarioUtils.createSim2dScenario(conf2d);
	////		sc.addScenarioElement(Sim2DScenario.ELEMENT_NAME,sc2d);
	////		EventBasedVisDebuggerEngine vis = new EventBasedVisDebuggerEngine(sc);
	////		em.addHandler(vis);
	////		vis.addAdditionalDrawer(new InfoBox(vis, sc));
	//		
	//		//TODO move agent over node
	//		caNet.runUntil(300);
	//		
	//		double tt = m.getAgentTravelTimeOnLink(a.getId(), l2.getId());
	//		
	//		//tt should be:
	//		int numOfCells = (int) (l2.getLength()*l2.getCapacity()*CANetwork.RHO_HAT+0.5);
	//		double freeSpeedCellTravelTime = 1/(l2.getCapacity()*CANetworkDynamic.RHO_HAT*CANetworkDynamic.V_HAT);
	//		double travelTime = numOfCells * freeSpeedCellTravelTime;
	//				
	//		assertEquals(travelTime, tt, EPSILON);
	//	}


	private final class Monitor implements LinkEnterEventHandler, LinkLeaveEventHandler {
		Map<Id,Map<Id,AI>> infos = new HashMap<Id,Map<Id,AI>>();

		@Override
		public void reset(int iteration) {
			// TODO Auto-generated method stub

		}

		@Override
		public void handleEvent(LinkLeaveEvent event) {
			Map<Id, AI> map = this.infos.get(event.getLinkId());
			if (map != null) {
				AI ai = map.get(event.getVehicleId());
				if (ai != null) {
					ai.leaveTime = event.getTime();
				}
			}
		}

		@Override
		public void handleEvent(LinkEnterEvent event) {
			Map<Id, AI> map = this.infos.get(event.getLinkId());
			if (map == null) {
				map = new HashMap<Id,AI>();
				this.infos.put(event.getLinkId(), map);
			}
			AI ai = new AI();
			map.put(event.getVehicleId(), ai);
			ai.enterTime = event.getTime();
		}

		public double getAgentTravelTimeOnLink(Id agentId, Id linkId) {
			Map<Id, AI> map = this.infos.get(linkId);
			if (map != null) {
				AI ai = map.get(agentId);
				if (ai != null) {
					return ai.leaveTime-ai.enterTime;
				}
			}
			return Double.NaN;
		}

		public double getAgentLinkExitTime(Id agentId, Id linkId) {
			Map<Id, AI> map = this.infos.get(linkId);
			if (map != null) {
				AI ai = map.get(agentId);
				if (ai != null) {
					return ai.leaveTime;
				}
			}
			return Double.NaN;
		}

		private final class AI {
			double enterTime;
			double leaveTime;
		}

	}

}
