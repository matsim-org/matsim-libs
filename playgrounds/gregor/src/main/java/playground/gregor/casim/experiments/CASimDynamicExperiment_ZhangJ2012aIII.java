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
import org.matsim.api.core.v01.events.LinkEnterEvent;
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
import org.matsim.core.utils.geometry.CoordImpl;

import playground.gregor.casim.events.CASimAgentConstructEvent;
import playground.gregor.casim.monitoring.CALinkMonitorExact;
import playground.gregor.casim.monitoring.CALinkMonitorExactII;
import playground.gregor.casim.simulation.physics.AbstractCANetwork;
import playground.gregor.casim.simulation.physics.CAEvent;
import playground.gregor.casim.simulation.physics.CAEvent.CAEventType;
import playground.gregor.casim.simulation.physics.CAMoveableEntity;
import playground.gregor.casim.simulation.physics.CAMultiLaneNetworkFactory;
import playground.gregor.casim.simulation.physics.CANetwork;
import playground.gregor.casim.simulation.physics.CANetworkFactory;
import playground.gregor.casim.simulation.physics.CASimpleDynamicAgent;
import playground.gregor.casim.simulation.physics.CASingleLaneDensityEstimatorSPA;
import playground.gregor.casim.simulation.physics.CASingleLaneDensityEstimatorSPAFactory;
import playground.gregor.casim.simulation.physics.CASingleLaneDensityEstimatorSPAII;
import playground.gregor.casim.simulation.physics.CASingleLaneDensityEstimatorSPH;
import playground.gregor.casim.simulation.physics.CASingleLaneDensityEstimatorSPHFactory;
import playground.gregor.casim.simulation.physics.CASingleLaneDensityEstimatorSPHII;
import playground.gregor.casim.simulation.physics.CASingleLaneLink;
import playground.gregor.casim.simulation.physics.CASingleLaneNetworkFactory;
import playground.gregor.sim2d_v4.debugger.eventsbaseddebugger.EventBasedVisDebuggerEngine;
import playground.gregor.sim2d_v4.debugger.eventsbaseddebugger.InfoBox;
import playground.gregor.sim2d_v4.debugger.eventsbaseddebugger.QSimDensityDrawer;
import playground.gregor.sim2d_v4.scenario.Sim2DConfig;
import playground.gregor.sim2d_v4.scenario.Sim2DConfigUtils;
import playground.gregor.sim2d_v4.scenario.Sim2DScenario;
import playground.gregor.sim2d_v4.scenario.Sim2DScenarioUtils;

public class CASimDynamicExperiment_ZhangJ2012aIII {

	// 0.50 0.50 0.65 0.65 0.75 0.75 0.85 0.85 1.00 1.00 0.50 0.50 0.75 0.75
	// 0.90 0.90 1.20 1.20 1.60 1.60 2.00 2.00 2.50 2.50
	private static final List<Setting> settings = new ArrayList<Setting>();

	public static boolean VIS = false;

	private static final double bEx = 10;
	private static final boolean USE_MULTI_LANE_MODEL = false;

	private static boolean USE_SPH = false;

	private static BufferedWriter bw2;
	private static int it = 0;

	static {

		CASimDynamicExperiment_ZhangJ2011.VIS = VIS;

		// settings.add(new Setting(.61,.61,.61));
		// for (double w = 0.61; w <= 1; w +=10) {
		// settings.add(new Setting(w,w,w));
		//
		// }
		int i = 0;
		while (i < 1) {
			double r0 = MatsimRandom.getRandom().nextGaussian() + 2;
			if (r0 > 1.5 || r0 < 1) {
				continue;
			}
			i++;
			r0 = 1;

			settings.add(new Setting(r0, r0, r0));
		}

	}

	public static final class Setting {
		public Setting(double bl, double bCor, double bR) {
			this.bL = bl;
			this.bCor = bCor;
			this.bR = bR;
		}

		public double bL;
		public double bCor;
		public double bR;
	}

