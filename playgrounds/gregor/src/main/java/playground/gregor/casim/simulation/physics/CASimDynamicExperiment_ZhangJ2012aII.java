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
import playground.gregor.casim.monitoring.CALinkMonitorII;
import playground.gregor.casim.simulation.physics.CAEvent.CAEventType;
import playground.gregor.sim2d_v4.debugger.eventsbaseddebugger.EventBasedVisDebuggerEngine;
import playground.gregor.sim2d_v4.debugger.eventsbaseddebugger.InfoBox;
import playground.gregor.sim2d_v4.debugger.eventsbaseddebugger.QSimDensityDrawer;
import playground.gregor.sim2d_v4.scenario.Sim2DConfig;
import playground.gregor.sim2d_v4.scenario.Sim2DConfigUtils;
import playground.gregor.sim2d_v4.scenario.Sim2DScenario;
import playground.gregor.sim2d_v4.scenario.Sim2DScenarioUtils;

public class CASimDynamicExperiment_ZhangJ2012aII {


	//	0.50 0.50 0.65 0.65 0.75 0.75 0.85 0.85 1.00 1.00 0.50 0.50 0.75 0.75 0.90 0.90 1.20 1.20 1.60 1.60 2.00 2.00 2.50 2.50
	private static final List<Setting> settings = new ArrayList<Setting>();

	public static final boolean VIS = true;
	
	private static BufferedWriter bw2;
	private static int it = 0;

