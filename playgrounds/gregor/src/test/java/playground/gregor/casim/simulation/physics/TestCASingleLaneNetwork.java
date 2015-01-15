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

public class TestCASingleLaneNetwork extends MatsimTestCase {

	private static final double CA_TT_EPSILON = 1; // due to integer inaccuracy
													// EPSILON needs to be
													// relaxed

	private static final Logger log = Logger
			.getLogger(TestCASingleLaneNetwork.class);

	@Test
	public void testCANetoworkDynamicSpacingsComputation() {
		log.warn("Test needs to be re-written. Exiting!");
		if (true)
			return;

		Scenario sc = createScenario(20, 20);
		Network net = sc.getNetwork();
		for (Link l : net.getLinks().values()) {
			l.setCapacity(.61);
		}
		EventsManager em = new EventsManagerImpl();
		CANetworkFactory fac = new CASingleLaneNetworkFactory();
		CANetwork caNet = fac.createCANetwork(net, em, null);

		List<Link> links = new ArrayList<Link>();
		links.add(net.getLinks().get(Id.createLinkId("2")));
		links.add(net.getLinks().get(Id.createLinkId("4")));

		CASingleLaneLink caLink = (CASingleLaneLink) caNet.getCALink(Id
				.createLinkId("2"));
		CAMoveableEntity[] parts = caLink.getParticles();
		// test: uni dir 50% load
		// x <--
		// o -->
		// * --> (particle under investigation
		// _ empty cell
		// ... o_o_o_o_o_*_o_o_o_o_o_o_ooo ...
		//
		int pos = (parts.length - 1) / 2;
		int dir = 1;
		parts[pos] = createAgent(links, "0", pos, dir, caLink);
		parts[pos - 2] = createAgent(links, "1", pos - 2, dir, caLink);
		parts[pos - 4] = createAgent(links, "2", pos - 4, dir, caLink);
		parts[pos - 6] = createAgent(links, "3", pos - 6, dir, caLink);
		parts[pos - 8] = createAgent(links, "4", pos - 8, dir, caLink);
		parts[pos - 10] = createAgent(links, "5", pos - 10, dir, caLink);
		parts[pos + 2] = createAgent(links, "6", pos + 2, dir, caLink);
		parts[pos + 4] = createAgent(links, "7", pos + 4, dir, caLink);
		parts[pos + 6] = createAgent(links, "8", pos + 6, dir, caLink);
		parts[pos + 8] = createAgent(links, "9", pos + 8, dir, caLink);
		parts[pos + 10] = createAgent(links, "10", pos + 10, dir, caLink);
		parts[pos + 12] = createAgent(links, "11", pos + 12, dir, caLink);
		parts[pos + 14] = createAgent(links, "12", pos + 14, dir, caLink);
		parts[pos + 15] = createAgent(links, "13", pos + 15, dir, caLink);
		parts[pos + 16] = createAgent(links, "14", pos + 16, dir, caLink);
		for (int i = 0; i < parts.length; i++) {
			if (parts[i] != null) {
				caNet.registerAgent(parts[i]);
			}
		}
		((AbstractCANetwork) caNet).updateRho();

		double rho50 = parts[pos].getRho();
		assertEquals((0 + AbstractCANetwork.RHO_HAT / 2) / 2, rho50);

		// just to make sure we don't produce Heisenbugs
		rho50 = parts[pos].getRho();
		assertEquals((0 + AbstractCANetwork.RHO_HAT / 2) / 2, rho50);

		((AbstractCANetwork) caNet).updateRho();
		rho50 = parts[pos].getRho();
		assertEquals(
				(AbstractCANetwork.RHO_HAT / 4 + AbstractCANetwork.RHO_HAT / 2) / 2,
				rho50);
		caLink.reset();
		// test: uni dir 100% load
		// x <--
		// o -->
		// * --> (particle under investigation
		// _ empty cell
		// ... ooooo*ooooooo_o ...
		//
		parts[pos] = createAgent(links, "0", pos, dir, caLink);
		parts[pos - 1] = createAgent(links, "1", pos - 1, dir, caLink);
		parts[pos - 2] = createAgent(links, "2", pos - 2, dir, caLink);
		parts[pos - 3] = createAgent(links, "3", pos - 3, dir, caLink);
		parts[pos - 4] = createAgent(links, "4", pos - 4, dir, caLink);
		parts[pos - 5] = createAgent(links, "5", pos - 5, dir, caLink);
		parts[pos + 1] = createAgent(links, "6", pos + 1, dir, caLink);
		parts[pos + 2] = createAgent(links, "7", pos + 2, dir, caLink);
		parts[pos + 3] = createAgent(links, "8", pos + 3, dir, caLink);
		parts[pos + 4] = createAgent(links, "9", pos + 4, dir, caLink);
		parts[pos + 5] = createAgent(links, "10", pos + 5, dir, caLink);
		parts[pos + 6] = createAgent(links, "11", pos + 6, dir, caLink);
		parts[pos + 7] = createAgent(links, "12", pos + 7, dir, caLink);
		parts[pos + 9] = createAgent(links, "13", pos + 9, dir, caLink);
		for (int i = 0; i < parts.length; i++) {
			if (parts[i] != null) {
				caNet.registerAgent(parts[i]);
			}
		}
		((AbstractCANetwork) caNet).updateRho();
		double rho100 = parts[pos].getRho();
		assertEquals(AbstractCANetwork.RHO_HAT / 2, rho100);
		rho100 = parts[pos].getRho();
		assertEquals(AbstractCANetwork.RHO_HAT / 2, rho100);
		((AbstractCANetwork) caNet).updateRho();
		rho100 = parts[pos].getRho();
		assertEquals(
				(AbstractCANetwork.RHO_HAT / 2 + AbstractCANetwork.RHO_HAT) / 2,
				rho100);
		((AbstractCANetwork) caNet).updateRho();
		caLink.reset();

		// test: uni dir 5/20 load (when looking ahead until the 7th agent
		// (including) or 20th cell (including) whatever comes first)
		// x <--
		// o -->
		// * --> (particle under investigation
		// _ empty cell
		// ...o*_oo_o____oo___o ...
		//
		parts[pos] = createAgent(links, "0", pos, dir, caLink);
		parts[pos - 1] = createAgent(links, "1", pos - 1, dir, caLink);
		parts[pos - 2] = createAgent(links, "2", pos - 2, dir, caLink);
		parts[pos - 3] = createAgent(links, "3", pos - 3, dir, caLink);
		parts[pos - 4] = createAgent(links, "4", pos - 4, dir, caLink);
		parts[pos - 5] = createAgent(links, "5", pos - 5, dir, caLink);
		parts[pos + 2] = createAgent(links, "6", pos + 2, dir, caLink);
		parts[pos + 3] = createAgent(links, "7", pos + 3, dir, caLink);
		parts[pos + 5] = createAgent(links, "8", pos + 5, dir, caLink);
		parts[pos + 10] = createAgent(links, "9", pos + 10, dir, caLink);
		parts[pos + 11] = createAgent(links, "10", pos + 11, dir, caLink);
		parts[pos + CASimDensityEstimator.MX_TRAVERSE + 1] = createAgent(links,
				"11", pos + CASimDensityEstimator.MX_TRAVERSE + 1, dir, caLink);
		for (int i = 0; i < parts.length; i++) {
			if (parts[i] != null) {
				caNet.registerAgent(parts[i]);
			}
		}
		((AbstractCANetwork) caNet).updateRho();
		double rho5_20 = parts[pos].getRho();
		assertEquals((AbstractCANetwork.RHO_HAT * (5. / 20.)) / 2, rho5_20);
		caLink.reset();

	}

