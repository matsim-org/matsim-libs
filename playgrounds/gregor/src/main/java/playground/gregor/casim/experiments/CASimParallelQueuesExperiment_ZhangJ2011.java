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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
import org.matsim.core.network.NodeImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;

import playground.gregor.casim.events.CASimAgentConstructEvent;
import playground.gregor.casim.monitoring.CALinkMonitorExact;
import playground.gregor.casim.monitoring.Monitor;
import playground.gregor.casim.simulation.physics.AbstractCANetwork;
import playground.gregor.casim.simulation.physics.CAEvent;
import playground.gregor.casim.simulation.physics.CAEvent.CAEventType;
import playground.gregor.casim.simulation.physics.CAMoveableEntity;
import playground.gregor.casim.simulation.physics.CAMultiLaneLink;
import playground.gregor.casim.simulation.physics.CAMultiLaneNetworkFactory;
import playground.gregor.casim.simulation.physics.CANetwork;
import playground.gregor.casim.simulation.physics.CANetworkFactory;
import playground.gregor.casim.simulation.physics.CASimDensityEstimator;
import playground.gregor.casim.simulation.physics.CASimpleDynamicAgent;
import playground.gregor.casim.simulation.physics.CASingleLaneNetworkFactory;
import playground.gregor.sim2d_v4.debugger.eventsbaseddebugger.EventBasedVisDebuggerEngine;
import playground.gregor.sim2d_v4.debugger.eventsbaseddebugger.InfoBox;
import playground.gregor.sim2d_v4.debugger.eventsbaseddebugger.QSimDensityDrawer;
import playground.gregor.sim2d_v4.scenario.Sim2DConfig;
import playground.gregor.sim2d_v4.scenario.Sim2DConfigUtils;
import playground.gregor.sim2d_v4.scenario.Sim2DScenario;
import playground.gregor.sim2d_v4.scenario.Sim2DScenarioUtils;

public class CASimParallelQueuesExperiment_ZhangJ2011 {

	// BFR-DML-360 exp
	private static final double ESPILON = 0.1;
	private static final double B_r = 20;

	// 0.50 0.50 0.65 0.65 0.75 0.75 0.85 0.85 1.00 1.00 0.50 0.50 0.75 0.75
	// 0.90 0.90 1.20 1.20 1.60 1.60 2.00 2.00 2.50 2.50
	private static final List<Setting> settings = new ArrayList<Setting>();
	private static final boolean USE_MULTI_LANE_MODEL = false;

	public static boolean VIS = false;
	private static BufferedWriter bw2;
	private static int it = 0;