	public static void main(String[] args) throws IOException {
		USE_SPH = true;
		VIS = false;
		AbstractCANetwork.EMIT_VIS_EVENTS = VIS;
		for (int R = 12; R <= 12; R++) {
			CASingleLaneDensityEstimatorSPH.H = R;
			CASingleLaneDensityEstimatorSPHII.H = R;
			CASingleLaneDensityEstimatorSPA.RANGE = R;
			CASingleLaneDensityEstimatorSPAII.RANGE = R;
			try {
				bw2 = new BufferedWriter(new FileWriter(new File(
						"/Users/laemmel/devel/bipedca/ant/spa_zhangJ2012" + R)));
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

				double bL = s.bL;
				double bCor = s.bCor;
				double bR = s.bR;
				double bBuff = bR;

				double rCL = 1 / (AbstractCANetwork.RHO_HAT * bR);
				double rL = rCL * 30000 + 1;

				double lCL = 1 / (AbstractCANetwork.RHO_HAT * bL);
				double lL = lCL * 30000 + 1;

				double xCl = 1 / (AbstractCANetwork.RHO_HAT * bEx);
				double xL = xCl * 100 + 1;

				Node n0 = fac.createNode(Id.createNodeId("0"), new CoordImpl(
						100 - rL, 0));
				Node n1 = fac.createNode(Id.createNodeId("1"), new CoordImpl(
						100, 0));
				Node n2 = fac.createNode(Id.createNodeId("2"), new CoordImpl(
						104, 0));
				Node n3 = fac.createNode(Id.createNodeId("3"), new CoordImpl(
						112, 0));
				Node n4 = fac.createNode(Id.createNodeId("4"), new CoordImpl(
						116, 0));
				Node n5 = fac.createNode(Id.createNodeId("5"), new CoordImpl(
						116 + lL, 0));
				Node n2ex = fac.createNode(Id.createNodeId("n2ex"),
						new CoordImpl(104, xL));
				Node n3ex = fac.createNode(Id.createNodeId("n3ex"),
						new CoordImpl(112, -xL));
				net.addNode(n2ex);
				net.addNode(n3ex);
				net.addNode(n0);
				net.addNode(n1);
				net.addNode(n2);
				net.addNode(n3);
				net.addNode(n4);
				net.addNode(n5);

				Link l0 = fac.createLink(Id.createLinkId("0"), n0, n1);
				Link l0rev = fac.createLink(Id.createLinkId("0rev"), n1, n0);
				Link l1 = fac.createLink(Id.createLinkId("1"), n1, n2);
				Link l1rev = fac.createLink(Id.createLinkId("1rev"), n2, n1);
				Link l2 = fac.createLink(Id.createLinkId("2"), n2, n3);
				Link l2rev = fac.createLink(Id.createLinkId("2rev"), n3, n2);
				Link l3 = fac.createLink(Id.createLinkId("3"), n3, n4);
				Link l3rev = fac.createLink(Id.createLinkId("3rev"), n4, n3);
				Link l4 = fac.createLink(Id.createLinkId("4"), n4, n5);
				Link l4rev = fac.createLink(Id.createLinkId("4rev"), n5, n4);

				Link l2ex = fac.createLink(Id.createLinkId("2ex"), n2, n2ex);
				Link l3ex = fac.createLink(Id.createLinkId("3ex"), n3, n3ex);

				l0.setLength(rL);
				l0rev.setLength(rL);
				l2ex.setLength(xL);
				l3ex.setLength(xL);
				l1.setLength(4);
				l1rev.setLength(4);
				l2.setLength(8);
				l2rev.setLength(8);
				l3.setLength(4);
				l3rev.setLength(4);
				l4.setLength(lL);
				l4rev.setLength(lL);
				net.addLink(l0);
				net.addLink(l0rev);
				net.addLink(l2ex);
				net.addLink(l3ex);
				net.addLink(l1);
				net.addLink(l1rev);
				net.addLink(l2);
				net.addLink(l2rev);
				net.addLink(l3);
				net.addLink(l3rev);
				net.addLink(l4);
				net.addLink(l4rev);

				l0.setCapacity(bL);
				l0rev.setCapacity(bL);
				l1.setCapacity(bBuff);
				l1rev.setCapacity(bBuff);
				l2.setCapacity(bCor);
				l2rev.setCapacity(bCor);
				l3.setCapacity(bBuff);
				l3rev.setCapacity(bBuff);
				l4.setCapacity(bR);
				l4rev.setCapacity(bR);
				l2ex.setCapacity(bEx);
				l3ex.setCapacity(bEx);

				List<Link> linksLR = new ArrayList<Link>();
				linksLR.add(l0);
				linksLR.add(l1);
				linksLR.add(l2);
				linksLR.add(l3ex);
				List<Link> linksLR2 = new ArrayList<Link>();
				linksLR2.add(l1);
				linksLR2.add(l2);
				linksLR2.add(l3ex);
				List<Link> linksLR3 = new ArrayList<Link>();
				linksLR3.add(l2);
				linksLR3.add(l3ex);

				List<Link> linksRL = new ArrayList<Link>();
				linksRL.add(l4rev);
				linksRL.add(l3rev);
				linksRL.add(l2rev);
				linksRL.add(l2ex);
				List<Link> linksRL2 = new ArrayList<Link>();
				linksRL2.add(l3rev);
				linksRL2.add(l2rev);
				linksRL2.add(l2ex);
				List<Link> linksRL3 = new ArrayList<Link>();
				linksRL3.add(l2rev);
				linksRL3.add(l2ex);

				System.out.println(" " + bL + " " + bCor + " " + bR + "\n");

				// runIt(net, linksLR, linksRL, linksLR2, linksRL2, linksLR3,
				// linksRL3, sc, s);
				runItII(net, linksLR, linksRL, sc, s);

			}

			bw2.close();
		}
	}