	private CAMoveableEntity createAgent(List<Link> links, String string,
			int pos, int dir, CALink caLink) {

		CAMoveableEntity a = new CASimpleDynamicAgent(links, 1, Id.create(
				string, CASimpleDynamicAgent.class), caLink);
		a.materialize(pos, dir);
		return a;
	}

	// @Test
	// public void
	// testCANetworkDynamicTTForOncomingAgentsForDifferentLinkWidths() {
	// AbstractCANetwork.NR_THREADS = 1;
	// double freeSpeedCellTravelTime = 1 / (AbstractCANetwork.RHO_HAT *
	// AbstractCANetwork.V_HAT);
	//
	// // {
	// // double linkLength = 10;//-1/CANetworkDynamic.RHO_HAT;;
	// // int nrAgents1 = 2;
	// // double w1 = 0.61;
	// // double tts1[] =
	// //
	// getAgentsBidirectionalTravelTimesForLinkOfWidthAngLength(w1,linkLength,nrAgents1);
	// // double diffSum = 0;
	// // for (int i = 0; i < tts1.length/2; i++) {
	// // log.info("diff =\t" + (tts1[i]-tts1[i+tts1.length/2]) +
	// // " max allowd:\t" + freeSpeedCellTravelTime/w1);
	// //
	// assertEquals(tts1[i],tts1[i+tts1.length/2],2*freeSpeedCellTravelTime/w1);
	// // diffSum += (tts1[i]-tts1[i+tts1.length/2]);
	// // }
	// // log.info("diffSum = \t" + diffSum);
	// // assertTrue("abs diff sum smaller than cell travel time",
	// // Math.abs(diffSum) <= freeSpeedCellTravelTime/w1+EPSILON);
	// // }
	//
	// {
	// double linkLength = 100;// -1/CANetworkDynamic.RHO_HAT;;
	// int nrAgents1 = 1000;
	// double w1 = .61;
	// double tts1[] = getAgentsBidirectionalTravelTimesForLinkOfWidthAngLength(
	// w1, linkLength, nrAgents1);
	// double diffSum = 0;
	// for (int i = 0; i < tts1.length / 2; i++) {
	// log.info("diff =\t" + (tts1[i] - tts1[i + tts1.length / 2])
	// + " max allowd:\t" + freeSpeedCellTravelTime / w1);
	// assertEquals(tts1[i], tts1[i + tts1.length / 2],
	// 3 * CA_TT_EPSILON); // TODO fixme
	// diffSum += (tts1[i] - tts1[i + tts1.length / 2]);
	// }
	// log.info("diffSum = \t" + diffSum);
	// // assertTrue("abs diff sum smaller than cell travel time",
	// // Math.abs(diffSum) <= freeSpeedCellTravelTime/w1+EPSILON);
	// }
	//
	// {
	// double linkLength = 100;// -1/CANetworkDynamic.RHO_HAT;;
	// int nrAgents1 = 10;
	// double w1 = 2.61;
	// double tts1[] = getAgentsBidirectionalTravelTimesForLinkOfWidthAngLength(
	// w1, linkLength, nrAgents1);
	// double diffSum = 0;
	// for (int i = 0; i < tts1.length / 2; i++) {
	// log.info("diff =\t" + (tts1[i] - tts1[i + tts1.length / 2])
	// + " max allowd:\t" + freeSpeedCellTravelTime / w1);
	// assertEquals(tts1[i], tts1[i + tts1.length / 2], 2
	// * freeSpeedCellTravelTime / w1);
	// diffSum += (tts1[i] - tts1[i + tts1.length / 2]);
	// }
	// log.info("diffSum = \t" + diffSum);
	// assertTrue("abs diff sum smaller than cell travel time",
	// Math.abs(diffSum) <= freeSpeedCellTravelTime / w1 + EPSILON);
	// }
	//
	// }