	static {

		CASimDynamicExperiment_ZhangJ2011.VIS = VIS;
		//		settings.add(new Setting(1,1,1));
		//		settings.add(new Setting(2,2,2));
		//		settings.add(new Setting(10,10,1));
		//		settings.add(new Setting(1,1,.9));
		//		settings.add(new Setting(1,1,.8));
		//		settings.add(new Setting(1,1,.7));
		//		settings.add(new Setting(1,1,.61));
		//		settings.add(new Setting(1,1,.5));
		//		settings.add(new Setting(1,1,.1));
		//		settings.add(new Setting(1,1,.01));
		//		settings.add(new Setting(1,1,.001));
		//		settings.add(new Setting(3.6,3.6,.2));
		//		settings.add(new Setting(3.6,3.6,.5));


		//		
		//		for (double bL = 1.2; bL <= 4; bL += 0.5) {
		//			for (double bCor = 0.61; bCor <= bL*1.2; bCor += 0.5) {
		//				for (double bEx = bL; bEx >= bL*0.5; bEx *= 0.9) {
		//					if (bEx > bCor) {
		//						continue;
		//					}
		//					if (bL > bCor) {
		//						continue;
		//					}
		//					if (bEx > bL) {
		//						continue;
		//					}
		//					settings.add(new Setting(bL,bCor,bEx));
		//				}
		//			}
		//		}
		//		for (double bL = 1.2; bL <= 4; bL += 0.5) {
		//			for (double bEx = bL; bEx >= bL*0.5; bEx *= 0.9) {
		//				double bCor = bL;
		//				if (bEx > bL) {
		//					continue;
		//				}
		//				settings.add(new Setting(bL,bCor,bEx));
		//			}
		//		}
		//		for (double bL = 1.2; bL <= 4; bL += 1.5) {
		//			for (double bEx = bL*0.8; bEx >= bL*0.65; bEx -= 0.01) {
		//				double bCor = bL;
		//				if (bEx > bL) {
		//					continue;
		//				}
		//				settings.add(new Setting(bL,bCor,bEx));
		//			}
		//		}
		//		
		//		
		//		
		//		settings.add(new Setting(4*1.8,4*1.8,4*.95));
		//		settings.add(new Setting(1.8,1.8,.95));

		////	

		try {
			bw2 =  new BufferedWriter(new FileWriter(new File("/Users/laemmel/devel/bipedca/plot_dynamicII/zhangJ2012")));
		} catch (IOException e) {
			e.printStackTrace();
		}
//		int i = 0;
//		while ( i < 500) {
//			double r0 = MatsimRandom.getRandom().nextGaussian()+2;
//			double r1 = MatsimRandom.getRandom().nextGaussian()+2;
//			double r2 = MatsimRandom.getRandom().nextGaussian()+2;
//			if (r0 > 5 || r0 < 0.61) {
//				continue;
//			}
//			if (r2 > 5 || r2 < 0.61) {
//				continue;
//			}
//			if (r1 > 5 || r1 < 0.61 || r1 < 2*(r0)) {
//				continue;
//			}
//			settings.add(new Setting(r0,r1,r0));
//			i++;
//			
//		}
//				settings.add(new Setting(1.8,1.8,.7));
//				settings.add(new Setting(2.4,2.4,1.0));
//				settings.add(new Setting(.5,1.8,1.8));
//				settings.add(new Setting(.6,1.8,1.8));
//				settings.add(new Setting(.7,1.8,1.8));
//				settings.add(new Setting(1.,1.8,1.8));
//				settings.add(new Setting(1.45,1.8,1.8));
//				settings.add(new Setting(1.8,1.8,1.8));
//				settings.add(new Setting(1.8,1.8,1.2));
//				settings.add(new Setting(.8,3.,3.));
//				settings.add(new Setting(1.,3.,3.));
//				settings.add(new Setting(1.8,3.,3.));
//
////		for (double i = 4.; i >= 0.5; i-=0.1) {
////			settings.add(new Setting(3.6,3.6,i));	
////		}
////		for (double i = 2.8; i >= 0.5; i-=0.1) {
////			settings.add(new Setting(3.6,2.4,i));	
////		}
////		for (double i = 4.4; i >= 0.5; i-=0.1) {
////			settings.add(new Setting(3.6,4,i));	
////		}
//				settings.add(new Setting(1.8,1.8,1.15));
//				settings.add(new Setting(1.8,1.8,1.1));
//				settings.add(new Setting(1.8,1.8,1.05));
//				settings.add(new Setting(1.8,1.8,1.0));
//				settings.add(new Setting(3.6,3.6,2.1));
//				settings.add(new Setting(1.8,1.8,0.95));
				
//		settings.add(new Setting(0.61,1.4,0.61));
//				
//		for (double cor =5; cor > 1; cor -= .5) {
//			for (double d = 0.61; d < 0.75*cor; d+=0.1) {
//				settings.add(new Setting(d,cor,d));	
//			}					
//		}
//		for (int i = 0; i < 1; i++) {
			settings.add(new Setting(0.61,1.61,0.61));
//		}
//		for (double cor = 1.5; cor < 1.55; cor+=0.001) {
//			settings.add(new Setting(.61,cor,.61));	
//		}					
				
////				
//				settings.add(new Setting(.5,3.,.5));
//				settings.add(new Setting(.65,3.,.65));
//				settings.add(new Setting(.75,3.,.75));
//				settings.add(new Setting(.85,3.,.85));
//				settings.add(new Setting(1,3.,1));
//				settings.add(new Setting(.5,3.6,.5));
//				settings.add(new Setting(.75,3.6,.75));
//				settings.add(new Setting(.9,3.6,.9));
//				settings.add(new Setting(1.2,3.6,1.2));
//				settings.add(new Setting(1.6,3.6,1.6));
//				settings.add(new Setting(1.7,3.6,1.7));
//				settings.add(new Setting(1.8,3.6,1.8));
//				settings.add(new Setting(1.85,3.6,1.85));
//				settings.add(new Setting(1.9,3.6,1.9));
//				settings.add(new Setting(2.,3.6,2.));
//				settings.add(new Setting(2.5,3.6,2.5));
//				settings.add(new Setting(.61,3.6,.61));
//				settings.add(new Setting(1.61,3.6,1.61));
//				settings.add(new Setting(1.7,3.6,1.7));
//				settings.add(new Setting(1.8,3.6,1.8));
//				settings.add(new Setting(2,3.6,2));
//				settings.add(new Setting(3,3.6,3));
//				settings.add(new Setting(1,3.6,1));
//				settings.add(new Setting(.75,3.6,.75));
//				settings.add(new Setting(1.8,1.8,.7));
//				settings.add(new Setting(3.,3.,1.5));
//				settings.add(new Setting(1.8,1.8,.7));
//				settings.add(new Setting(2.4,2.4,1.0));
//				settings.add(new Setting(.5,1.8,1.8));
//				settings.add(new Setting(.6,1.8,1.8));
//				settings.add(new Setting(.7,1.8,1.8));
//				settings.add(new Setting(1.,1.8,1.8));
//				settings.add(new Setting(1.45,1.8,1.8));
//				settings.add(new Setting(1.8,1.8,1.8));
//				settings.add(new Setting(1.8,1.8,1.2));
//				settings.add(new Setting(.65,2.4,2.4));
//				settings.add(new Setting(.8,2.4,2.4));
//				settings.add(new Setting(.95,2.4,2.4));
//				settings.add(new Setting(1.45,2.4,2.4));
//				settings.add(new Setting(1.9,2.4,2.4));
//				settings.add(new Setting(2.4,2.4,2.4));
//				settings.add(new Setting(2.4,2.4,1.6));
//				settings.add(new Setting(2.4,3.,3.));
//				settings.add(new Setting(3.,3.,3.));
//				settings.add(new Setting(3.,3.,1.6));
//				settings.add(new Setting(3.,3.,1.2));


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
			Node n1a = fac.createNode(Id.createNodeId("1a"), new CoordImpl(-1,0));
			Node n1 = fac.createNode(Id.createNodeId("1"), new CoordImpl(0,0));
			Node n2 = fac.createNode(Id.createNodeId("2"), new CoordImpl(4,0));
			Node n2ex = fac.createNode(Id.createNodeId("2ex"), new CoordImpl(4,100));
			
			
			Node n3 = fac.createNode(Id.createNodeId("3"), new CoordImpl(12,0));
			Node n3ex = fac.createNode(Id.createNodeId("3ex"), new CoordImpl(12,-100));
			Node n4 = fac.createNode(Id.createNodeId("4"), new CoordImpl(16,0));
			Node n4a = fac.createNode(Id.createNodeId("4a"), new CoordImpl(17,0));
			Node n5 = fac.createNode(Id.createNodeId("5"), new CoordImpl(116,0));

			net.addNode(n1a);net.addNode(n4a);net.addNode(n2ex);net.addNode(n3ex);net.addNode(n5);net.addNode(n4);net.addNode(n3);net.addNode(n2);net.addNode(n1);net.addNode(n0);

			Link l0 = fac.createLink(Id.createLinkId("0"), n0, n1a);
			Link l0rev = fac.createLink(Id.createLinkId("0rev"), n1a, n0);
			Link l0a = fac.createLink(Id.createLinkId("0a"), n1a, n1);
			Link l0arev = fac.createLink(Id.createLinkId("0arev"), n1, n1a);
			Link l1 = fac.createLink(Id.createLinkId("1"), n1, n2);
			Link l1rev = fac.createLink(Id.createLinkId("1rev"), n2, n1);
			Link l2 = fac.createLink(Id.createLinkId("2"), n2, n3);
			Link l2rev = fac.createLink(Id.createLinkId("2rev"), n3, n2);


			Link l2ex = fac.createLink(Id.createLinkId("2ex"), n1, n2ex);
			
			Link l3 = fac.createLink(Id.createLinkId("3"), n3, n4);
			Link l3ex = fac.createLink(Id.createLinkId("3ex"), n4, n3ex);
			Link l3rev = fac.createLink(Id.createLinkId("3rev"), n4, n3);
			
			Link l4a = fac.createLink(Id.createLinkId("4a"), n4, n4a);
			Link l4arev = fac.createLink(Id.createLinkId("4arev"), n4a, n4);
			Link l4 = fac.createLink(Id.createLinkId("4"), n4a, n5);
			Link l4rev = fac.createLink(Id.createLinkId("4rev"), n5, n4a);





			l0.setLength(99);
			l0rev.setLength(99);
			l0a.setLength(1);
			l0arev.setLength(1);
			l1.setLength(4);
			l1rev.setLength(4);
			l2.setLength(8);
			l2rev.setLength(8);
			l3.setLength(4);
			l3rev.setLength(4);
			l4a.setLength(1);
			l4arev.setLength(1);
			l4.setLength(99);
			l4rev.setLength(99);
			l2ex.setLength(100);
			l3ex.setLength(100);

			
			
			net.addLink(l0);
			net.addLink(l0rev);
			net.addLink(l0a);
			net.addLink(l0arev);
			net.addLink(l1);
			net.addLink(l1rev);
			net.addLink(l2);
			net.addLink(l2rev);
			net.addLink(l3);
			net.addLink(l3rev);
			net.addLink(l4a);
			net.addLink(l4arev);
			net.addLink(l4);
			net.addLink(l4rev);
			net.addLink(l2ex);
			net.addLink(l3ex);


			


			double bL = s.bL;
			double bCor = s.bCor;
			double bR = s.bR;



			double size = 500;
			double width = bCor;
			double ratio = CANetworkDynamic.PED_WIDTH/width;
			double cellLength = ratio/(CANetworkDynamic.RHO_HAT*CANetworkDynamic.PED_WIDTH);
			double length = size*cellLength;

			double width2 = bCor;
			double ratio2 = CANetworkDynamic.PED_WIDTH/width2;
			double cellLength2 = ratio2/(CANetworkDynamic.RHO_HAT*CANetworkDynamic.PED_WIDTH);
			double length2 = size/2*cellLength2;
			l3ex.setLength(length2*8);
			l2ex.setLength(length2*8);

			l0.setLength(length);;
			l0rev.setLength(length);
			((CoordImpl)((NodeImpl)n0).getCoord()).setX(-1-length);
			((CoordImpl)((NodeImpl)n5).getCoord()).setX(17+length);
			
			l0.setCapacity(bCor);
			l0rev.setCapacity(bCor);
			
			l4.setLength(length);
			l4rev.setLength(length);
			l4.setCapacity(bCor);
			l4rev.setCapacity(bCor);
			l4a.setCapacity(bR);
			l4arev.setCapacity(bR);
			l3.setCapacity(bL);
			l3rev.setCapacity(bL);
			
			l0a.setCapacity(bL);
			l0arev.setCapacity(bL);
			l1.setCapacity(bL);
			l1rev.setCapacity(bL);
			
			l2.setCapacity(bCor);
			//			l2a.setCapacity(bCor);
			//			l2arev.setCapacity(bCor);
			//			l2b.setCapacity(bCor);
			//			l2brev.setCapacity(bCor);
			l2rev.setCapacity(bCor);
			//			l2ex.setCapacity(B_exit);
			//			l3ex.setCapacity(B_exit);
			l2ex.setCapacity(bCor);
			l3ex.setCapacity(bCor);

			List<Link> linksLR = new ArrayList<Link>();
			linksLR.add(l0);
			linksLR.add(l0a);
			linksLR.add(l1);
			linksLR.add(l2);
			linksLR.add(l3);
//			linksLR.add(l4a);
//			linksLR.add(l4);
			linksLR.add(l3ex);
			
			
			
			List<Link> linksRL = new ArrayList<Link>();
			linksRL.add(l4);
			linksRL.add(l4a);
			linksRL.add(l3);
			linksRL.add(l2);
			linksRL.add(l1);
//			linksRL.add(l0a);
//			linksRL.add(l0);
			linksRL.add(l2ex);
			
			System.out.println(" " + bL + " " + bCor + " " + bR +"\n");
			
			CALinkMonitorII mon = new CALinkMonitorII(l2.getId(), l2rev.getId(), l2.getLength(), l2.getCapacity(),timeOffset);
			
			
			runIt(net,linksLR,linksRL,sc,s,mon);

			timeOffset = mon.report(bw2);
			bw2.flush();
		}
		bw2.close();
	}

	private static void runIt(Network net,List<Link>linksLR, List<Link> linksRL, Scenario sc, Setting s, CALinkMonitorII mon){
		//visualization stuff
		EventsManager em = new EventsManagerImpl();
		em.addHandler(mon);
		////		//		if (iter == 9)

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
		int skip = 12;

		{
			CALink caLink = caNet.getCALink(linksRL.get(0).getId());
			CAMoveableEntity[] particles = caLink.getParticles();
			System.out.println("part left:" + particles.length);
			CAMoveableEntity last = null;
			for (int i = particles.length-1; i > skip; i--) {
				CAMoveableEntity a = new CASimpleDynamicAgent(linksRL, 1, Id.create(agents++, CASimpleDynamicAgent.class), caLink);
				a.materialize(i, -1);
				particles[i] = a;
				CASimAgentConstructEvent ee = new CASimAgentConstructEvent(0, a);
				em.processEvent(ee);
				
				CAEvent e = new CAEvent(1/(CANetworkDynamic.V_HAT*CANetworkDynamic.RHO_HAT), a,caLink, CAEventType.TTA);
				caNet.pushEvent(e);
				last = a; 
				caNet.registerAgent(a);
			}
			System.out.println(last);
		}
		
		{
			CALink caLink = caNet.getCALink(linksLR.get(0).getId());
			CAMoveableEntity[] particles = caLink.getParticles();
			System.out.println("part left:" + particles.length);
			CAMoveableEntity last = null;
			for (int i = 0; i < particles.length-skip; i++) {
				CAMoveableEntity a = new CASimpleDynamicAgent(linksLR, 1, Id.create(agents++, CASimpleDynamicAgent.class), caLink);
				a.materialize(i, 1);
				particles[i] = a;
				CASimAgentConstructEvent ee = new CASimAgentConstructEvent(0, a);
				em.processEvent(ee);
				CAEvent e = new CAEvent(1/(CANetworkDynamic.V_HAT*CANetworkDynamic.RHO_HAT), a,caLink, CAEventType.TTA);
				caNet.pushEvent(e);
				last = a;
				caNet.registerAgent(a);
			}
			System.out.println(last);
		}
		
		

//		List<CALinkDynamic> links = new ArrayList<CALinkDynamic>();
//		links.add((CALinkDynamic) caNet.getCALink(Id.createLinkId("0a")));
//		links.add((CALinkDynamic) caNet.getCALink(Id.createLinkId("4a")));
//		LinkFlowController lfc = new LinkFlowController(Id.createLinkId("2"),Id.createLinkId("2rev"),caNet.getCALink(Id.createLinkId("2")).getLink() , links);
//		em.addHandler(lfc);
		//		em.addHandler(monitor);
		//		monitor.setCALinkDynamic((CALinkDynamic)caNet.getCALink(new IdImpl("2")));
		caNet.run();
	}

}
