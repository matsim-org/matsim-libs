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
import org.matsim.core.network.NodeImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;

import playground.gregor.casim.events.CASimAgentConstructEvent;
import playground.gregor.casim.monitoring.CALinkMonitorExact;
import playground.gregor.casim.monitoring.CALinkMonitorII;
import playground.gregor.casim.simulation.physics.CAEvent.CAEventType;
import playground.gregor.sim2d_v4.debugger.eventsbaseddebugger.EventBasedVisDebuggerEngine;
import playground.gregor.sim2d_v4.debugger.eventsbaseddebugger.InfoBox;
import playground.gregor.sim2d_v4.debugger.eventsbaseddebugger.QSimDensityDrawer;
import playground.gregor.sim2d_v4.scenario.Sim2DConfig;
import playground.gregor.sim2d_v4.scenario.Sim2DConfigUtils;
import playground.gregor.sim2d_v4.scenario.Sim2DScenario;
import playground.gregor.sim2d_v4.scenario.Sim2DScenarioUtils;

public class CASimDynamicExperiment_ZhangJ2012aIII {


	//	0.50 0.50 0.65 0.65 0.75 0.75 0.85 0.85 1.00 1.00 0.50 0.50 0.75 0.75 0.90 0.90 1.20 1.20 1.60 1.60 2.00 2.00 2.50 2.50
	private static final List<Setting> settings = new ArrayList<Setting>();

	public static final boolean VIS = false ;

	private static BufferedWriter bw2;
	private static int it = 0;