	@Test
	public void testCANetworkDynamicTTInBothDirectionsOfAPairOfAgentsForDifferentLinkWidths() {
		// free speed travel time must be independent of link width
		double linkLength = 100;

		double[] tts1 = getAgentTravelTimesForLinkOfWidthAndLength(1,
				linkLength);
		double[] tts1Rev = getAgentTravelTimesForLinkOfWidthAndLengthRev(1,
				linkLength);
		for (int i = 0; i < tts1.length; i++) {
			assertEquals(tts1[i], tts1Rev[i], EPSILON);
		}

		double[] tts2 = getAgentTravelTimesForLinkOfWidthAndLength(2,
				linkLength);
		double[] tts2Rev = getAgentTravelTimesForLinkOfWidthAndLengthRev(2,
				linkLength);
		for (int i = 0; i < tts1.length; i++) {
			assertEquals(tts2[i], tts2Rev[i], EPSILON);
		}
	}

	@Test
	public void testCANetworkDynamicFreespeedOnLinksOfDifferentWidth() {

		// free speed travel time must be independent of link width
		double linkLength = 100;
		// tt should be:
		int numOfCells = (int) (linkLength * AbstractCANetwork.RHO_HAT + 0.5);
		double freeSpeedCellTravelTime = 1 / (AbstractCANetwork.RHO_HAT * AbstractCANetwork.V_HAT);
		double travelTime = numOfCells * freeSpeedCellTravelTime;

		double t1 = freespeedForLinkOfXmWidth(1, linkLength);
		assertEquals(travelTime, t1, 1);

		double t061 = freespeedForLinkOfXmWidth(0.61, linkLength);
		assertEquals(travelTime, t061, CA_TT_EPSILON);

		double t2 = freespeedForLinkOfXmWidth(2, linkLength);
		assertEquals(travelTime, t2, CA_TT_EPSILON);

		double t5 = freespeedForLinkOfXmWidth(5, linkLength);
		assertEquals(travelTime, t5, CA_TT_EPSILON);

		double t1Rev = freespeedForLinkOfXmWidthRev(1, linkLength);
		assertEquals(t1, t1Rev, 1);

		double t061Rev = freespeedForLinkOfXmWidthRev(0.61, linkLength);
		assertEquals(t061, t061Rev, 1);

		double t2Rev = freespeedForLinkOfXmWidthRev(2, linkLength);
		assertEquals(t2, t2Rev, 1);

		double t5Rev = freespeedForLinkOfXmWidthRev(5, linkLength);
		assertEquals(t5, t5Rev, 1);
	}

