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
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.misc.StringUtils;

import playground.gregor.casim.events.CASimAgentConstructEvent;
import playground.gregor.casim.monitoring.CALinkMonitor;
import playground.gregor.casim.simulation.physics.CAEvent.CAEventType;
import playground.gregor.sim2d_v4.scenario.Sim2DConfig;
import playground.gregor.sim2d_v4.scenario.Sim2DConfigUtils;
import playground.gregor.sim2d_v4.scenario.Sim2DScenario;
import playground.gregor.sim2d_v4.scenario.Sim2DScenarioUtils;

public class CASimExperimentBi3_ZhangJ2012a_RND {


	//BFR-DML-360 exp
	private static double B_cor = 3.6;
	private static final double ESPILON = 0.1;
	private static double B_l = 2;
	private static double B_r = 2;



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
		Node n2a = fac.createNode(new IdImpl("2a"), new CoordImpl(7,0));
		Node n2b = fac.createNode(new IdImpl("2b"), new CoordImpl(9,0));


		Node n3 = fac.createNode(new IdImpl("3"), new CoordImpl(12,0));
		Node n3ex = fac.createNode(new IdImpl("3ex"), new CoordImpl(12,-100));
		Node n4 = fac.createNode(new IdImpl("4"), new CoordImpl(16,0));
		Node n5 = fac.createNode(new IdImpl("5"), new CoordImpl(116,0));

		net.addNode(n2b);net.addNode(n2a);net.addNode(n3ex);net.addNode(n2ex);net.addNode(n5);net.addNode(n4);net.addNode(n3);net.addNode(n2);net.addNode(n1);net.addNode(n0);

		Link l0 = fac.createLink(new IdImpl("0"), n0, n1);
		Link l0rev = fac.createLink(new IdImpl("0rev"), n1, n0);
		Link l1 = fac.createLink(new IdImpl("1"), n1, n2);
		Link l1rev = fac.createLink(new IdImpl("1rev"), n2, n1);
		Link l2 = fac.createLink(new IdImpl("2"), n2, n2a);
		Link l2ex = fac.createLink(new IdImpl("2ex"), n2, n2ex);
		Link l2rev = fac.createLink(new IdImpl("2rev"), n2a, n2);
		Link l2a = fac.createLink(new IdImpl("2a"), n2a, n2b);
		Link l2b = fac.createLink(new IdImpl("2b"), n2b, n3);
		Link l2aRev = fac.createLink(new IdImpl("2aRev"), n2b, n2a);
		Link l2bRev = fac.createLink(new IdImpl("2bREv"), n3, n2b);
		Link l3 = fac.createLink(new IdImpl("3"), n3, n4);
		Link l3ex = fac.createLink(new IdImpl("3ex"), n3, n3ex);
		Link l3rev = fac.createLink(new IdImpl("3rev"), n4, n3);
		Link l4 = fac.createLink(new IdImpl("4"), n4, n5);
		Link l4rev = fac.createLink(new IdImpl("4rev"), n5, n4);

		l0.setLength(1000);
		l1.setLength(4);
		l2ex.setLength(100);
		l2.setLength(3);
		l2a.setLength(2);
		l2b.setLength(3);
		l3ex.setLength(100);
		l3.setLength(4);
		l4.setLength(1000);

		l0rev.setLength(1000);
		l1rev.setLength(4);
		l2rev.setLength(3);
		l2aRev.setLength(2);
		l2bRev.setLength(3);
		l3rev.setLength(4);
		l4rev.setLength(1000);

		net.addLink(l2b);net.addLink(l2a);net.addLink(l3ex);net.addLink(l2ex);net.addLink(l4);net.addLink(l3);net.addLink(l2);net.addLink(l1);net.addLink(l0);
		net.addLink(l2aRev);net.addLink(l2bRev);net.addLink(l4rev);net.addLink(l3rev);net.addLink(l2rev);net.addLink(l1rev);net.addLink(l0rev);

		CALinkMonitor monitor = new CALinkMonitor(l2a.getId(), l2aRev.getId(),l2a);

		MatsimRandom.reset(System.currentTimeMillis());
		MatsimRandom.getRandom().nextDouble();
		MatsimRandom.getRandom().nextDouble();
		BufferedWriter buf = new BufferedWriter(new FileWriter(new File("/Users/laemmel/devel/bipedca/plot/03rnd_2_"+MatsimRandom.getRandom().nextLong())));