	static {

		CASimDynamicExperiment_ZhangJ2011.VIS = VIS;

		try {
			bw2 =  new BufferedWriter(new FileWriter(new File("/Users/laemmel/devel/bipedca/plot_dynamicII/bi_flow")));
		} catch (IOException e) {
			e.printStackTrace();
		}
//		settings.add(new Setting(.61,.61,.61));
		for (double w = .61; w < 2; w *=3) {
			settings.add(new Setting(w,w,w));

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

	public static void main(String [] args) throws IOException {

		double timeOffset = 0;

		for (int skip = 1; skip < 10; skip++ ) {
			for (Setting s : settings){

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


				Node n0 = fac.createNode(Id.createNodeId("0"), new CoordImpl(-100,0));
				Node n2ex = fac.createNode(Id.createNodeId("2ex"), new CoordImpl(-100,100));


				Node n3ex = fac.createNode(Id.createNodeId("3ex"), new CoordImpl(100,-100));
				Node n5 = fac.createNode(Id.createNodeId("5"), new CoordImpl(100,0));

				net.addNode(n2ex);net.addNode(n3ex);net.addNode(n5);net.addNode(n0);

				Link l0 = fac.createLink(Id.createLinkId("0"), n0, n5);
				Link l0rev = fac.createLink(Id.createLinkId("0rev"), n5, n0);
				Link l2ex = fac.createLink(Id.createLinkId("2ex"), n0, n2ex);

				Link l3ex = fac.createLink(Id.createLinkId("3ex"), n5, n3ex);



				l0.setLength(200);
				l0rev.setLength(200);
				l2ex.setLength(100);
				l3ex.setLength(100);



				net.addLink(l0);
				net.addLink(l0rev);
				net.addLink(l2ex);
				net.addLink(l3ex);





				double bL = s.bL;
				double bCor = s.bCor;
				double bR = s.bR;




				l0.setCapacity(bCor);
				l0rev.setCapacity(bCor);
				l2ex.setCapacity(bCor);
				l3ex.setCapacity(bCor);

				List<Link> linksLR = new ArrayList<Link>();
				linksLR.add(l0);
				linksLR.add(l3ex);



				List<Link> linksRL = new ArrayList<Link>();
				linksRL.add(l0);
				linksRL.add(l2ex);

				System.out.println(" " + bL + " " + bCor + " " + bR +"\n");

				CALinkMonitorII mon = new CALinkMonitorII(l0.getId(), l0rev.getId(), l0.getLength(), l0.getCapacity(),timeOffset);

				runIt(net,linksLR,linksRL,sc,s,mon,skip);

			}
		}
		bw2.close();
	}

	private static void runIt(Network net,List<Link>linksLR, List<Link> linksRL, Scenario sc, Setting s, CALinkMonitorII mon, int skip){
		//visualization stuff
		EventsManager em = new EventsManagerImpl();
		em.addHandler(mon);

		if (VIS)  {
			EventBasedVisDebuggerEngine vis = new EventBasedVisDebuggerEngine(sc);
			em.addHandler(vis);
			QSimDensityDrawer qDbg = new QSimDensityDrawer(sc);
			em.addHandler(qDbg);
			vis.addAdditionalDrawer(new InfoBox(vis, sc));
			vis.addAdditionalDrawer(qDbg);
		}
		CANetworkDynamic caNet = new CANetworkDynamic(net,em);




		int agents = 0;
		{
			CALink caLinkLR = caNet.getCALink(linksLR.get(0).getId());
			CAAgent[] particles = caLinkLR.getParticles();
			System.out.println("part left:" + particles.length);
			for (int i = 1; i < particles.length; i+=skip) {
				//				if (MatsimRandom.getRandom().nextDouble() < 0.5) {
				//					continue;
				//				}
				//				
				if (agents % 2 == 0){ //(MatsimRandom.getRandom().nextBoolean()) {
					CAAgent a = new CASimpleDynamicAgent(linksLR, 1, Id.create("g"+agents++, CASimpleDynamicAgent.class), caLinkLR);
					a.materialize(i, 1);
					particles[i] = a;
					CASimAgentConstructEvent ee = new CASimAgentConstructEvent(0, a);
					em.processEvent(ee);
					LinkEnterEvent ee2 = new LinkEnterEvent(0,a.getId(),Id.createLinkId("0"),a.getId());
					em.processEvent(ee2);
					//					CAEvent e = new CAEvent(1/(CANetworkDynamic.V_HAT*CANetworkDynamic.RHO_HAT), a,caLinkLR, CAEventType.TTA);
					CAEvent e = new CAEvent(0, a,caLinkLR, CAEventType.TTA);
					caNet.pushEvent(e);
					caNet.registerAgent(a);
				} else {
					CAAgent a = new CASimpleDynamicAgent(linksRL, 1, Id.create("r"+-(agents++), CASimpleDynamicAgent.class), caLinkLR);
					a.materialize(i, -1);
					particles[i] = a;
					CASimAgentConstructEvent ee = new CASimAgentConstructEvent(0, a);
					em.processEvent(ee);
					LinkEnterEvent ee2 = new LinkEnterEvent(0,a.getId(),Id.createLinkId("0rev"),a.getId());
					em.processEvent(ee2);
					//					CAEvent e = new CAEvent(1/(CANetworkDynamic.V_HAT*CANetworkDynamic.RHO_HAT), a,caLinkLR, CAEventType.TTA);
					if (particles[i-1] != null && particles[i-1].getDir() == 1) {
						CAEvent e = new CAEvent(0, a,caLinkLR, CAEventType.SWAP);
						caNet.pushEvent(e);
					} else {
						CAEvent e = new CAEvent(0, a,caLinkLR, CAEventType.TTA);
						caNet.pushEvent(e);
					}
					caNet.registerAgent(a);
				}
			}
		}




		//		List<CALinkDynamic> links = new ArrayList<CALinkDynamic>();
		//		links.add((CALinkDynamic) caNet.getCALink(Id.createLinkId("0a")));
		//		links.add((CALinkDynamic) caNet.getCALink(Id.createLinkId("4a")));
		//		LinkFlowController lfc = new LinkFlowController(Id.createLinkId("2"),Id.createLinkId("2rev"),caNet.getCALink(Id.createLinkId("2")).getLink() , links);
		//		em.addHandler(lfc);
		//		em.addHandler(monitor);
		//		monitor.setCALinkDynamic((CALinkDynamic)caNet.getCALink(new IdImpl("2")));

		CALinkMonitorExact monitor = new CALinkMonitorExact(caNet.getCALink(Id.createLinkId("0")));
		caNet.addMonitor(monitor);
		monitor.init();
		caNet.run();
		try {
			monitor.report(bw2);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
