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

package playground.gregor.casim.simulation.physics;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;

import playground.gregor.casim.monitoring.CALinkMonitor;
import playground.gregor.casim.simulation.physics.CAEvent.CAEventType;
import playground.gregor.sim2d_v4.scenario.Sim2DConfig;
import playground.gregor.sim2d_v4.scenario.Sim2DConfigUtils;
import playground.gregor.sim2d_v4.scenario.Sim2DScenario;
import playground.gregor.sim2d_v4.scenario.Sim2DScenarioUtils;

public class CASimExperiment_ZhangJ2011 {


	//BFR-DML-360 exp
	private static final double ESPILON = 0.1;
	private static final double B_r = 0;

	//	0.50 0.50 0.65 0.65 0.75 0.75 0.85 0.85 1.00 1.00 0.50 0.50 0.75 0.75 0.90 0.90 1.20 1.20 1.60 1.60 2.00 2.00 2.50 2.50
	private static final List<Setting> settings = new ArrayList<Setting>();
	
	static {
		settings.add(new Setting(.5,1.8,1.8));
		settings.add(new Setting(.6,1.8,1.8));
		settings.add(new Setting(.7,1.8,1.8));
		settings.add(new Setting(1.,1.8,1.8));
		settings.add(new Setting(1.45,1.8,1.8));
		settings.add(new Setting(1.8,1.8,1.8));
		settings.add(new Setting(1.8,1.8,1.2));
		settings.add(new Setting(1.8,1.8,.95));
		settings.add(new Setting(1.8,1.8,.7));
		settings.add(new Setting(.65,2.4,2.4));
		settings.add(new Setting(.8,2.4,2.4));
		settings.add(new Setting(.95,2.4,2.4));
		settings.add(new Setting(1.45,2.4,2.4));
		settings.add(new Setting(1.9,2.4,2.4));
		settings.add(new Setting(2.4,2.4,2.4));
		settings.add(new Setting(2.4,2.4,1.6));
		settings.add(new Setting(2.4,2.4,1.3));
		settings.add(new Setting(2.4,2.4,1.0));
		settings.add(new Setting(.8,3.,3.));
		settings.add(new Setting(1.,3.,3.));
		settings.add(new Setting(1.8,3.,3.));
		settings.add(new Setting(2.4,3.,3.));
		settings.add(new Setting(3.,3.,3.));
		settings.add(new Setting(3.,3.,1.6));
		settings.add(new Setting(3.,3.,1.2));
		settings.add(new Setting(3.,3.,.8));
		
		settings.add(new Setting(1.8,1.8,1.15));
		settings.add(new Setting(1.8,1.8,1.1));
		settings.add(new Setting(1.8,1.8,1.05));
		settings.add(new Setting(1.8,1.8,1.0));
		settings.add(new Setting(1.8,1.8,0.95));
		settings.add(new Setting(1.8,1.8,0.9));
		
	}


	private static final class Setting {
		public Setting(double bl, double bCor, double bEx) {
			this.bL = bl;
			this.bCor = bCor;
			this.bEx = bEx;
		}
		double bL;
		double bCor;
		double bEx;
	}