	private static void runIt(Network net, List<Link> linksLR,
			List<Link> linksRL, List<Link> linksLR2, List<Link> linksRL2,
			List<Link> linksLR3, List<Link> linksRL3, Scenario sc, Setting s) {
		// visualization stuff
		EventsManager em = new EventsManagerImpl();

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
			if (USE_SPH) {
				fac.setDensityEstimatorFactory(new CASingleLaneDensityEstimatorSPHFactory());
			} else {
				fac.setDensityEstimatorFactory(new CASingleLaneDensityEstimatorSPAFactory());
			}
			// fac.setDensityEstimatorFactory(new
			// CAConstantDensityEstimatorFactory());
		}

		CANetwork caNet = fac.createCANetwork(net, em, null);

		int agents = 0;
		{
			CASingleLaneLink caLinkLR = (CASingleLaneLink) caNet
					.getCALink(linksLR.get(0).getId());
			CAMoveableEntity[] particles = caLinkLR.getParticles();
			System.out.println("part left:" + particles.length);

			for (int i = 0; i < particles.length - 1; i++) {

				CAMoveableEntity a = new CASimpleDynamicAgent(linksLR, 1,
						Id.create("g" + agents++, CASimpleDynamicAgent.class),
						caLinkLR);
				a.materialize(i, 1);
				particles[i] = a;
				CASimAgentConstructEvent ee = new CASimAgentConstructEvent(0, a);
				em.processEvent(ee);
				LinkEnterEvent ee2 = new LinkEnterEvent(0, a.getId(),
						Id.createLinkId("0"), a.getId());
				em.processEvent(ee2);
				// CAEvent e = new
				// CAEvent(1/(CANetworkDynamic.V_HAT*CANetworkDynamic.RHO_HAT),
				// a,caLinkLR, CAEventType.TTA);
				CAEvent e = new CAEvent(0, a, caLinkLR, CAEventType.TTA);
				caNet.pushEvent(e);
				caNet.registerAgent(a);
			}
		}
		{
			CASingleLaneLink caLinkLR = (CASingleLaneLink) caNet
					.getCALink(linksLR2.get(0).getId());
			CAMoveableEntity[] particles = caLinkLR.getParticles();
			System.out.println("part left:" + particles.length);

			for (int i = 0; i < particles.length - 1; i++) {

				CAMoveableEntity a = new CASimpleDynamicAgent(linksLR2, 1,
						Id.create("g" + agents++, CASimpleDynamicAgent.class),
						caLinkLR);
				a.materialize(i, 1);
				particles[i] = a;
				CASimAgentConstructEvent ee = new CASimAgentConstructEvent(0, a);
				em.processEvent(ee);
				LinkEnterEvent ee2 = new LinkEnterEvent(0, a.getId(),
						Id.createLinkId("0"), a.getId());
				em.processEvent(ee2);
				// CAEvent e = new
				// CAEvent(1/(CANetworkDynamic.V_HAT*CANetworkDynamic.RHO_HAT),
				// a,caLinkLR, CAEventType.TTA);
				CAEvent e = new CAEvent(0, a, caLinkLR, CAEventType.TTA);
				caNet.pushEvent(e);
				caNet.registerAgent(a);
			}
		}
		{
			CASingleLaneLink caLinkRL = (CASingleLaneLink) caNet
					.getCALink(linksRL.get(0).getId());
			CAMoveableEntity[] particles = caLinkRL.getParticles();
			System.out.println("part left:" + particles.length);

			for (int i = 1; i < particles.length; i++) {

				CAMoveableEntity a = new CASimpleDynamicAgent(linksRL, 1,
						Id.create("r" + agents++, CASimpleDynamicAgent.class),
						caLinkRL);
				a.materialize(i, -1);
				particles[i] = a;
				CASimAgentConstructEvent ee = new CASimAgentConstructEvent(0, a);
				em.processEvent(ee);
				LinkEnterEvent ee2 = new LinkEnterEvent(0, a.getId(),
						Id.createLinkId("0"), a.getId());
				em.processEvent(ee2);
				// CAEvent e = new
				// CAEvent(1/(CANetworkDynamic.V_HAT*CANetworkDynamic.RHO_HAT),
				// a,caLinkLR, CAEventType.TTA);
				CAEvent e = new CAEvent(0, a, caLinkRL, CAEventType.TTA);
				caNet.pushEvent(e);
				caNet.registerAgent(a);
			}
		}
		{
			CASingleLaneLink caLinkRL = (CASingleLaneLink) caNet
					.getCALink(linksRL2.get(0).getId());
			CAMoveableEntity[] particles = caLinkRL.getParticles();
			System.out.println("part left:" + particles.length);

			for (int i = 1; i < particles.length; i++) {

				CAMoveableEntity a = new CASimpleDynamicAgent(linksRL2, 1,
						Id.create("r" + agents++, CASimpleDynamicAgent.class),
						caLinkRL);
				a.materialize(i, -1);
				particles[i] = a;
				CASimAgentConstructEvent ee = new CASimAgentConstructEvent(0, a);
				em.processEvent(ee);
				LinkEnterEvent ee2 = new LinkEnterEvent(0, a.getId(),
						Id.createLinkId("0"), a.getId());
				em.processEvent(ee2);
				// CAEvent e = new
				// CAEvent(1/(CANetworkDynamic.V_HAT*CANetworkDynamic.RHO_HAT),
				// a,caLinkLR, CAEventType.TTA);
				CAEvent e = new CAEvent(0, a, caLinkRL, CAEventType.TTA);
				caNet.pushEvent(e);
				caNet.registerAgent(a);
			}
		}