	static {

		// settings.add(new Setting(1,1,1));
		// settings.add(new Setting(2,2,2));
		// settings.add(new Setting(10,10,1));
		// settings.add(new Setting(1,1,.9));
		// settings.add(new Setting(1,1,.8));
		// settings.add(new Setting(1,1,.7));
		// settings.add(new Setting(1,1,.61));
		// settings.add(new Setting(1,1,.5));
		// settings.add(new Setting(1,1,.1));
		// settings.add(new Setting(1,1,.01));
		// settings.add(new Setting(1,1,.001));
		// settings.add(new Setting(3.6,3.6,.2));
		// settings.add(new Setting(3.6,3.6,.5));

		//
		// for (double bL = 1.2; bL <= 4; bL += 0.5) {
		// for (double bCor = 0.61; bCor <= bL*1.2; bCor += 0.5) {
		// for (double bEx = bL; bEx >= bL*0.5; bEx *= 0.9) {
		// if (bEx > bCor) {
		// continue;
		// }
		// if (bL > bCor) {
		// continue;
		// }
		// if (bEx > bL) {
		// continue;
		// }
		// settings.add(new Setting(bL,bCor,bEx));
		// }
		// }
		// }
		// for (double bL = 1.2; bL <= 4; bL += 0.5) {
		// for (double bEx = bL; bEx >= bL*0.5; bEx *= 0.9) {
		// double bCor = bL;
		// if (bEx > bL) {
		// continue;
		// }
		// settings.add(new Setting(bL,bCor,bEx));
		// }
		// }
		// for (double bL = 1.2; bL <= 4; bL += 1.5) {
		// for (double bEx = bL*0.8; bEx >= bL*0.65; bEx -= 0.01) {
		// double bCor = bL;
		// if (bEx > bL) {
		// continue;
		// }
		// settings.add(new Setting(bL,bCor,bEx));
		// }
		// }
		//
		//
		//
		// settings.add(new Setting(4*1.8,4*1.8,4*.95));
		// settings.add(new Setting(1.8,1.8,.95));

		// //

		// settings.add(new Setting(3.,2,0.4));
		int i = 0;
		while (i < 500) {
			double r0 = MatsimRandom.getRandom().nextGaussian() + 2;
			double r1 = MatsimRandom.getRandom().nextGaussian() + 2;
			double r2 = MatsimRandom.getRandom().nextGaussian() + 2;
			if (r0 > 5 || r0 < 0.61) {
				continue;
			}
			if (r1 > 5 || r1 < 0.61) {
				continue;
			}
			if (r2 > 5 || r2 < 0.61) {
				continue;
			}
			settings.add(new Setting(r0, r1, r2 / 2));
			i++;

		}
		// settings.add(new Setting(1.8,1.8,.7));
		// settings.add(new Setting(2.4,2.4,1.0));
		// settings.add(new Setting(.5,1.8,1.8));
		// settings.add(new Setting(.6,1.8,1.8));
		// settings.add(new Setting(.7,1.8,1.8));
		// settings.add(new Setting(1.,1.8,1.8));
		// settings.add(new Setting(1.45,1.8,1.8));
		// settings.add(new Setting(1.8,1.8,1.8));
		// settings.add(new Setting(1.8,1.8,1.2));
		// settings.add(new Setting(.8,3.,3.));
		// settings.add(new Setting(1.,3.,3.));
		// settings.add(new Setting(1.8,3.,3.));
		//
		// // for (double i = 4.; i >= 0.5; i-=0.1) {
		// // settings.add(new Setting(3.6,3.6,i));
		// // }
		// // for (double i = 2.8; i >= 0.5; i-=0.1) {
		// // settings.add(new Setting(3.6,2.4,i));
		// // }
		// // for (double i = 4.4; i >= 0.5; i-=0.1) {
		// // settings.add(new Setting(3.6,4,i));
		// // }
		// settings.add(new Setting(1.8,1.8,1.15));
		// settings.add(new Setting(1.8,1.8,1.1));
		// settings.add(new Setting(1.8,1.8,1.05));
		// settings.add(new Setting(1.8,1.8,1.0));
		// settings.add(new Setting(3.6,3.6,2.1));
		// settings.add(new Setting(1.8,1.8,0.95));

		// settings.add(new Setting(3.6,3.6,1.6));
		// settings.add(new Setting(1.8,1.8,.7));
		// settings.add(new Setting(3.,3.,1.5));
		// settings.add(new Setting(1.8,1.8,.7));
		// settings.add(new Setting(2.4,2.4,1.0));
		// settings.add(new Setting(.5,1.8,1.8));
		// settings.add(new Setting(.6,1.8,1.8));
		// settings.add(new Setting(.7,1.8,1.8));
		// settings.add(new Setting(1.,1.8,1.8));
		// settings.add(new Setting(1.45,1.8,1.8));
		// settings.add(new Setting(1.8,1.8,1.8));
		// settings.add(new Setting(1.8,1.8,1.2));
		// settings.add(new Setting(.65,2.4,2.4));
		// settings.add(new Setting(.8,2.4,2.4));
		// settings.add(new Setting(.95,2.4,2.4));
		// settings.add(new Setting(1.45,2.4,2.4));
		// settings.add(new Setting(1.9,2.4,2.4));
		// settings.add(new Setting(2.4,2.4,2.4));
		// settings.add(new Setting(2.4,2.4,1.6));
		// settings.add(new Setting(2.4,2.4,1.3));
		// settings.add(new Setting(.8,3.,3.));
		// settings.add(new Setting(1.,3.,3.));
		// settings.add(new Setting(1.8,3.,3.));
		// settings.add(new Setting(2.4,3.,3.));
		// settings.add(new Setting(3.,3.,3.));
		// settings.add(new Setting(3.,3.,1.6));
		// settings.add(new Setting(3.,3.,1.2));
		// for (double bl = 0.61; bl <= 3.61; bl += 0.5) {
		// for (double bCor = 0.61; bCor <= 3.61; bCor += 0.5) {
		// for (double bEx = 0.61; bEx < 3.61; bEx += 0.5) {
		// if (bEx > bCor) {
		// continue;
		// }
		// settings.add(new Setting(bl,bCor,bEx));
		// }
		// }
		// }

	}

	public static final class Setting {
		public Setting(double bl, double bCor, double bEx) {
			this.bL = bl;
			this.bCor = bCor;
			this.bEx = bEx;
		}

		public double bL;
		public double bCor;
		public double bEx;
	}

