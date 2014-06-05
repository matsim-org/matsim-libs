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
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;

import playground.gregor.casim.events.CASimAgentConstructEvent;
import playground.gregor.casim.monitoring.CALinkMonitor;
import playground.gregor.casim.simulation.physics.CAEvent.CAEventType;
import playground.gregor.sim2d_v4.scenario.Sim2DConfig;
import playground.gregor.sim2d_v4.scenario.Sim2DConfigUtils;
import playground.gregor.sim2d_v4.scenario.Sim2DScenario;
import playground.gregor.sim2d_v4.scenario.Sim2DScenarioUtils;

public class CASimExperimentConflict {

	public static void main(String [] args) {
		Config c = ConfigUtils.createConfig();
		c.global().setCoordinateSystem("EPSG:3395");
		Scenario sc = ScenarioUtils.createScenario(c);

		double dist2 = 1;

		//VIS only
		Sim2DConfig conf2d = Sim2DConfigUtils.createConfig();
		Sim2DScenario sc2d = Sim2DScenarioUtils.createSim2dScenario(conf2d);
		sc.addScenarioElement(Sim2DScenario.ELEMENT_NAME,sc2d);

		Network net = sc.getNetwork();
		((NetworkImpl)net).setCapacityPeriod(1);
		NetworkFactory fac = net.getFactory();

		Node n0 = fac.createNode(new IdImpl("0"), new CoordImpl(0,0));
		Node n1 = fac.createNode(new IdImpl("1"), new CoordImpl(10,0));
		Node n2 = fac.createNode(new IdImpl("2"), new CoordImpl(10,-10));
		Node n3 = fac.createNode(new IdImpl("3"), new CoordImpl(10,-20));
		Node n4 = fac.createNode(new IdImpl("4"), new CoordImpl(0,-20));
		Node n5 = fac.createNode(new IdImpl("5"), new CoordImpl(0,-10));

		net.addNode(n5);net.addNode(n4);net.addNode(n3);net.addNode(n2);net.addNode(n1);net.addNode(n0);

		Link l0 = fac.createLink(new IdImpl("0"), n0, n1);
		Link l0rev = fac.createLink(new IdImpl("0rev"), n1, n0);
		Link l1 = fac.createLink(new IdImpl("1"), n1, n2);
		Link l1rev = fac.createLink(new IdImpl("1rev"), n2, n1);
		Link l2 = fac.createLink(new IdImpl("2"), n2, n3);
		Link l2rev = fac.createLink(new IdImpl("2rev"), n3, n2);
		Link l3 = fac.createLink(new IdImpl("3"), n3, n4);
		Link l3rev = fac.createLink(new IdImpl("3rev"), n4, n3);
		Link l4 = fac.createLink(new IdImpl("4"), n4, n5);
		Link l4rev = fac.createLink(new IdImpl("4rev"), n5, n4);
		Link l5 = fac.createLink(new IdImpl("5"), n5, n0);
		Link l5rev = fac.createLink(new IdImpl("5rev"), n0, n5);
		Link l6 = fac.createLink(new IdImpl("6"), n5, n2);
		Link l6rev = fac.createLink(new IdImpl("6rev"), n2, n5);		

		l0.setLength(10);
		l1.setLength(10);
		l2.setLength(10);
		l3.setLength(10);
		l4.setLength(10);
		l5.setLength(10);
		l6.setLength(10);
		l0rev.setLength(10);
		l1rev.setLength(10);
		l2rev.setLength(10);
		l3rev.setLength(10);
		l4rev.setLength(10);
		l5rev.setLength(10);
		l6rev.setLength(10);
		net.addLink(l6);net.addLink(l5);net.addLink(l4);net.addLink(l3);net.addLink(l2);net.addLink(l1);net.addLink(l0);
		net.addLink(l6rev);net.addLink(l5rev);net.addLink(l4rev);net.addLink(l3rev);net.addLink(l2rev);net.addLink(l1rev);net.addLink(l0rev);

		List<Link> green = new ArrayList<Link>();
		List<Link> red = new ArrayList<Link>();
		List<Link> black = new ArrayList<Link>();

		green.add(l6);
		green.add(l2);
		green.add(l3);
		green.add(l4);

		black.add(l4);
		black.add(l3);
		black.add(l2);
		black.add(l6);

		red.add(l5);
		red.add(l0);
		red.add(l1);
		red.add(l6);

		for (int rnd = 0; rnd < 40; rnd++) {
			List<Link> gSub = green.subList(0, 4);
			green.addAll(gSub);
			List<Link> bSub = black.subList(0, 4);
			black.addAll(bSub);
			List<Link> rSub = red.subList(0, 4);
			red.addAll(rSub);
		}
		CALinkMonitor m1 = new CALinkMonitor(l2.getId(), l2rev.getId(),l2);
		CALinkMonitor m2 = new CALinkMonitor(l1.getId(), l1rev.getId(),l1);
		for (double rho = .1; rho <= 1; rho += 0.1) {
			for (int i = 0; i < 100; i++) {
				runIt(net,m1,m2,green,black,red,rho,sc);
				m1.reset(0);
				m2.reset(0);
			}
		}
		System.out.println(m1);
		System.out.println("=========================");
		System.out.println(m2);
	}

