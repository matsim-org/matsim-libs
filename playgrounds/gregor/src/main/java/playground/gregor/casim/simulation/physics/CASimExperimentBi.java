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
import java.util.Collections;
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

import playground.gregor.casim.events.CASimAgentConstructEvent;
import playground.gregor.casim.monitoring.CALinkMonitor;
import playground.gregor.casim.simulation.physics.CAEvent.CAEventType;
import playground.gregor.sim2d_v4.debugger.eventsbaseddebugger.EventBasedVisDebuggerEngine;
import playground.gregor.sim2d_v4.debugger.eventsbaseddebugger.InfoBox;
import playground.gregor.sim2d_v4.debugger.eventsbaseddebugger.QSimDensityDrawer;
import playground.gregor.sim2d_v4.scenario.Sim2DConfig;
import playground.gregor.sim2d_v4.scenario.Sim2DConfigUtils;
import playground.gregor.sim2d_v4.scenario.Sim2DScenario;
import playground.gregor.sim2d_v4.scenario.Sim2DScenarioUtils;

public class CASimExperimentBi {

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
		List<Link> links = new ArrayList<Link>();
		List<Link> links2 = new ArrayList<Link>();
		Node n0 = fac.createNode(new IdImpl("0"), new CoordImpl(-90,0));
		Node n1 = fac.createNode(new IdImpl("1"), new CoordImpl(10,0));
		Node n2 = fac.createNode(new IdImpl("2"), new CoordImpl(14,0));
		Node n3 = fac.createNode(new IdImpl("3"), new CoordImpl(22,0));
		Node n4 = fac.createNode(new IdImpl("4"), new CoordImpl(26,0));
		Node n5 = fac.createNode(new IdImpl("5"), new CoordImpl(126,0));

		Node n6 = fac.createNode(new IdImpl("6"), new CoordImpl(14,100));
		Node n7 = fac.createNode(new IdImpl("7"), new CoordImpl(22,-100));
		Node n8 = fac.createNode(new IdImpl("8"), new CoordImpl(18,41));

		net.addNode(n8);net.addNode(n7);net.addNode(n6);net.addNode(n5);net.addNode(n4);net.addNode(n3);net.addNode(n2);net.addNode(n1);net.addNode(n0);

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
		Link l5 = fac.createLink(new IdImpl("5"), n2, n6);
		Link l5rev = fac.createLink(new IdImpl("5rev"), n6, n2);
		Link l6 = fac.createLink(new IdImpl("6"), n3, n7);
		Link l6rev = fac.createLink(new IdImpl("6rev"), n7, n3);

		Link l7 = fac.createLink(new IdImpl("7"), n4, n8);
		Link l8 = fac.createLink(new IdImpl("8"), n8, n1);
		Link l7rev = fac.createLink(new IdImpl("7rev"), n8, n4);
		Link l8rev = fac.createLink(new IdImpl("8rev"), n1, n8);

		l0.setLength(100);
		l1.setLength(4);
		l2.setLength(8);
		l3.setLength(4);
		l4.setLength(100*dist2);
		l5.setLength(100);
		l6.setLength(100);
		l7.setLength(42);
		l8.setLength(42);
		l7rev.setLength(42);
		l8rev.setLength(42);
		l0rev.setLength(100);
		l1rev.setLength(4);
		l2rev.setLength(8);
		l3rev.setLength(4);
		l4rev.setLength(100*dist2);
		l5rev.setLength(100);
		l6rev.setLength(100);
		net.addLink(l8);net.addLink(l7);net.addLink(l6);net.addLink(l5);net.addLink(l4);net.addLink(l3);net.addLink(l2);net.addLink(l1);net.addLink(l0);
		net.addLink(l8rev);net.addLink(l7rev);net.addLink(l6rev);net.addLink(l5rev);net.addLink(l4rev);net.addLink(l3rev);net.addLink(l2rev);net.addLink(l1rev);net.addLink(l0rev);


		links.add(l1);
		links.add(l2);
		links.add(l3);
		links.add(l7);
		links.add(l8);
		for (int rnd = 0; rnd < 40; rnd++) {
			links.add(l1);
			links.add(l2);
			links.add(l3);
			links.add(l7);
			links.add(l8);			
		}
		links2.add(l4);
		links2.add(l3);
		links2.add(l6);

		for (Link link : links) {
			link.setCapacity(1);
		}
		for (Link link : links2) {
			link.setCapacity(1);
		}
		CALinkMonitor monitor = new CALinkMonitor(l2.getId(), l2rev.getId(),l2);
		for (double dist = 32; dist >= 1; dist--) {
			runIt(net,monitor,links,dist,sc);
			monitor.reset(0);
		}
		System.out.println(monitor);

	}
	
	private static void runIt(Network net,CALinkMonitor monitor,List<Link>links,double dist, Scenario sc){
		//visualization stuff
		EventsManager em = new EventsManagerImpl();
		EventBasedVisDebuggerEngine vis = new EventBasedVisDebuggerEngine(sc);
		em.addHandler(vis);
		QSimDensityDrawer qDbg = new QSimDensityDrawer(sc);
		em.addHandler(qDbg);
		vis.addAdditionalDrawer(new InfoBox(vis, sc));
		vis.addAdditionalDrawer(qDbg);
		CANetwork caNet = new CANetwork(net,em);

		int id = 0;
		for (int j = 0; j  < 5; j++){
			CALink caLink = caNet.getCALink(links.get(j).getId());
			CAAgent[] particles = caLink.getParticles();
			for (int i = 0; i < particles.length; i += 2*dist) {
				CAAgent a = new CASimpleAgent(links,j+1,new IdImpl(id++),caLink);
				CASimAgentConstructEvent ee = new CASimAgentConstructEvent(0, a);
				em.processEvent(ee);
				a.materialize(i, 1);
				particles[i] = a;
				//				if (i == particles.length-1 || particles[i+1] == null) {
				CAEvent e = new CAEvent(0, a,caLink, CAEventType.TTA);
				caNet.pushEvent(e);	
				//				}
			}
		}
		ArrayList<Link> ll = new ArrayList<Link>(links);
		Collections.reverse(ll);
		for (int j = 0; j  < 5; j++){
			CALink caLink = caNet.getCALink(ll.get(j).getId());
			CAAgent[] particles = caLink.getParticles();
			for (int i = (int) dist; i < particles.length; i += 2*dist) {
				CAAgent a = new CASimpleAgent(ll,j+1,new IdImpl(id++),caLink);
				CASimAgentConstructEvent ee = new CASimAgentConstructEvent(0, a);
				em.processEvent(ee);
				a.materialize(i, -1);
				particles[i] = a;
				if (i >0 && particles[i-1] != null && particles[i-1].getDir() == 1) {
					CAEvent e = new CAEvent(0.01, a,caLink, CAEventType.SWAP);
					caNet.pushEvent(e);
				} else { 
					CAEvent e = new CAEvent(0, a,caLink, CAEventType.TTA);
					caNet.pushEvent(e);	
				}
			}
		}
		em.addHandler(monitor);
		caNet.run();
	}

}