	public static void main(String[] args) throws IOException {

		if (VIS) {
			AbstractCANetwork.EMIT_VIS_EVENTS = true;
		}

		double timeOffset = 0;

		for (int R = 6; R <= 6; R++) {
			CASimDensityEstimator.LOOK_AHEAD = R;
			try {
				bw2 = new BufferedWriter(new FileWriter(new File(
						"/Users/laemmel/devel/bipedca/plot_dynamicIII/sp_avg_zhangJ2011"
								+ R)));
			} catch (IOException e) {
				e.printStackTrace();
			}

			for (Setting s : settings) {

				Config c = ConfigUtils.createConfig();
				c.global().setCoordinateSystem("EPSG:3395");
				Scenario sc = ScenarioUtils.createScenario(c);

				// VIS only
				Sim2DConfig conf2d = Sim2DConfigUtils.createConfig();
				Sim2DScenario sc2d = Sim2DScenarioUtils
						.createSim2dScenario(conf2d);
				sc.addScenarioElement(Sim2DScenario.ELEMENT_NAME, sc2d);

				Network net = sc.getNetwork();
				((NetworkImpl) net).setCapacityPeriod(1);
				NetworkFactory fac = net.getFactory();

				Node n0 = fac.createNode(Id.createNodeId("0"), new CoordImpl(
						-100, 0));
				Node n1 = fac.createNode(Id.createNodeId("1"), new CoordImpl(0,
						0));
				Node n2 = fac.createNode(Id.createNodeId("2"), new CoordImpl(4,
						0));
				Node n2ex = fac.createNode(Id.createNodeId("2ex"),
						new CoordImpl(4, 100));

				Node n3 = fac.createNode(Id.createNodeId("3"), new CoordImpl(
						12, 0));

				Node n3ex1 = fac.createNode(Id.createNodeId("3ex1"),
						new CoordImpl(12, -1));
				Node n3ex2 = fac.createNode(Id.createNodeId("3ex2"),
						new CoordImpl(12, -2));
				Node n3ex3 = fac.createNode(Id.createNodeId("3ex3"),
						new CoordImpl(12, -97));
				Node n3ex4 = fac.createNode(Id.createNodeId("3ex4"),
						new CoordImpl(12, -100));

				Node n4 = fac.createNode(Id.createNodeId("4"), new CoordImpl(
						16, 0));
				Node n5 = fac.createNode(Id.createNodeId("5"), new CoordImpl(
						116, 0));

				net.addNode(n3ex1);
				net.addNode(n3ex2);
				net.addNode(n3ex3);
				net.addNode(n3ex4);
				net.addNode(n2ex);
				net.addNode(n5);
				net.addNode(n4);
				net.addNode(n3);
				net.addNode(n2);
				net.addNode(n1);
				net.addNode(n0);
				// net.addNode(n2a); net.addNode(n2b);

				Link l0 = fac.createLink(Id.createLinkId("0"), n0, n1);
				Link l0rev = fac.createLink(Id.createLinkId("0rev"), n1, n0);
				Link l1 = fac.createLink(Id.createLinkId("1"), n1, n2);
				Link l1rev = fac.createLink(Id.createLinkId("1rev"), n2, n1);
				Link l2 = fac.createLink(Id.createLinkId("2"), n2, n3);
				Link l2rev = fac.createLink(Id.createLinkId("2rev"), n3, n2);

				Link l2ex = fac.createLink(Id.createLinkId("2ex"), n2, n2ex);
				Link l3 = fac.createLink(Id.createLinkId("3"), n3, n4);
				Link l3ex1 = fac.createLink(Id.createLinkId("3ex1"), n3, n3ex1);
				Link l3ex2 = fac.createLink(Id.createLinkId("3ex2"), n3ex1,
						n3ex2);
				Link l3ex3 = fac.createLink(Id.createLinkId("3ex3"), n3ex2,
						n3ex3);
				Link l3ex4 = fac.createLink(Id.createLinkId("3ex4"), n3ex3,
						n3ex4);
				Link l3rev = fac.createLink(Id.createLinkId("3rev"), n4, n3);
				Link l4 = fac.createLink(Id.createLinkId("4"), n4, n5);
				Link l4rev = fac.createLink(Id.createLinkId("4rev"), n5, n4);

				l0.setLength(100);
				l1.setLength(4);
				l2ex.setLength(100);
				l2.setLength(8);
				l3ex1.setLength(1);
				l3ex2.setLength(1);
				l3ex3.setLength(1000);
				l3ex4.setLength(1);
				l3.setLength(4);
				l4.setLength(100);

				l0rev.setLength(100);
				l1rev.setLength(4);
				l2rev.setLength(8);
				l3rev.setLength(4);
				l4rev.setLength(100);

				net.addLink(l3ex3);
				net.addLink(l3ex4);
				net.addLink(l3ex1);
				net.addLink(l3ex2);
				net.addLink(l2);
				net.addLink(l1);
				net.addLink(l0);

				double bL = s.bL;
				double bCor = s.bCor;
				double bEx = s.bEx;

				double size = 500;
				double width = bL;
				double ratio = AbstractCANetwork.PED_WIDTH / width;
				double cellLength = ratio
						/ (AbstractCANetwork.RHO_HAT * AbstractCANetwork.PED_WIDTH);
				double length = size * cellLength;

				double width2 = bEx;
				double ratio2 = AbstractCANetwork.PED_WIDTH / width2;
				double cellLength2 = ratio2
						/ (AbstractCANetwork.RHO_HAT * AbstractCANetwork.PED_WIDTH);
				double length2 = size / 2 * cellLength2;
				l3ex3.setLength(length);
				l3ex3.setCapacity(bL);

				l0.setLength(length);
				;
				l0rev.setLength(length);
				((CoordImpl) ((NodeImpl) n0).getCoord()).setX(0 - length);
				l0.setCapacity(bL);
				l1.setCapacity(bL);
				l2.setCapacity(bCor);
				l3.setCapacity(bCor);
				l4.setCapacity(B_r);
				l0rev.setCapacity(bL);
				l1rev.setCapacity(bCor);
				l2rev.setCapacity(bCor);
				l3rev.setCapacity(bCor);
				l4rev.setCapacity(B_r);
				l2ex.setCapacity(bEx);
				l3ex1.setCapacity(bEx);
				l3ex2.setCapacity(bEx);
				l3ex4.setCapacity(bEx);

				List<Link> linksLR = new ArrayList<Link>();
				linksLR.add(l0);
				linksLR.add(l1);
				linksLR.add(l2);
				linksLR.add(l3ex1);
				linksLR.add(l3ex2);
				linksLR.add(l3ex3);
				linksLR.add(l3ex4);

				System.out.println(" " + bL + " " + bCor + " " + bEx + "\n");

				runIt(net, linksLR, sc, s);

			}
			bw2.close();
		}
	}

