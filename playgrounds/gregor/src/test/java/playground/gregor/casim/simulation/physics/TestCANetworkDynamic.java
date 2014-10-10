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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
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

	private static final Logger log = Logger.getLogger(TestCANetworkDynamic.class);


	@Test
	public void testCANetworkDynamicTTForOncomingAgentsForDifferentLinkWidths(){

		double freeSpeedCellTravelTime = 1/(CANetworkDynamic.RHO_HAT*CANetworkDynamic.V_HAT);

		{
			double linkLength = 10;//-1/CANetworkDynamic.RHO_HAT;;
			int nrAgents1 = 2;
			double w1 = 0.61;
			double tts1[] = getAgentsBidirectionalTravelTimesForLinkOfWidthAngLength(w1,linkLength,nrAgents1);
			double diffSum = 0;
			for (int i = 0; i < tts1.length/2; i++) {
				log.info("diff =\t" + (tts1[i]-tts1[i+tts1.length/2]) + " max allowd:\t" + freeSpeedCellTravelTime/w1);
				assertEquals(tts1[i],tts1[i+tts1.length/2],2*freeSpeedCellTravelTime/w1);
				diffSum += (tts1[i]-tts1[i+tts1.length/2]);
			}
			log.info("diffSum = \t" + diffSum);
			assertTrue("abs diff sum smaller than cell travel time", Math.abs(diffSum) <= freeSpeedCellTravelTime/w1+EPSILON);
		}
		
		{
			double linkLength = 100;//-1/CANetworkDynamic.RHO_HAT;;
			int nrAgents1 = 1000;
			double w1 = 2.61;
			double tts1[] = getAgentsBidirectionalTravelTimesForLinkOfWidthAngLength(w1,linkLength,nrAgents1);
			double diffSum = 0;
			for (int i = 0; i < tts1.length/2; i++) {
				log.info("diff =\t" + (tts1[i]-tts1[i+tts1.length/2]) + " max allowd:\t" + freeSpeedCellTravelTime/w1);
				assertEquals(tts1[i],tts1[i+tts1.length/2],2*freeSpeedCellTravelTime/w1);
				diffSum += (tts1[i]-tts1[i+tts1.length/2]);
			}
			log.info("diffSum = \t" + diffSum);
			assertTrue("abs diff sum smaller than cell travel time", Math.abs(diffSum) <= freeSpeedCellTravelTime/w1+EPSILON);
		}
		
		{
			double linkLength = 100;//-1/CANetworkDynamic.RHO_HAT;;
			int nrAgents1 = 10;
			double w1 = 2.61;
			double tts1[] = getAgentsBidirectionalTravelTimesForLinkOfWidthAngLength(w1,linkLength,nrAgents1);
			double diffSum = 0;
			for (int i = 0; i < tts1.length/2; i++) {
				log.info("diff =\t" + (tts1[i]-tts1[i+tts1.length/2]) + " max allowd:\t" + freeSpeedCellTravelTime/w1);
				assertEquals(tts1[i],tts1[i+tts1.length/2],2*freeSpeedCellTravelTime/w1);
				diffSum += (tts1[i]-tts1[i+tts1.length/2]);
			}
			log.info("diffSum = \t" + diffSum);
			assertTrue("abs diff sum smaller than cell travel time", Math.abs(diffSum) <= freeSpeedCellTravelTime/w1+EPSILON);
		}

	}



	@Test
	public void testCANetworkDynamicTTInBothDirectionsOfAPairOfAgentsForDifferentLinkWidths(){
		//free speed travel time must be independent of link width
		double linkLength = 100;

		double [] tts1 = getAgentTravelTimesForLinkOfWidthAndLength(1,linkLength);
		double [] tts1Rev = getAgentTravelTimesForLinkOfWidthAndLengthRev(1,linkLength);
		for (int i = 0; i < tts1.length; i++) {
			assertEquals(tts1[i],tts1Rev[i],EPSILON);
		}

		double [] tts2 = getAgentTravelTimesForLinkOfWidthAndLength(2,linkLength); 
		double [] tts2Rev = getAgentTravelTimesForLinkOfWidthAndLengthRev(2,linkLength);
		for (int i = 0; i < tts1.length; i++) {
			assertEquals(tts2[i],tts2Rev[i],EPSILON);
		}
	}

	@Test
	public void testCANetworkDynamicFreespeedOnLinksOfDifferentWidth() {

		//free speed travel time must be independent of link width
		double linkLength = 100;
		//tt should be:
		int numOfCells = (int) (linkLength*CANetworkDynamic.RHO_HAT+0.5);
		double freeSpeedCellTravelTime = 1/(CANetworkDynamic.RHO_HAT*CANetworkDynamic.V_HAT);
		double travelTime = numOfCells * freeSpeedCellTravelTime;

		double t1 = freespeedForLinkOfXmWidth(1,linkLength);
		assertEquals(travelTime, t1, EPSILON);

		double t061 = freespeedForLinkOfXmWidth(0.61,linkLength);
		assertEquals(travelTime, t061, CA_TT_EPSILON);

		double t2 = freespeedForLinkOfXmWidth(2,linkLength);
		assertEquals(travelTime, t2, CA_TT_EPSILON);

		double t5 = freespeedForLinkOfXmWidth(5,linkLength);
		assertEquals(travelTime, t5, CA_TT_EPSILON);

		double t1Rev = freespeedForLinkOfXmWidthRev(1,linkLength);
		assertEquals(t1, t1Rev, EPSILON);

		double t061Rev = freespeedForLinkOfXmWidthRev(0.61,linkLength);
		assertEquals(t061, t061Rev, EPSILON);

		double t2Rev = freespeedForLinkOfXmWidthRev(2,linkLength);
		assertEquals(t2, t2Rev, EPSILON);

		double t5Rev = freespeedForLinkOfXmWidthRev(5,linkLength);
		assertEquals(t5, t5Rev, EPSILON);
	}

	private double[] getAgentTravelTimesForLinkOfWidthAndLengthRev(double width,
			double linkLength) {
		Scenario sc = createScenario();
		Network net = sc.getNetwork();
		for (Link l : net.getLinks().values()) {
			l.setCapacity(width);
		}

		EventsManager em = new EventsManagerImpl();
		Monitor m = new Monitor();
		em.addHandler(m);
		CANetworkDynamic caNet = new CANetworkDynamic(net, em);

		List<Link> links = getDSRoute(net);
		links.get(1).setLength(linkLength);
		Collections.reverse(links);
		CALink caLink = caNet.getCALink(links.get(0).getId());
		CAAgent[] particles = caLink.getParticles();

		for (int i = 0; i < 10; i++) {
			CAAgent a = new CASimpleDynamicAgent(links, 1, Id.create(i, CAAgent.class), caLink);
			CASimAgentConstructEvent ee = new CASimAgentConstructEvent(0, a);
			em.processEvent(ee);
			a.materialize(particles.length-1-i, -1);
			particles[particles.length-1-i] = a;
			CAEvent e = new CAEvent(0, a,caLink, CAEventType.TTA);
			caNet.pushEvent(e);
		}

		caNet.runUntil(3*linkLength);

		Link ll = links.get(1);
		Link llRev = null;
		for (Link cand : ll.getToNode().getOutLinks().values()) {
			if (cand.getToNode() == ll.getFromNode()) {
				llRev = cand;
				break;
			}
		}


		double [] tt = new double [10];

		for (int i = 0; i < 10; i++) {

			tt[i] = m.getAgentTravelTimeOnLink(Id.create(i, CAAgent.class), llRev.getId());
			log.info("Travel time for agent:" + i + " was \t" + tt[i]);
		}

		return tt;

	}
	private double[] getAgentsBidirectionalTravelTimesForLinkOfWidthAngLength(
			double width, double linkLength, int nrAgents) {
		Scenario sc = createScenario();
		Network net = sc.getNetwork();
		for (Link l : net.getLinks().values()) {
			l.setCapacity(width);
			l.setLength(linkLength);
		}

		EventsManager em = new EventsManagerImpl();
		Monitor m = new Monitor();
		em.addHandler(m);
		CANetworkDynamic caNet = new CANetworkDynamic(net, em);



		//		//DEBUG
		//		//VIS only
		//		Sim2DConfig conf2d = Sim2DConfigUtils.createConfig();
		//		Sim2DScenario sc2d = Sim2DScenarioUtils.createSim2dScenario(conf2d);
		//		sc.addScenarioElement(Sim2DScenario.ELEMENT_NAME,sc2d);
		//		sc.getConfig().global().setCoordinateSystem("EPSG:3395");
		//		EventBasedVisDebuggerEngine vis = new EventBasedVisDebuggerEngine(sc);
		//		em.addHandler(vis);
		//		QSimDensityDrawer qDbg = new QSimDensityDrawer(sc);
		//		em.addHandler(qDbg);
		//		vis.addAdditionalDrawer(new InfoBox(vis, sc));
		//		vis.addAdditionalDrawer(qDbg);



		List<Link> linksDS = getDSRoute(net);
		linksDS.get(1).setLength(linkLength);
		List<Link> linksUS = getDSRoute(net);
		Collections.reverse(linksUS);

		int numEachSide = nrAgents/2;

		{
			CALink caLink = caNet.getCALink(linksDS.get(0).getId());
			CAAgent[] particles = caLink.getParticles();
			log.info(particles.length);
			for (int i = 0; i < numEachSide; i++) {
				CAAgent a = new CASimpleDynamicAgent(linksDS, 1, Id.create(i, CAAgent.class), caLink);
				CASimAgentConstructEvent ee = new CASimAgentConstructEvent(0, a);
				em.processEvent(ee);
				a.materialize(i, 1);
				particles[i] = a;
				CAEvent e = new CAEvent(0, a,caLink, CAEventType.TTA);
				caNet.pushEvent(e);
			}
		}
		{
			CALink caLink = caNet.getCALink(linksUS.get(0).getId());
			CAAgent[] particles = caLink.getParticles();

			for (int i = numEachSide; i < 2*numEachSide; i++) {
				CAAgent a = new CASimpleDynamicAgent(linksUS, 1, Id.create(i, CAAgent.class), caLink);
				CASimAgentConstructEvent ee = new CASimAgentConstructEvent(0, a);
				em.processEvent(ee);
				a.materialize(particles.length-1-i+numEachSide, -1);
				particles[particles.length-1-i+numEachSide] = a;
				CAEvent e = new CAEvent(0, a,caLink, CAEventType.TTA);
				caNet.pushEvent(e);
			}


		}

		caNet.runUntil(3*linkLength);
		double [] tt = new double [2*numEachSide];
		Link ll = linksUS.get(1);
		Link llRev = null;
		for (Link cand : ll.getToNode().getOutLinks().values()) {
			if (cand.getToNode() == ll.getFromNode()) {
				llRev = cand;
				break;
			}
		}
		for (int i = 0; i < numEachSide; i++) {
			tt[i] = m.getAgentTravelTimeOnLink(Id.create(i, CAAgent.class), linksDS.get(1).getId());
			log.info("Travel time for agent:" + i + " was \t" + tt[i]);
		}

		for (int i = numEachSide; i < 2*numEachSide; i++) {
			tt[i] = m.getAgentTravelTimeOnLink(Id.create(i, CAAgent.class), llRev.getId());
			log.info("Travel time for agent:" + i + " was \t" + tt[i]);
		}

		return tt;
	}

	private double[] getAgentTravelTimesForLinkOfWidthAndLength(double width,
			double linkLength) {
		Scenario sc = createScenario();
		Network net = sc.getNetwork();
		for (Link l : net.getLinks().values()) {
			l.setCapacity(width);
		}

		EventsManager em = new EventsManagerImpl();
		Monitor m = new Monitor();
		em.addHandler(m);
		CANetworkDynamic caNet = new CANetworkDynamic(net, em);

		List<Link> links = getDSRoute(net);
		links.get(1).setLength(linkLength);

		CALink caLink = caNet.getCALink(links.get(0).getId());
		CAAgent[] particles = caLink.getParticles();

		for (int i = 0; i < 10; i++) {
			CAAgent a = new CASimpleDynamicAgent(links, 1, Id.create(i, CAAgent.class), caLink);
			CASimAgentConstructEvent ee = new CASimAgentConstructEvent(0, a);
			em.processEvent(ee);
			a.materialize(i, 1);
			particles[i] = a;
			CAEvent e = new CAEvent(0, a,caLink, CAEventType.TTA);
			caNet.pushEvent(e);
		}

		caNet.runUntil(3*linkLength);
		double [] tt = new double [10];

		for (int i = 0; i < 10; i++) {

			tt[i] = m.getAgentTravelTimeOnLink(Id.create(i, CAAgent.class), links.get(1).getId());
			log.info("Travel time for agent:" + i + " was \t" + tt[i]);
		}

		return tt;
	}

	private double freespeedForLinkOfXmWidth(double width, double linkLength) {
		Scenario sc = createScenario();
		Network net = sc.getNetwork();
		for (Link l : net.getLinks().values()) {
			l.setCapacity(width);
		}

		EventsManager em = new EventsManagerImpl();
		Monitor m = new Monitor();
		em.addHandler(m);
		CANetworkDynamic caNet = new CANetworkDynamic(net, em);

		List<Link> links = getDSRoute(net);
		links.get(1).setLength(linkLength);		

		CALink caLink = caNet.getCALink(links.get(0).getId());
		CAAgent[] particles = caLink.getParticles();
		CAAgent a = new CASimpleDynamicAgent(links, 1, Id.create("0", CAAgent.class), caLink);
		CASimAgentConstructEvent ee = new CASimAgentConstructEvent(0, a);
		em.processEvent(ee);
		a.materialize(0, 1);
		particles[0] = a;
		CAEvent e = new CAEvent(0, a,caLink, CAEventType.TTA);
		caNet.pushEvent(e);
		caNet.runUntil(3*linkLength);

		double tt = m.getAgentTravelTimeOnLink(a.getId(), links.get(1).getId());
		return tt;
	}


	private double freespeedForLinkOfXmWidthRev(double width, double linkLength) {
		Scenario sc = createScenario();
		Network net = sc.getNetwork();
		for (Link l : net.getLinks().values()) {
			l.setCapacity(width);
		}

		EventsManager em = new EventsManagerImpl();
		Monitor m = new Monitor();
		em.addHandler(m);
		CANetworkDynamic caNet = new CANetworkDynamic(net, em);

		List<Link> links = getDSRoute(net);
		links.get(1).setLength(linkLength);		
		Collections.reverse(links);

		CALink caLink = caNet.getCALink(links.get(0).getId());
		CAAgent[] particles = caLink.getParticles();
		CAAgent a = new CASimpleDynamicAgent(links, 1, Id.create("0", CAAgent.class), caLink);
		CASimAgentConstructEvent ee = new CASimAgentConstructEvent(0, a);
		em.processEvent(ee);
		a.materialize(particles.length-1, -1);
		particles[particles.length-1] = a;
		CAEvent e = new CAEvent(0, a,caLink, CAEventType.TTA);
		caNet.pushEvent(e);
		caNet.runUntil(3*linkLength);


		Link ll = links.get(1);
		Link llRev = null;
		for (Link cand : ll.getToNode().getOutLinks().values()) {
			if (cand.getToNode() == ll.getFromNode()) {
				llRev = cand;
				break;
			}
		}

		double tt = m.getAgentTravelTimeOnLink(a.getId(),llRev.getId());
		return tt;
	}



	private List<Link> getDSRoute(Network net) {
		ArrayList<Link> links = new ArrayList<Link>();
		Link l0 = net.getLinks().get(Id.createLinkId("0"));
		Link l2 = net.getLinks().get(Id.createLinkId("2"));
		Link l4 = net.getLinks().get(Id.createLinkId("4"));
		links.add(l0);links.add(l2);links.add(l4);
		return links;
	}





	private Scenario createScenario() {
		Config c = ConfigUtils.createConfig();
		Scenario sc = ScenarioUtils.createScenario(c);
		Network net = sc.getNetwork();
		NetworkFactory fac = net.getFactory();

		Node n0 = fac.createNode(Id.create("0", Node.class), sc.createCoord(-10, 0));
		Node n1 = fac.createNode(Id.create("1", Node.class), sc.createCoord(0, 0));
		Node n2 = fac.createNode(Id.create("2", Node.class), sc.createCoord(10, 0));
		Node n3 = fac.createNode(Id.create("3", Node.class), sc.createCoord(20, 0));
		net.addNode(n0);
		net.addNode(n1);
		net.addNode(n2);
		net.addNode(n3);

		Link l0 = fac.createLink(Id.create("0", Link.class), n0, n1);
		Link l1 = fac.createLink(Id.create("1", Link.class), n1, n0);
		Link l2 = fac.createLink(Id.create("2", Link.class), n1, n2);
		Link l3 = fac.createLink(Id.create("3", Link.class), n2, n1);
		Link l4 = fac.createLink(Id.create("4", Link.class), n2, n3);
		Link l5 = fac.createLink(Id.create("5", Link.class), n3, n2);

		net.addLink(l0);
		net.addLink(l1);
		net.addLink(l2);
		net.addLink(l3);
		net.addLink(l4);
		net.addLink(l5);

		for (Link l : net.getLinks().values()) {
			l.setLength(10);
		}
		l2.setLength(100);
		l3.setLength(100);

		return sc;
	}


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