	private double[] getAgentTravelTimesForLinkOfWidthAndLengthRev(
			double width, double linkLength) {
		Scenario sc = createScenario(10, linkLength);
		Network net = sc.getNetwork();
		for (Link l : net.getLinks().values()) {
			l.setCapacity(width);
		}

		EventsManager em = new EventsManagerImpl();
		Monitor m = new Monitor();
		em.addHandler(m);
		CANetworkFactory fac = new CASingleLaneNetworkFactory();
		CANetwork caNet = fac.createCANetwork(net, em, null);

		List<Link> links = getUSRoute(net);
		CASingleLaneLink caLink = (CASingleLaneLink) caNet.getCALink(links.get(
				0).getId());
		CAMoveableEntity[] particles = caLink.getParticles();

		for (int i = 0; i < 10; i++) {
			CAMoveableEntity a = new CASimpleDynamicAgent(links, 1, Id.create(
					i, CASimpleDynamicAgent.class), caLink);
			CASimAgentConstructEvent ee = new CASimAgentConstructEvent(0, a);
			em.processEvent(ee);
			a.materialize(particles.length - 1 - i, -1);
			particles[particles.length - 1 - i] = a;
			CAEvent e = new CAEvent(0, a, caLink, CAEventType.TTA);
			caNet.pushEvent(e);
			caNet.registerAgent(a);
		}

		caNet.run(3600);

		double[] tt = new double[10];

		for (int i = 0; i < 10; i++) {

			tt[i] = m.getAgentTravelTimeOnLink(Id.create(i,
					CASimpleDynamicAgent.class), links.get(1).getId());
			log.info("Travel time for agent:" + i + " was \t" + tt[i]);
		}

		return tt;

	}