	private static void runIt(Network net, List<Link> linksLR, Scenario sc,
			Setting s) {
		// visualization stuff
		EventsManager em = new EventsManagerImpl();
		// // // if (iter == 9)
		if (VIS) {
			EventBasedVisDebuggerEngine vis = new EventBasedVisDebuggerEngine(
					sc);
			em.addHandler(vis);
			QSimDensityDrawer qDbg = new QSimDensityDrawer(sc);
			em.addHandler(qDbg);
			vis.addAdditionalDrawer(new InfoBox(vis, sc));
			vis.addAdditionalDrawer(qDbg);
		}

		CANetworkFactory fac;
		if (USE_MULTI_LANE_MODEL) {
			fac = new CAMultiLaneNetworkFactory();

		} else {
			fac = new CASingleLaneNetworkFactory();
		}

		CANetwork caNet = fac.createCANetwork(net, em, null);

		int agents = 0;

		{
			CAMultiLaneLink caLink = (CAMultiLaneLink) caNet.getCALink(linksLR
					.get(0).getId());

			for (int lane = 0; lane < caLink.getNrLanes(); lane++) {
				CAMoveableEntity[] particles = caLink.getParticles(lane);
				System.out.println("part left:" + particles.length);
				for (int i = 0; i < particles.length - 1; i++) {
					// if (i > 0) {
					// i+=3;
					// }
					if (i >= particles.length) {
						continue;
					}
					// agents++;
					CAMoveableEntity a = new CASimpleDynamicAgent(linksLR, 1,
							Id.create(agents++, CASimpleDynamicAgent.class),
							caLink);
					a.materialize(i, 1, lane);
					particles[i] = a;
					CASimAgentConstructEvent ee = new CASimAgentConstructEvent(
							0, a);
					em.processEvent(ee);

					// LinkEnterEvent eee = new LinkEnterEvent(0, a.getId(),
					// caLink.getLink().getId(), a.getId());
					// em.processEvent(eee);
					CAEvent e = new CAEvent(
							1 / (AbstractCANetwork.V_HAT * AbstractCANetwork.RHO_HAT),
							a, caLink, CAEventType.TTA);
					caNet.pushEvent(e);
					caNet.registerAgent(a);
				}

			}
		}

		CAMultiLaneLink cal = (CAMultiLaneLink) caNet.getCALink(Id
				.createLinkId("2"));

		for (int lane = 0; lane < cal.getNrLanes(); lane++) {

			CALinkMonitorExact monitor = new CALinkMonitorExact(cal, 7.,
					cal.getParticles(lane), cal.getLaneWidth());
			caNet.addMonitor(monitor);
			monitor.init();

		}
		caNet.run(3600);
		try {
			for (Monitor monitor : caNet.getMonitors()) {
				monitor.report(bw2);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