		{
			CASingleLaneLink caLinkRL = (CASingleLaneLink) caNet
					.getCALink(linksRL3.get(0).getId());
			CAMoveableEntity[] particles = caLinkRL.getParticles();
			System.out.println("part left:" + particles.length);

			for (int i = 1; i < particles.length; i += 2) {
				if (agents % 2 == 0) {

					CAMoveableEntity a = new CASimpleDynamicAgent(linksLR3, 1,
							Id.create("g" + agents++,
									CASimpleDynamicAgent.class), caLinkRL);
					a.materialize(i, 1);
					particles[i] = a;
					CASimAgentConstructEvent ee = new CASimAgentConstructEvent(
							0, a);
					em.processEvent(ee);
					LinkEnterEvent ee2 = new LinkEnterEvent(0, a.getId(),
							Id.createLinkId("0"), a.getId());
					em.processEvent(ee2);
					// CAEvent e = new
					// CAEvent(1/(CANetworkDynamic.V_HAT*CANetworkDynamic.RHO_HAT),
					// a,caLinkLR, CAEventType.TTA);
					CAEvent e = new CAEvent(0, a, caLinkRL, CAEventType.TTA);
					caNet.pushEvent(e);
					caNet.registerAgent(a);
				} else {

					CAMoveableEntity a = new CASimpleDynamicAgent(linksRL3, 1,
							Id.create("r" + agents++,
									CASimpleDynamicAgent.class), caLinkRL);
					a.materialize(i, -1);
					particles[i] = a;
					CASimAgentConstructEvent ee = new CASimAgentConstructEvent(
							0, a);
					em.processEvent(ee);
					LinkEnterEvent ee2 = new LinkEnterEvent(0, a.getId(),
							Id.createLinkId("0"), a.getId());
					em.processEvent(ee2);
					// CAEvent e = new
					// CAEvent(1/(CANetworkDynamic.V_HAT*CANetworkDynamic.RHO_HAT),
					// a,caLinkLR, CAEventType.TTA);
					CAEvent e = new CAEvent(0, a, caLinkRL, CAEventType.TTA);
					caNet.pushEvent(e);
					caNet.registerAgent(a);
				}
			}
		}