	private static void runIt(Network net,CALinkMonitor m1,CALinkMonitor m2, List<Link>green,List<Link> black, List<Link> red, double rho, Scenario sc){
		//visualization stuff
		EventsManager em = new EventsManagerImpl();
//		EventBasedVisDebuggerEngine vis = new EventBasedVisDebuggerEngine(sc);
//		em.addHandler(vis);
//		vis.addAdditionalDrawer(new InfoBox(vis, sc));

		em.addHandler(m1);
		em.addHandler(m2);
		
		CANetwork.RHO = rho * 5.091;
		CANetwork caNet = new CANetwork(net,em);

		int id = 0;
		int fields = 0;
		int agents = 0;
		for (int j = 0; j  < 4; j++){
			fields++;
			CALink caLink = caNet.getCALink(green.get(j).getId());
			CAAgent[] particles = caLink.getParticles();
			for (int i = 0; i < particles.length; i ++) {
				fields++;
				if (MatsimRandom.getRandom().nextDouble() > rho) {
					continue;
				}
				agents++;
				CAAgent a;
				if (MatsimRandom.getRandom().nextBoolean() || i == 0) {
					a = new CASimpleAgent(green,j+1,new IdImpl("g"+id++),caLink);
					a.materialize(i, 1);
					CAEvent e = new CAEvent(0, a,caLink, CAEventType.TTA);
					caNet.pushEvent(e);
				} else{
					a = new CASimpleAgent(black,3-j+1,new IdImpl("b"+id++),caLink);//up till here
					a.materialize(i, -1);
					if (i >0 && particles[i-1] != null && particles[i-1].getDir() == 1) {
						CAEvent e = new CAEvent(0.01, a,caLink, CAEventType.SWAP);
						caNet.pushEvent(e);
					}else if (i == 0 && caLink.getUpstreamCANode().peekForAgent() != null && caLink.getUpstreamCANode().peekForAgent().getNextLinkId() == caLink.getLink().getId()){
						CAEvent e = new CAEvent(0.01, a,caLink, CAEventType.SWAP);
						caNet.pushEvent(e);						
					} else { 
						CAEvent e = new CAEvent(0, a,caLink, CAEventType.TTA);
						caNet.pushEvent(e);	
					}
				}
				CASimAgentConstructEvent ee = new CASimAgentConstructEvent(0, a);
				em.processEvent(ee);
				particles[i] = a;
				//				if (i == particles.length-1 || particles[i+1] == null) {
				CAEvent e = new CAEvent(0, a,caLink, CAEventType.TTA);
				caNet.pushEvent(e);	
				//				}
			}
//			CAAgent a = new CASimpleAgent(green,j+1,new IdImpl("g"+id++),caLink);
//			caLink.getDownstreamCANode().putAgent(a);
//			CAEvent e = new CAEvent(0, a,caLink.getDownstreamCANode(), CAEventType.TTA);
//			caNet.pushEvent(e);
//			CASimAgentConstructEvent ee = new CASimAgentConstructEvent(0, a);
//			em.processEvent(ee);
		}

		for (int j = 0; j  < 3; j++){
			fields++;
			CALink caLink = caNet.getCALink(red.get(j).getId());
			CAAgent[] particles = caLink.getParticles();
			for (int i = 0; i < particles.length; i ++) {
				fields++;
				if (MatsimRandom.getRandom().nextDouble() > rho) {
					continue;
				}
				agents++;
				CAAgent a;
				a = new CASimpleAgent(red,j+1,new IdImpl("r"+id++),caLink);
				a.materialize(i, 1);
				CAEvent e = new CAEvent(0, a,caLink, CAEventType.TTA);
				caNet.pushEvent(e);
				CASimAgentConstructEvent ee = new CASimAgentConstructEvent(0, a);
				em.processEvent(ee);
				particles[i] = a;
			}
		}


		
		caNet.run();
	}

}
