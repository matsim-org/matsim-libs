/* *********************************************************************** *
 * project: org.matsim.*
 * CASimTest.java
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

package playground.gregor.casim.experiments;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.scenario.ScenarioUtils;
import playground.gregor.casim.events.CASimAgentConstructEvent;
import playground.gregor.casim.monitoring.CALinkMultiLaneMonitor;
import playground.gregor.casim.monitoring.Monitor;
import playground.gregor.casim.simulation.physics.*;
import playground.gregor.casim.simulation.physics.CAEvent.CAEventType;
import playground.gregor.sim2d_v4.debugger.eventsbaseddebugger.EventBasedVisDebuggerEngine;
import playground.gregor.sim2d_v4.debugger.eventsbaseddebugger.InfoBox;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class LongChannelBiMultiLane {

	public final static boolean USE_MULTI_LANE_MODEL = true;
	private static BufferedWriter bw;
	private static boolean USE_SPH;

	public static final class Setting {

		private final double rho1;
		private final double rho2;
		private final int lanes;
		private final double sideLength;
		


		public Setting(double rho1, double rho2, int lanes, double sideLength) {
			this.rho1 = rho1;
			this.rho2 = rho2;
			this.lanes = lanes;
			this.sideLength = sideLength;
			

		}


		@Override
		public String toString() {

			return "rho1: " + this.rho1 + " rho2: " + this.rho2 + " lanes: " + this.lanes + " sideLength: " + this.sideLength;
		}
	}

	public static void main(String[] args) throws IOException {
		List<Setting> settings = new ArrayList<>();
	

		settings.add(new Setting(.5,.5,16,200));


		AbstractCANetwork.EMIT_VIS_EVENTS = true;
		USE_SPH = true;
		for (int R = 12; R <= 12; R++) {
			CASingleLaneDensityEstimatorSPH.H = R;
			CAMultiLaneDensityEstimatorSPH.H = R;
			CASingleLaneDensityEstimatorSPA.RANGE = R;
			CASingleLaneDensityEstimatorSPHII.H = R;
			CASingleLaneDensityEstimatorSPAII.RANGE = R;
			try {
				bw = new BufferedWriter(new FileWriter(new File(
						"/Users/laemmel/devel/bipedca/ant/trash" + R)));
				// bw = new BufferedWriter(
				// new FileWriter(new File(
				// "/home/laemmel/scenarios/bipedca/rho_new_uni_spl_"
				// + R)));
			} catch (IOException e) {
				e.printStackTrace();
			}

			for (Setting s : settings) {
				System.out.println(s);
				Config c = ConfigUtils.createConfig();
				c.global().setCoordinateSystem("EPSG:3395");
				Scenario sc = ScenarioUtils.createScenario(c);

//				// VIS only
//				Sim2DConfig conf2d = Sim2DConfigUtils.createConfig();
//				Sim2DScenario sc2d = Sim2DScenarioUtils
//						.createSim2dScenario(conf2d);
//				sc.addScenarioElement(Sim2DScenario.ELEMENT_NAME, sc2d);

				Network net = sc.getNetwork();
				((NetworkImpl) net).setCapacityPeriod(1);
				NetworkFactory fac = net.getFactory();

				double l = s.sideLength;
				Node n0 = fac.createNode(Id.createNodeId("0"), new Coord((double) 0, (double) 0));
				Node n1 = fac.createNode(Id.createNodeId("1"), new Coord(l, (double) 0));
				Node n2 = fac.createNode(Id.createNodeId("2"), new Coord(l + l, (double) 0));
				Node n3 = fac.createNode(Id.createNodeId("3"), new Coord(l + l + l, (double) 0));
				Node n4 = fac.createNode(Id.createNodeId("4"), new Coord(l + l + l + l, (double) 0));
				net.addNode(n4);
				net.addNode(n3);
				net.addNode(n2);
				net.addNode(n1);
				net.addNode(n0);

				Link l0 = fac.createLink(Id.createLinkId("0"), n0, n1);
				Link l0rev = fac.createLink(Id.createLinkId("0rev"), n1, n0);
				Link l1 = fac.createLink(Id.createLinkId("1"), n1, n2);
				Link l1rev = fac.createLink(Id.createLinkId("1rev"), n2, n1);
				Link l2 = fac.createLink(Id.createLinkId("2"), n2, n3);
				Link l2rev = fac.createLink(Id.createLinkId("2rev"), n3, n2);
				Link l3 = fac.createLink(Id.createLinkId("3"), n3, n4);
				Link l3rev = fac.createLink(Id.createLinkId("3rev"), n4, n3);
				
				l0.setLength(l);
				l0rev.setLength(l);
				l1.setLength(l);
				l1rev.setLength(l);
				l2rev.setLength(l);
				l2.setLength(l);
				l3rev.setLength(l);
				l3.setLength(l);

				net.addLink(l0);
				net.addLink(l0rev);
				net.addLink(l1);
				net.addLink(l1rev);
				net.addLink(l2);
				net.addLink(l2rev);
				net.addLink(l3);
				net.addLink(l3rev);

				double width = s.lanes * AbstractCANetwork.PED_WIDTH;
				l0.setCapacity(width);
				l0rev.setCapacity(width);
				l1.setCapacity(width);
				l1rev.setCapacity(width);
				l2.setCapacity(width);
				l2rev.setCapacity(width);
				l3.setCapacity(width);
				l3rev.setCapacity(width);

				List<Link> linksLR = new ArrayList<Link>();
				linksLR.add(l1);
				linksLR.add(l2);
				linksLR.add(l3);
				List<Link> linksRL = new ArrayList<Link>();
				linksRL.add(l2rev);
				linksRL.add(l1rev);
				linksRL.add(l0rev);
				runIt(net, linksLR, linksRL, sc, s);


			}
			bw.close();
		}
	}

	private static void runIt(Network net, List<Link> linksLR,
			List<Link> linksRL, Scenario sc, Setting s) {
		EventsManager em = new EventsManagerImpl();

		if (AbstractCANetwork.EMIT_VIS_EVENTS) {
			EventBasedVisDebuggerEngine vis = new EventBasedVisDebuggerEngine(
					sc);
			em.addHandler(vis);
//			QSimDensityDrawer qDbg = new QSimDensityDrawer(sc);
//			em.addHandler(qDbg);
			vis.addAdditionalDrawer(new InfoBox(vis, sc));
//			vis.addAdditionalDrawer(qDbg);
		}
		CANetworkFactory fac;
		if (USE_MULTI_LANE_MODEL) {
			fac = new CAMultiLaneNetworkFactory();
			fac.setDensityEstimatorFactory(new CAMultiLaneDensityEstimatorSPHFactory());

		} else {
			throw new RuntimeException("works only for multi lanes");
		}

		CANetwork caNet = fac.createCANetwork(net, em, null);


		double rho = s.rho1+s.rho2;
		if (rho > AbstractCANetwork.RHO_HAT) {
			throw new RuntimeException("total density must not exceed RHO_HAT");
		}



		int agents = 0;
		{
			CAMultiLaneLink caLink = (CAMultiLaneLink) caNet.getCALink(linksLR
					.get(0).getId());
			int nextLR = 1;
			int nextRL = 2;

			agents = initAgents(caLink,nextLR,nextRL,s,rho,linksLR, linksRL,caNet,agents,em);
		}
		{
			CAMultiLaneLink caLink = (CAMultiLaneLink) caNet.getCALink(linksLR
					.get(1).getId());
			int nextLR = 2;
			int nextRL = 1;

			agents = initAgents(caLink,nextLR,nextRL,s,rho,linksLR, linksRL,caNet,agents,em);
		}
		System.out.println("nr agents: " + agents);
		
		Monitor monitor = new CALinkMultiLaneMonitor(
				(CAMultiLaneLink) caNet.getCALink(Id.createLinkId("1")), 2.);

		// CALinkMonitorExact monitor = new CALinkMonitorExactIIUni(
		// caNet.getCALink(Id.createLinkId("0")), 10.,
		// ((CAMultiLaneLink) caNet.getCALink(Id.createLinkId("0")))
		// .getParticles(0), caNet.getCALink(Id.createLinkId("0"))
		// .getLink().getCapacity());
//		caNet.addMonitor(monitor);
//		monitor.init();
		caNet.run(10*3600);
//		try {
//			monitor.report(bw);
//			bw.flush();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
	}

	private static int initAgents(CAMultiLaneLink caLink, int nextLR,
			int nextRL, Setting s, double rho, List<Link> linksLR, List<Link> linksRL, CANetwork caNet, int agents, EventsManager em) {
		int lanes = caLink.getNrLanes();
		CAMoveableEntity[] particles = caLink.getParticles(0);
		
		

		for (int i = 0; i < particles.length; i++) {
			for (int lane = 0; lane < lanes; lane++) {

				if (MatsimRandom.getRandom().nextDouble() * AbstractCANetwork.RHO_HAT > rho){
					continue;
				}

				double pLR = s.rho1 /(s.rho1+s.rho2);
				int dir = -1;
				int next = nextRL;
				List<Link> links = linksRL;
				String prefix = "r";
				if (MatsimRandom.getRandom().nextDouble() <= pLR) {
					dir = 1;
					next = nextLR;
					links= linksLR;
					prefix = "g";
				}


				// agents++;
				CAMoveableEntity a = new CASimpleDynamicAgent(links, next,
						Id.create(prefix + agents++,
								CASimpleDynamicAgent.class), caLink);
				a.materialize(i, dir, lane);
				caLink.getParticles(lane)[i] = a;
				CASimAgentConstructEvent ee = new CASimAgentConstructEvent(
						0, a);
				em.processEvent(ee);

				if (dir == -1 && i > 0 && caLink.getParticles(lane)[i-1] != null) {
					CAEvent e = new CAEvent(
							1 / (AbstractCANetwork.V_HAT * AbstractCANetwork.RHO_HAT),
							a, caLink, CAEventType.SWAP);
					caNet.pushEvent(e);						
				} else {
					CAEvent e = new CAEvent(
							1 / (AbstractCANetwork.V_HAT * AbstractCANetwork.RHO_HAT),
							a, caLink, CAEventType.TTA);
					caNet.pushEvent(e);
				}
				caNet.registerAgent(a);
			}
		}
	
		return agents;
	}

}