		CALinkMonitorExact monitor = new CALinkMonitorExactII(
				caNet.getCALink(Id.createLinkId("2")), 2.,
				((CASingleLaneLink) caNet.getCALink(Id.createLinkId("2")))
						.getParticles(), caNet.getCALink(Id.createLinkId("2"))
						.getLink().getCapacity());
		caNet.addMonitor(monitor);
		monitor.init();
		caNet.run(3600);
		try {
			monitor.report(bw2);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void runItII(Network net, List<Link> linksLR,
			List<Link> linksRL, Scenario sc, Setting s) {
		// visualization stuff
		EventsManager em = new EventsManagerImpl();

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
			if (USE_SPH) {
				fac.setDensityEstimatorFactory(new CASingleLaneDensityEstimatorSPHFactory());
			} else {
				fac.setDensityEstimatorFactory(new CASingleLaneDensityEstimatorSPAFactory());
			}
			// fac.setDensityEstimatorFactory(new
			// CAConstantDensityEstimatorFactory());
		}

		CANetwork caNet = fac.createCANetwork(net, em, null);

		int agents = 0;
		{
			CASingleLaneLink caLinkLR = (CASingleLaneLink) caNet
					.getCALink(linksLR.get(0).getId());
			CAMoveableEntity[] particles = caLinkLR.getParticles();
			System.out.println("part left:" + particles.length);

			int skip = 20;
			int cnt = 0;
			for (int i = particles.length - 1; i >= 0; i -= skip) {
				if (cnt > 150 && skip > 12 || cnt > 300) {
					System.out.println(skip + " " + cnt);
					cnt = 0;
					skip--;
					if (skip == 10) {
						break;
					}
				}
				cnt++;

				CAMoveableEntity a = new CASimpleDynamicAgent(linksLR, 1,
						Id.create("g" + agents++, CASimpleDynamicAgent.class),
						caLinkLR);
				a.materialize(i, 1);
				particles[i] = a;
				CASimAgentConstructEvent ee = new CASimAgentConstructEvent(0, a);
				em.processEvent(ee);
				LinkEnterEvent ee2 = new LinkEnterEvent(0, a.getId(),
						Id.createLinkId("0"), a.getId());
				em.processEvent(ee2);
				// CAEvent e = new
				// CAEvent(1/(CANetworkDynamic.V_HAT*CANetworkDynamic.RHO_HAT),
				// a,caLinkLR, CAEventType.TTA);
				CAEvent e = new CAEvent(0, a, caLinkLR, CAEventType.TTA);
				caNet.pushEvent(e);
				caNet.registerAgent(a);
			}
		}
		{
			CASingleLaneLink caLinkRL = (CASingleLaneLink) caNet
					.getCALink(linksRL.get(0).getId());
			CAMoveableEntity[] particles = caLinkRL.getParticles();
			System.out.println("part left:" + particles.length);
			int skip = 20;
			int cnt = 0;
			for (int i = 0; i < particles.length; i += skip) {

				if (cnt > 150 && skip > 12 || cnt > 300) {
					skip--;
					cnt = 0;
					if (skip == 10) {
						break;
					}
				}
				cnt++;
				CAMoveableEntity a = new CASimpleDynamicAgent(linksRL, 1,
						Id.create("r" + agents++, CASimpleDynamicAgent.class),
						caLinkRL);
				a.materialize(i, -1);
				particles[i] = a;
				CASimAgentConstructEvent ee = new CASimAgentConstructEvent(0, a);
				em.processEvent(ee);
				LinkEnterEvent ee2 = new LinkEnterEvent(0, a.getId(),
						Id.createLinkId("0"), a.getId());
				em.processEvent(ee2);
				// CAEvent e = new
				// CAEvent(1/(CANetworkDynamic.V_HAT*CANetworkDynamic.RHO_HAT),
				// a,caLinkLR, CAEventType.TTA);
				CAEvent e = new CAEvent(0, a, caLinkRL, CAEventType.TTA);
				caNet.pushEvent(e);
				caNet.registerAgent(a);
			}
		}

		CALinkMonitorExact monitor = new CALinkMonitorExactII(
				caNet.getCALink(Id.createLinkId("2")), 2.,
				((CASingleLaneLink) caNet.getCALink(Id.createLinkId("2")))
						.getParticles(), caNet.getCALink(Id.createLinkId("2"))
						.getLink().getCapacity());
		caNet.addMonitor(monitor);
		monitor.init();
		caNet.run(3600);
		try {
			monitor.report(bw2);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