	private double[] getAgentsBidirectionalTravelTimesForLinkOfWidthAngLength(
			double width, double linkLength, int nrAgents) {

		double cellLength = 1 / (width * AbstractCANetwork.RHO_HAT);
		double minLength = cellLength * nrAgents / 2;

		Scenario sc = createScenario(minLength, linkLength);
		Network net = sc.getNetwork();

		for (Link l : net.getLinks().values()) {
			l.setCapacity(width);
		}

		EventsManager em = new EventsManagerImpl();
		Monitor m = new Monitor();
		em.addHandler(m);
		CANetworkFactory fac = new CASingleLaneNetworkFactory();
		CANetwork caNet = fac.createCANetwork(net, em, null);

		// // DEBUG
		// // VIS only
		// CANetworkDynamic.EMIT_VIS_EVENTS = true;
		// Sim2DConfig conf2d = Sim2DConfigUtils.createConfig();
		// Sim2DScenario sc2d = Sim2DScenarioUtils.createSim2dScenario(conf2d);
		// sc.addScenarioElement(Sim2DScenario.ELEMENT_NAME, sc2d);
		// sc.getConfig().global().setCoordinateSystem("EPSG:3395");
		// EventBasedVisDebuggerEngine vis = new
		// EventBasedVisDebuggerEngine(sc);
		// em.addHandler(vis);
		// QSimDensityDrawer qDbg = new QSimDensityDrawer(sc);
		// em.addHandler(qDbg);
		// vis.addAdditionalDrawer(new InfoBox(vis, sc));
		// vis.addAdditionalDrawer(qDbg);

		List<Link> linksDS = getDSRoute(net);
		List<Link> linksUS = getUSRoute(net);

		int numEachSide = nrAgents / 2;

		{
			CASingleLaneLink caLink = (CASingleLaneLink) caNet
					.getCALink(linksDS.get(0).getId());
			CAMoveableEntity[] particles = caLink.getParticles();
			log.info(particles.length);
			for (int i = 0; i < numEachSide; i++) {
				CAMoveableEntity a = new CASimpleDynamicAgent(linksDS, 1,
						Id.create(i, CASimpleDynamicAgent.class), caLink);
				CASimAgentConstructEvent ee = new CASimAgentConstructEvent(0, a);
				em.processEvent(ee);
				a.materialize(i, 1);
				particles[i] = a;
				CAEvent e = new CAEvent(0, a, caLink, CAEventType.TTA);
				caNet.pushEvent(e);
				caNet.registerAgent(a);
			}
		}
		{
			CASingleLaneLink caLink = (CASingleLaneLink) caNet
					.getCALink(linksUS.get(0).getId());
			CAMoveableEntity[] particles = caLink.getParticles();

			for (int i = numEachSide; i < 2 * numEachSide; i++) {
				CAMoveableEntity a = new CASimpleDynamicAgent(linksUS, 1,
						Id.create(i, CASimpleDynamicAgent.class), caLink);
				CASimAgentConstructEvent ee = new CASimAgentConstructEvent(0, a);
				em.processEvent(ee);
				a.materialize(particles.length - 1 - i + numEachSide, -1);
				particles[particles.length - 1 - i + numEachSide] = a;
				CAEvent e = new CAEvent(0, a, caLink, CAEventType.TTA);
				caNet.pushEvent(e);
				caNet.registerAgent(a);
			}

		}

		caNet.run(3600);// (3*linkLength + 3*minLength);
		double[] tt = new double[2 * numEachSide];
		Link ll = linksUS.get(1);
		Link llRev = linksUS.get(1);
		for (int i = 0; i < numEachSide; i++) {
			tt[i] = m.getAgentTravelTimeOnLink(Id.create(i,
					CASimpleDynamicAgent.class), linksDS.get(1).getId());
			log.info("Travel time for agent:" + i + " was \t" + tt[i]);
		}

		for (int i = numEachSide; i < 2 * numEachSide; i++) {
			tt[i] = m.getAgentTravelTimeOnLink(
					Id.create(i, CASimpleDynamicAgent.class), llRev.getId());
			log.info("Travel time for agent:" + i + " was \t" + tt[i]);
		}

		return tt;
	}