	public static void main(String [] args) throws IOException {
		Config c = ConfigUtils.createConfig();
		c.global().setCoordinateSystem("EPSG:3395");
		Scenario sc = ScenarioUtils.createScenario(c);


		//VIS only
		Sim2DConfig conf2d = Sim2DConfigUtils.createConfig();
		Sim2DScenario sc2d = Sim2DScenarioUtils.createSim2dScenario(conf2d);
		sc.addScenarioElement(Sim2DScenario.ELEMENT_NAME,sc2d);

		Network net = sc.getNetwork();
		((NetworkImpl)net).setCapacityPeriod(1);
		NetworkFactory fac = net.getFactory();

		Node n0 = fac.createNode(new IdImpl("0"), new CoordImpl(-100,0));
		Node n1 = fac.createNode(new IdImpl("1"), new CoordImpl(0,0));
		Node n2 = fac.createNode(new IdImpl("2"), new CoordImpl(4,0));
		Node n2ex = fac.createNode(new IdImpl("2ex"), new CoordImpl(4,100));
		Node n3 = fac.createNode(new IdImpl("3"), new CoordImpl(12,0));
		Node n3ex = fac.createNode(new IdImpl("3ex"), new CoordImpl(12,-100));
		Node n4 = fac.createNode(new IdImpl("4"), new CoordImpl(16,0));
		Node n5 = fac.createNode(new IdImpl("5"), new CoordImpl(116,0));

		net.addNode(n3ex);net.addNode(n2ex);net.addNode(n5);net.addNode(n4);net.addNode(n3);net.addNode(n2);net.addNode(n1);net.addNode(n0);

		Link l0 = fac.createLink(new IdImpl("0"), n0, n1);
		Link l0rev = fac.createLink(new IdImpl("0rev"), n1, n0);
		Link l1 = fac.createLink(new IdImpl("1"), n1, n2);
		Link l1rev = fac.createLink(new IdImpl("1rev"), n2, n1);
		Link l2 = fac.createLink(new IdImpl("2"), n2, n3);
		Link l2ex = fac.createLink(new IdImpl("2ex"), n2, n2ex);
		Link l2rev = fac.createLink(new IdImpl("2rev"), n3, n2);
		Link l3 = fac.createLink(new IdImpl("3"), n3, n4);
		Link l3ex = fac.createLink(new IdImpl("3ex"), n3, n3ex);
		Link l3rev = fac.createLink(new IdImpl("3rev"), n4, n3);
		Link l4 = fac.createLink(new IdImpl("4"), n4, n5);
		Link l4rev = fac.createLink(new IdImpl("4rev"), n5, n4);

		l0.setLength(100);
		l1.setLength(4);
		l2ex.setLength(100);
		l2.setLength(8);
		l3ex.setLength(100);
		l3.setLength(4);
		l4.setLength(100);

		l0rev.setLength(100);
		l1rev.setLength(4);
		l2rev.setLength(8);
		l3rev.setLength(4);
		l4rev.setLength(100);

		net.addLink(l3ex);net.addLink(l2ex);net.addLink(l4);net.addLink(l3);net.addLink(l2);net.addLink(l1);net.addLink(l0);
		net.addLink(l4rev);net.addLink(l3rev);net.addLink(l2rev);net.addLink(l1rev);net.addLink(l0rev);

		CALinkMonitor monitor = new CALinkMonitor(l2.getId(), l2rev.getId(),l2);

		BufferedWriter buf = new BufferedWriter(new FileWriter(new File("/Users/laemmel/devel/bipedca/plot/unbl_ZhangJ2011")));


		for (Setting s : settings){

			double bL = s.bL;
			double bCor = s.bCor;
			double bEx = s.bEx;


			l0.setCapacity(bL);
			l1.setCapacity(bCor);
			l2.setCapacity(bCor);
			l3.setCapacity(bCor);
			l4.setCapacity(B_r);
			l0rev.setCapacity(bL);
			l1rev.setCapacity(bCor);
			l2rev.setCapacity(bCor);
			l3rev.setCapacity(bCor);
			l4rev.setCapacity(B_r);
			//			l2ex.setCapacity(B_exit);
			//			l3ex.setCapacity(B_exit);
			l2ex.setCapacity(bEx);
			l3ex.setCapacity(bEx);

			List<Link> linksLR = new ArrayList<Link>();
			linksLR.add(l0);
			linksLR.add(l1);
			linksLR.add(l2);
			linksLR.add(l3ex);
			List<Link> linksRL = new ArrayList<Link>();
			linksRL.add(l4);
			linksRL.add(l3);
			linksRL.add(l2);
			linksRL.add(l2ex);

			double rho = 1;
			int ii = 0;
			while (ii++ < 5) {
				runIt(net,monitor,linksLR,linksRL,sc,rho);

				double diff = Math.abs(rho-monitor.getCurrentRho());
				if (diff > ESPILON) {
					rho = monitor.getCurrentRho();
					monitor.reset(0);
				} else {
					monitor.save();
					monitor.reset(0);
					break;
				}
			}
			monitor.reset(0);
			//					System.out.println(monitor);
			String app = monitor.toString();
			System.out.println(app);
			buf.append(app);
			buf.append(" " + bL + " " + bCor + " " + bEx +"\n");
		}

		buf.close();
	}

	private static void runIt(Network net,CALinkMonitor monitor,List<Link>linksLR,List<Link>linksRL, Scenario sc,double rho){
		//visualization stuff
		EventsManager em = new EventsManagerImpl();

		//		if (iter == 9)
		//		EventBasedVisDebuggerEngine vis = new EventBasedVisDebuggerEngine(sc);
		//		em.addHandler(vis);
		//		vis.addAdditionalDrawer(new InfoBox(vis, sc));

		CANetwork.RHO = rho;//(double)agents/fields * 5.091;
		CANetwork caNet = new CANetwork(net,em);

		int agents = 0;

		{
			CALink caLink = caNet.getCALink(linksLR.get(0).getId());
			CAAgent[] particles = caLink.getParticles();
			System.out.println("part left:" + particles.length);
			for (int i = 0; i < particles.length; i ++) {
				agents++;
				CAAgent a = new CASimpleAgent(linksLR, 1, new IdImpl(agents++), caLink);
				a.materialize(i, 1);
				CAEvent e = new CAEvent(0, a,caLink, CAEventType.TTA);
				caNet.pushEvent(e);
			}

		}
		{
			CALink caLink = caNet.getCALink(linksRL.get(0).getId());
			CAAgent[] particles = caLink.getParticles();
			System.out.println("part right:" + particles.length);
			for (int i = 0; i < particles.length; i ++) {
				agents++;
				CAAgent a = new CASimpleAgent(linksRL, 1, new IdImpl(-(agents++)), caLink);
				a.materialize(i, -1);
				CAEvent e = new CAEvent(0, a,caLink, CAEventType.TTA);
				caNet.pushEvent(e);
			}

		}

		em.addHandler(monitor);

		caNet.run();
	}

}
