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
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;

import playground.gregor.casim.events.CASimAgentConstructEvent;
import playground.gregor.casim.monitoring.CALinkMonitorExactII;
import playground.gregor.casim.monitoring.Monitor;
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

public class Long1DChannelBi {

	public final static boolean USE_MULTI_LANE_MODEL = false;
	private static BufferedWriter bw;
	private static boolean USE_SPH;

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
		AbstractCANetwork.EMIT_VIS_EVENTS = false;
		USE_SPH = true;

		for (int R = 12; R <= 12; R++) {
			CASingleLaneDensityEstimatorSPH.H = R;
			CASingleLaneDensityEstimatorSPHII.H = R;
			CASingleLaneDensityEstimatorSPA.RANGE = R;
			CASingleLaneDensityEstimatorSPAII.RANGE = R;
			try {
				bw = new BufferedWriter(
						new FileWriter(new File(
								"/Users/laemmel/devel/bipedca/ant/rho_new_bi_spl_"
										+ R)));
			} catch (IOException e) {
				e.printStackTrace();
			}

			for (int ii = 0; ii < 20; ii++) {
				System.out
						.println("+++++++++++++++++++++++++++++++++++++++++++++++++++");
				System.out.println("it:" + ii);
				System.out
						.println("+++++++++++++++++++++++++++++++++++++++++++++++++++");
				double w = 0;
				while (w < 1 || w > 3) {
					w = 2 + MatsimRandom.getRandom().nextGaussian();
				}
				w = AbstractCANetwork.PED_WIDTH;
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

				Node n3 = fac.createNode(Id.createNodeId("3"), new CoordImpl(
						-10, 0));

				Node n0 = fac.createNode(Id.createNodeId("0"), new CoordImpl(0,
						0));
				Node n1 = fac.createNode(Id.createNodeId("1"), new CoordImpl(
						100, 0));
				Node n2 = fac.createNode(Id.createNodeId("2"), new CoordImpl(
						100, 0));
				net.addNode(n3);
				net.addNode(n2);
				net.addNode(n1);
				net.addNode(n0);

				Link l0 = fac.createLink(Id.createLinkId("0"), n0, n1);
				Link l0rev = fac.createLink(Id.createLinkId("0rev"), n1, n0);
				Link l1 = fac.createLink(Id.createLinkId("1"), n1, n2);
				Link l1rev = fac.createLink(Id.createLinkId("1rev"), n2, n1);
				Link l2 = fac.createLink(Id.createLinkId("2"), n0, n3);
				Link l2rev = fac.createLink(Id.createLinkId("2rev"), n3, n0);

				l0.setLength(100);
				l1.setLength(10);
				l2.setLength(10);

				l0rev.setLength(100);
				l1rev.setLength(10);
				l2rev.setLength(10);
				net.addLink(l1);
				net.addLink(l0);
				net.addLink(l1rev);
				net.addLink(l0rev);
				net.addLink(l2);
				net.addLink(l2rev);
				l0.setCapacity(w);
				l1.setCapacity(w);
				l0rev.setCapacity(w);
				l1rev.setCapacity(w);
				l2.setCapacity(w);
				l2rev.setCapacity(w);

				List<Link> linksLR = new ArrayList<Link>();
				linksLR.add(l0);
				linksLR.add(l1);
				List<Link> linksRL = new ArrayList<Link>();
				linksRL.add(l0);
				linksRL.add(l2);

				c.qsim().setEndTime(3600);
				for (double skip = 20; skip >= 1; skip--) {

					// if (skip == 2) {
					// c.qsim().setEndTime(240);
					// } else if (skip == 1) {
					// c.qsim().setEndTime(480);
					// }
					for (double mskip = 5; mskip <= 5; mskip++) {
						System.out
								.println("---------------------------------------");
						System.out.println("skip" + skip + " mskip:" + mskip);
						System.out
								.println("---------------------------------------");
						// skip = 3;
						// mskip = 9;
						for (double rho = 0.2; rho < 0.25; rho += 0.1)
							runIt(net, linksLR, linksRL, sc, skip, mskip, rho);
						// skip = -1;
						// break;
					}
				}
				// break;
			}
			bw.close();
		}
	}

	private static void runIt(Network net, List<Link> linksLR,
			List<Link> linksRL, Scenario sc, double skip, double mskip,
			double rho) {

		// visualization stuff
		EventsManager em = new EventsManagerImpl();
		// // // if (iter == 9)

		if (AbstractCANetwork.EMIT_VIS_EVENTS) {
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
			fac.setDensityEstimatorFactory(null);

		} else {
			fac = new CASingleLaneNetworkFactory();
			if (USE_SPH) {
				fac.setDensityEstimatorFactory(new CASingleLaneDensityEstimatorSPHFactory());
			} else {
				fac.setDensityEstimatorFactory(new CASingleLaneDensityEstimatorSPAFactory());
			}

		}

		CANetwork caNet = fac.createCANetwork(net, em, null);

		int agents = 0;

		CASingleLaneLink caLink = (CASingleLaneLink) caNet.getCALink(linksLR
				.get(0).getId());
		CAMoveableEntity[] particles = caLink.getParticles();
		System.out.println("part left:" + particles.length);

		CAMoveableEntity a1 = null;
		CAMoveableEntity a2 = null;
		double rho1 = Double.NaN; // rho * AbstractCANetwork.RHO_HAT / 2;
		double rho2 = Double.NaN; // rho * AbstractCANetwork.RHO_HAT / 2;
		// double rho = MatsimRandom.getRandom().nextDouble() / 10;
		for (int i = 0; i < particles.length; i += skip) {

			// if (mskip < 5 && i % mskip == 0) {
			// i++;
			// if (i >= particles.length) {
			// break;
			// }
			// }
			// if (MatsimRandom.getRandom().nextDouble() > rho) {
			// continue;
			// }

			boolean side = agents % 2 == 0;
			int shift = -1;
			if (skip == 1) {
				shift = 0;
			} else {
				while (shift < skip / 4. || shift > 3. * skip / 4.) {
					shift = (int) (MatsimRandom.getRandom().nextGaussian()
							* skip / 4. + skip / 2);
				}
			}
			int pos = i + shift;

			if (pos >= particles.length) {
				continue;
			}
			if (side) {

				// agents++;
				CAMoveableEntity a = new CASimpleDynamicAgent(linksLR, 1,
						Id.create(agents++, CASimpleDynamicAgent.class), caLink);
				a.materialize(pos, 1);
				particles[pos] = a;
				if (a1 == null && pos > particles.length / 3) {
					a1 = a;
				}
				CASimAgentConstructEvent ee = new CASimAgentConstructEvent(0, a);
				em.processEvent(ee);

				CAEvent e = new CAEvent(
						1 / (AbstractCANetwork.V_HAT * AbstractCANetwork.RHO_HAT),
						a, caLink, CAEventType.TTA);
				caNet.pushEvent(e);
				caNet.registerAgent(a);
			} else {
				// agents++;
				CAMoveableEntity a = new CASimpleDynamicAgent(linksRL, 1,
						Id.create(agents++, CASimpleDynamicAgent.class), caLink);
				a.materialize(pos, -1);
				particles[pos] = a;
				if (pos < 2 * particles.length / 3) {
					a2 = a;
				}
				CASimAgentConstructEvent ee = new CASimAgentConstructEvent(0, a);
				em.processEvent(ee);

				if (i > 0 && particles[pos - 1] != null
						&& particles[pos - 1].getDir() == 1) {
					CAEvent e = new CAEvent(0, a, caLink, CAEventType.SWAP);
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

		// CALinkMonitorExact monitor = new
		// CALinkMonitorRange(caNet.getCALink(Id
		// .createLinkId("0")), 10.,
		// ((CASingleLaneLink) caNet.getCALink(Id.createLinkId("0")))
		// .getParticles(), caNet.getCALink(Id.createLinkId("0"))
		// .getLink().getCapacity(), rho1, rho2, a1, a2);
		Monitor monitor = new CALinkMonitorExactII(caNet.getCALink(Id
				.createLinkId("0")), 10.,
				((CASingleLaneLink) caNet.getCALink(Id.createLinkId("0")))
						.getParticles(), caNet.getCALink(Id.createLinkId("0"))
						.getLink().getCapacity());
		caNet.addMonitor(monitor);
		monitor.init();
		caNet.run(sc.getConfig().qsim().getEndTime());
		try {
			monitor.report(bw);
			bw.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