	private double[] getAgentTravelTimesForLinkOfWidthAndLength(double width,
			double linkLength) {
		Scenario sc = createScenario(10, linkLength);
		Network net = sc.getNetwork();
		for (Link l : net.getLinks().values()) {
			l.setCapacity(width);
		}

		EventsManager em = new EventsManagerImpl();
		Monitor m = new Monitor();
		em.addHandler(m);
		CANetworkFactory fac = new CASingleLaneNetworkFactory();
		CANetwork caNet = fac.createCANetwork(net, em, null);

		List<Link> links = getDSRoute(net);

		CASingleLaneLink caLink = (CASingleLaneLink) caNet.getCALink(links.get(
				0).getId());
		CAMoveableEntity[] particles = caLink.getParticles();

		for (int i = 0; i < 10; i++) {
			CAMoveableEntity a = new CASimpleDynamicAgent(links, 1, Id.create(
					i, CASimpleDynamicAgent.class), caLink);
			CASimAgentConstructEvent ee = new CASimAgentConstructEvent(0, a);
			em.processEvent(ee);
			a.materialize(i, 1);
			particles[i] = a;
			CAEvent e = new CAEvent(0, a, caLink, CAEventType.TTA);
			caNet.pushEvent(e);
			caNet.registerAgent(a);
		}

		caNet.run(3600);
		double[] tt = new double[10];

		for (int i = 0; i < 10; i++) {

			tt[i] = m.getAgentTravelTimeOnLink(Id.create(i,
					CASimpleDynamicAgent.class), links.get(1).getId());
			log.info("Travel time for agent:" + i + " was \t" + tt[i]);
		}

		return tt;
	}

	private double freespeedForLinkOfXmWidth(double width, double linkLength) {
		Scenario sc = createScenario(10, linkLength);
		Network net = sc.getNetwork();
		for (Link l : net.getLinks().values()) {
			l.setCapacity(width);
		}

		EventsManager em = new EventsManagerImpl();
		Monitor m = new Monitor();
		em.addHandler(m);
		CANetworkFactory fac = new CASingleLaneNetworkFactory();
		CANetwork caNet = fac.createCANetwork(net, em, null);

		List<Link> links = getDSRoute(net);

		CASingleLaneLink caLink = (CASingleLaneLink) caNet.getCALink(links.get(
				0).getId());
		CAMoveableEntity[] particles = caLink.getParticles();
		CAMoveableEntity a = new CASimpleDynamicAgent(links, 1, Id.create("0",
				CASimpleDynamicAgent.class), caLink);
		CASimAgentConstructEvent ee = new CASimAgentConstructEvent(0, a);
		em.processEvent(ee);
		a.materialize(0, 1);
		particles[0] = a;
		CAEvent e = new CAEvent(0, a, caLink, CAEventType.TTA);
		caNet.pushEvent(e);
		caNet.registerAgent(a);
		caNet.run(3600);

		double tt = m.getAgentTravelTimeOnLink(a.getId(), links.get(1).getId());
		return tt;
	}