		for (int i = 0; i < 50; i++){
			B_l = Math.min(5,Math.max(2+(2*MatsimRandom.getRandom().nextDouble()-1)*1.8,0.2));
			B_r = Math.min(5,Math.max(2+(2*MatsimRandom.getRandom().nextDouble()-1)*1.8,0.2));
			B_cor = Math.min(5,Math.max(2+(2*MatsimRandom.getRandom().nextDouble()-1)*1.8,0.4));

			double bExR = Math.min(5,Math.max(2+(2*MatsimRandom.getRandom().nextDouble()-1)*1.8,0.2));
			double bExL = Math.min(5,Math.max(2+(2*MatsimRandom.getRandom().nextDouble()-1)*1.8,0.2));

//			if (B_l > bExR && B_r > bExL) {
//				continue;
//			}
			
			double total = 2000;
			double left = total * B_l /(B_l+B_r);
			double right = total * B_r /(B_l+B_r);
			left = Math.max(left, 400);
			right = Math.max(right, 400);
			double lL = left/(5.091*B_l);
			double lR = right/(5.091*B_r);

			l0.setLength(lL);
			NodeImpl n = (NodeImpl) l0.getFromNode();
			CoordImpl cc = (CoordImpl) n.getCoord();
			cc.setX(-lL);
			l4.setLength(lR);
			CoordImpl ccc = (CoordImpl)(l4.getToNode().getCoord());
			ccc.setX(16+lR);


			l0rev.setLength(lL);
			l4rev.setLength(lR);

			l0.setCapacity(B_l);
			l1.setCapacity(B_cor);
			l2.setCapacity(B_cor);
			l2a.setCapacity(B_cor);
			l2b.setCapacity(B_cor);
			l3.setCapacity(B_cor);
			l4.setCapacity(B_r);
			l0rev.setCapacity(B_l);
			l1rev.setCapacity(B_cor);
			l2rev.setCapacity(B_cor);
			l2aRev.setCapacity(B_cor);
			l2bRev.setCapacity(B_cor);
			l3rev.setCapacity(B_cor);
			l4rev.setCapacity(B_r);
			//			l2ex.setCapacity(B_exit);
			//			l3ex.setCapacity(B_exit);
			l2ex.setCapacity(bExL);
			l3ex.setCapacity(bExR);

			List<Link> linksLR = new ArrayList<Link>();
			linksLR.add(l0);
			linksLR.add(l1);
			linksLR.add(l2);
			linksLR.add(l2a);
			linksLR.add(l2b);
			linksLR.add(l3ex);
			List<Link> linksRL = new ArrayList<Link>();
			linksRL.add(l4);
			linksRL.add(l3);
			linksRL.add(l2b);
			linksRL.add(l2a);
			linksRL.add(l2);
			linksRL.add(l2ex);

			double rho =6.661;
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
			StringBuffer app = new StringBuffer();
			app.append(monitor.toString());
			if (app.length() < 2) {
				continue;
			}
			app.append(" ");
			app.append(B_l);
			app.append(" ");
			app.append(B_r);
			app.append(" ");
			app.append(bExR);
			app.append(" ");
			app.append(bExL);
			app.append(" ");
			app.append(CANetwork.RHO);
			app.append(" ");
			app.append(B_cor);
			app.append("\n");
			System.out.println(app);
			
			String[] expl = StringUtils.explode(app.toString(), ' ' );
//			if (expl.length > 14) {
//				System.out.println("got you");
//			}
			buf.append(app);
//			buf.flush();
			System.out.println(i);
		}
		buf.close();
	}

	private static void runIt(Network net,CALinkMonitor monitor,List<Link>linksLR,List<Link>linksRL, Scenario sc,double rho){
		//visualization stuff
		EventsManager em = new EventsManagerImpl();

		//		//		if (iter == 9)
//				EventBasedVisDebuggerEngine vis = new EventBasedVisDebuggerEngine(sc);
//				em.addHandler(vis);
//				vis.addAdditionalDrawer(new InfoBox(vis, sc));

		CANetwork.RHO = rho;//(double)agents/fields * 5.091;
		CANetwork caNet = new CANetwork(net,em);

		int agents = 0;

		{
			CALink caLink = caNet.getCALink(linksLR.get(0).getId());
			CAAgent[] particles = caLink.getParticles();
//			System.out.println("part left:" + particles.length);
			for (int i = 0; i < particles.length; i ++) {
				agents++;
				CAAgent a = new CASimpleAgent(linksLR, 1, new IdImpl(agents++), caLink);
				CASimAgentConstructEvent ee = new CASimAgentConstructEvent(0, a);
				em.processEvent(ee);
				a.materialize(i, 1);
				CAEvent e = new CAEvent(0, a,caLink, CAEventType.TTA);
				caNet.pushEvent(e);
			}

		}
		{
			CALink caLink = caNet.getCALink(linksRL.get(0).getId());
			CAAgent[] particles = caLink.getParticles();
//			System.out.println("part right:" + particles.length);
			for (int i = 0; i < particles.length; i ++) {
				agents++;
				CAAgent a = new CASimpleAgent(linksRL, 1, new IdImpl(-(agents++)), caLink);
				CASimAgentConstructEvent ee = new CASimAgentConstructEvent(0, a);
				em.processEvent(ee);
				a.materialize(i, -1);
				CAEvent e = new CAEvent(0, a,caLink, CAEventType.TTA);
				caNet.pushEvent(e);
			}

		}

		em.addHandler(monitor);

		try {
			caNet.run();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
		
}