	private double freespeedForLinkOfXmWidthRev(double width, double linkLength) {
		Scenario sc = createScenario(10, linkLength);
		Network net = sc.getNetwork();
		for (Link l : net.getLinks().values()) {
			l.setCapacity(width);
		}

		EventsManager em = new EventsManagerImpl();
		Monitor m = new Monitor();
		em.addHandler(m);
		CANetworkFactory fac = new CASingleLaneNetworkFactory();
		CANetwork caNet = fac.createCANetwork(net, em, null);

		List<Link> links = getUSRoute(net);

		CASingleLaneLink caLink = (CASingleLaneLink) caNet.getCALink(links.get(
				0).getId());
		CAMoveableEntity[] particles = caLink.getParticles();
		CAMoveableEntity a = new CASimpleDynamicAgent(links, 1, Id.create("0",
				CASimpleDynamicAgent.class), caLink);
		CASimAgentConstructEvent ee = new CASimAgentConstructEvent(0, a);
		em.processEvent(ee);
		a.materialize(particles.length - 1, -1);
		particles[particles.length - 1] = a;
		CAEvent e = new CAEvent(0, a, caLink, CAEventType.TTA);
		caNet.pushEvent(e);
		caNet.registerAgent(a);
		caNet.run(3600);

		Link ll = links.get(1);

		double tt = m.getAgentTravelTimeOnLink(a.getId(), ll.getId());
		return tt;
	}

	private List<Link> getDSRoute(Network net) {
		ArrayList<Link> links = new ArrayList<Link>();
		Link l0 = net.getLinks().get(Id.createLinkId("0"));
		Link l2 = net.getLinks().get(Id.createLinkId("2"));
		Link l4 = net.getLinks().get(Id.createLinkId("4"));
		links.add(l0);
		links.add(l2);
		links.add(l4);
		return links;
	}

	private List<Link> getUSRoute(Network net) {
		ArrayList<Link> links = new ArrayList<Link>();
		Link l0 = net.getLinks().get(Id.createLinkId("5"));
		Link l2 = net.getLinks().get(Id.createLinkId("3"));
		Link l4 = net.getLinks().get(Id.createLinkId("1"));
		links.add(l0);
		links.add(l2);
		links.add(l4);
		return links;
	}

	private Scenario createScenario(double minLength, double linkLength) {
		Config c = ConfigUtils.createConfig();
		Scenario sc = ScenarioUtils.createScenario(c);
		Network net = sc.getNetwork();
		NetworkFactory fac = net.getFactory();

		Node n0 = fac.createNode(Id.create("0", Node.class),
				sc.createCoord(0, 0));
		Node n1 = fac.createNode(Id.create("1", Node.class),
				sc.createCoord(minLength, 0));
		Node n2 = fac.createNode(Id.create("2", Node.class),
				sc.createCoord(minLength + linkLength, 0));
		Node n3 = fac.createNode(Id.create("3", Node.class),
				sc.createCoord(minLength + linkLength + minLength, 0));
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
			l.setLength(minLength);
		}
		l2.setLength(linkLength);
		l3.setLength(linkLength);

		return sc;
	}

	private final class Monitor implements LinkEnterEventHandler,
			LinkLeaveEventHandler {
		Map<Id, Map<Id, AI>> infos = new HashMap<Id, Map<Id, AI>>();

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
				map = new HashMap<Id, AI>();
				this.infos.put(event.getLinkId(), map);
			}
			AI ai = new AI();
			map.put(event.getVehicleId(), ai);
			ai.enterTime = event.getTime();
		}

		public double getAgentTravelTimeOnLink(
				Id<CASimpleDynamicAgent> agentId, Id linkId) {
			Map<Id, AI> map = this.infos.get(linkId);
			if (map != null) {
				AI ai = map.get(agentId);
				if (ai != null) {
					return ai.leaveTime - ai.enterTime;
				}
			}
			return Double.NaN;
		}

		public double getAgentLinkExitTime(Id<CASimpleDynamicAgent> agentId,
				Id linkId) {
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
