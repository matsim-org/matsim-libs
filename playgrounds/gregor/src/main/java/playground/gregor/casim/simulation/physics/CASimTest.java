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
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;

import playground.gregor.casim.events.CASimAgentConstructEvent;
import playground.gregor.casim.simulation.physics.CAEvent.CAEventType;
import playground.gregor.sim2d_v4.debugger.eventsbaseddebugger.EventBasedVisDebuggerEngine;
import playground.gregor.sim2d_v4.debugger.eventsbaseddebugger.InfoBox;
import playground.gregor.sim2d_v4.scenario.Sim2DConfig;
import playground.gregor.sim2d_v4.scenario.Sim2DConfigUtils;
import playground.gregor.sim2d_v4.scenario.Sim2DScenario;
import playground.gregor.sim2d_v4.scenario.Sim2DScenarioUtils;

public class CASimTest {

	public static void main(String [] args) {
		Config c = ConfigUtils.createConfig();
		c.global().setCoordinateSystem("EPSG:3395");
		Scenario sc = ScenarioUtils.createScenario(c);

		//VIS only
		Sim2DConfig conf2d = Sim2DConfigUtils.createConfig();
		Sim2DScenario sc2d = Sim2DScenarioUtils.createSim2dScenario(conf2d);
		sc.addScenarioElement(Sim2DScenario.ELEMENT_NAME,sc2d);

		Network net = sc.getNetwork();
		NetworkFactory fac = net.getFactory();
		List<Link> links = new ArrayList<Link>();
		{
			Node n0 = fac.createNode(Id.create("0", Node.class), new CoordImpl(0,0));
			Node n1 = fac.createNode(Id.create("1", Node.class), new CoordImpl(0,10));
			Node n2 = fac.createNode(Id.create("2", Node.class), new CoordImpl(10,10));
			Node n3 = fac.createNode(Id.create("3", Node.class), new CoordImpl(10,0));
			Node n4 = fac.createNode(Id.create("4", Node.class), new CoordImpl(10,-10));
			Node n5 = fac.createNode(Id.create("5", Node.class), new CoordImpl(20,-10));
			Node n6 = fac.createNode(Id.create("6", Node.class), new CoordImpl(20,0));
			net.addNode(n6);net.addNode(n5);net.addNode(n4);net.addNode(n3);net.addNode(n2);net.addNode(n1);net.addNode(n0);

			Link l0 = fac.createLink(Id.create("0", Link.class), n0, n1);
			Link l0rev = fac.createLink(Id.create("0rev", Link.class), n1, n0);
			Link l1 = fac.createLink(Id.create("1", Link.class), n1, n2);
			Link l1rev = fac.createLink(Id.create("1rev", Link.class), n2, n1);
			Link l2 = fac.createLink(Id.create("2", Link.class), n2, n3);
			Link l2rev = fac.createLink(Id.create("2rev", Link.class), n3, n2);
			Link l3 = fac.createLink(Id.create("3", Link.class), n3, n4);
			Link l3rev = fac.createLink(Id.create("3rev", Link.class), n4, n3);
			Link l4 = fac.createLink(Id.create("4", Link.class), n4, n5);
			Link l4rev = fac.createLink(Id.create("4rev", Link.class), n5, n4);
			Link l5 = fac.createLink(Id.create("5", Link.class), n5, n6);
			Link l5rev = fac.createLink(Id.create("5rev", Link.class), n6, n5);
			Link l6 = fac.createLink(Id.create("6", Link.class), n6, n3);
			Link l6rev = fac.createLink(Id.create("6rev", Link.class), n3, n6);
			Link l7 = fac.createLink(Id.create("7", Link.class), n3, n0);
			Link l7rev = fac.createLink(Id.create("7rev", Link.class), n0, n3);
			l0.setLength(10);
			l1.setLength(10);
			l2.setLength(10);
			l3.setLength(10);
			l4.setLength(10);
			l5.setLength(10);
			l6.setLength(10);
			l7.setLength(10);
			l0rev.setLength(10);
			l1rev.setLength(10);
			l2rev.setLength(10);
			l3rev.setLength(10);
			l4rev.setLength(10);
			l5rev.setLength(10);
			l6rev.setLength(10);
			l7rev.setLength(10);
			net.addLink(l7);net.addLink(l6);net.addLink(l5);net.addLink(l4);net.addLink(l3);net.addLink(l2);net.addLink(l1);net.addLink(l0);
			net.addLink(l7rev);net.addLink(l6rev);net.addLink(l5rev);net.addLink(l4rev);net.addLink(l3rev);net.addLink(l2rev);net.addLink(l1rev);net.addLink(l0rev);


			links.add(l0);
			links.add(l1);
			links.add(l2);
			links.add(l3);
			links.add(l4);
			links.add(l5);
			links.add(l6);
			links.add(l7);
		}
		List<Link> links2 = new ArrayList<Link>();
		{
			Node n0 = fac.createNode(Id.create("_0", Node.class), new CoordImpl(25,0));
			Node n1 = fac.createNode(Id.create("_1", Node.class), new CoordImpl(25,10));
			Node n1a = fac.createNode(Id.create("_1a", Node.class), new CoordImpl(30,10));
			Node n2 = fac.createNode(Id.create("_2", Node.class), new CoordImpl(35,10));
			Node n3 = fac.createNode(Id.create("_3", Node.class), new CoordImpl(35,0));
			Node n4 = fac.createNode(Id.create("_4", Node.class), new CoordImpl(35,-10));
			Node n5 = fac.createNode(Id.create("_5", Node.class), new CoordImpl(45,-10));
			Node n6 = fac.createNode(Id.create("_6", Node.class), new CoordImpl(45,0));
			net.addNode(n6);net.addNode(n5);net.addNode(n4);net.addNode(n3);net.addNode(n2);net.addNode(n1);net.addNode(n1a);net.addNode(n0);

			Link l0 = fac.createLink(Id.create("_0", Link.class), n0, n1);
			Link l0rev = fac.createLink(Id.create("_0rev", Link.class), n1, n0);
			Link l1a = fac.createLink(Id.create("_1a", Link.class), n1, n1a);
			Link l1reva = fac.createLink(Id.create("_1reva", Link.class), n1a, n1);
			Link l1b = fac.createLink(Id.create("_1b", Link.class), n1a, n2);
			Link l1revb = fac.createLink(Id.create("_1revb", Link.class), n2, n1a);
			Link l2 = fac.createLink(Id.create("_2", Link.class), n2, n3);
			Link l2rev = fac.createLink(Id.create("_2rev", Link.class), n3, n2);
			Link l3 = fac.createLink(Id.create("_3", Link.class), n3, n4);
			Link l3rev = fac.createLink(Id.create("_3rev", Link.class), n4, n3);
			Link l4 = fac.createLink(Id.create("_4", Link.class), n4, n5);
			Link l4rev = fac.createLink(Id.create("_4rev", Link.class), n5, n4);
			Link l5 = fac.createLink(Id.create("_5", Link.class), n5, n6);
			Link l5rev = fac.createLink(Id.create("_5rev", Link.class), n6, n5);
			Link l6 = fac.createLink(Id.create("_6", Link.class), n6, n3);
			Link l6rev = fac.createLink(Id.create("_6rev", Link.class), n3, n6);
			Link l7 = fac.createLink(Id.create("_7", Link.class), n3, n0);
			Link l7rev = fac.createLink(Id.create("_7rev", Link.class), n0, n3);
			l0.setLength(10);
			l1a.setLength(5);
			l1b.setLength(5);
			l2.setLength(10);
			l3.setLength(10);
			l4.setLength(10);
			l5.setLength(10);
			l6.setLength(10);
			l7.setLength(10);
			l0rev.setLength(10);
			l1reva.setLength(5);
			l1revb.setLength(5);
			l2rev.setLength(10);
			l3rev.setLength(10);
			l4rev.setLength(10);
			l5rev.setLength(10);
			l6rev.setLength(10);
			l7rev.setLength(10);
			net.addLink(l7);net.addLink(l6);net.addLink(l5);net.addLink(l4);net.addLink(l3);net.addLink(l2);net.addLink(l1a);net.addLink(l1b);net.addLink(l0);
			net.addLink(l7rev);net.addLink(l6rev);net.addLink(l5rev);net.addLink(l4rev);net.addLink(l3rev);net.addLink(l2rev);net.addLink(l1reva);net.addLink(l1revb);net.addLink(l0rev);


			links2.add(l0);
			links2.add(l1a);
			links2.add(l1b);
			links2.add(l2);
			links2.add(l3);
			links2.add(l4);
			links2.add(l5);
			links2.add(l6);
			links2.add(l7);
		}



		//visualization stuff
		EventsManager em = new EventsManagerImpl();
		EventBasedVisDebuggerEngine vis = new EventBasedVisDebuggerEngine(sc);
		em.addHandler(vis);
		vis.addAdditionalDrawer(new InfoBox(vis, sc));

		CANetwork caNet = new CANetwork(net,em);

		{
			CALink caLink = caNet.getCALink(links.get(0).getId());
			CAAgent[] particles = caLink.getParticles();
			for (int i = 0; i < particles.length; i++) {
				CAAgent a = new CASimpleAgent(links,1,Id.create(i, CAAgent.class),caLink);
				CASimAgentConstructEvent ee = new CASimAgentConstructEvent(0, a);
				em.processEvent(ee);
				a.materialize(i, 1);
				particles[i] = a;
				if (i == particles.length-1) {
					CAEvent e = new CAEvent(0, a,caLink, CAEventType.TTA);
					caNet.pushEvent(e);	
				}
			}
		}
		{
			CALink caLink = caNet.getCALink(links.get(links.size()-1).getId());
			CAAgent[] particles = caLink.getParticles();
			ArrayList<Link> ll = new ArrayList<Link>(links);
			Collections.reverse(ll);
			for (int i = 0; i < particles.length; i++) {
				CAAgent a = new CASimpleAgent(ll,1,Id.create(-i-1, CAAgent.class),caLink);
				CASimAgentConstructEvent ee = new CASimAgentConstructEvent(0, a);
				em.processEvent(ee);
				a.materialize(i, -1);
				particles[i] = a;
				if (i == 0) {
					CAEvent e = new CAEvent(0, a,caLink, CAEventType.TTA);
					caNet.pushEvent(e);	
				}
			}
		}		

		{
			CALink caLink = caNet.getCALink(links2.get(0).getId());
			CAAgent[] particles = caLink.getParticles();
			for (int i = 0; i < particles.length; i++) {
				CAAgent a = new CASimpleAgent(links2,1,Id.create(i+1000, CAAgent.class),caLink);
				CASimAgentConstructEvent ee = new CASimAgentConstructEvent(0, a);
				em.processEvent(ee);
				a.materialize(i, 1);
				particles[i] = a;
				if (i == particles.length-1) {
					CAEvent e = new CAEvent(0, a,caLink, CAEventType.TTA);
					caNet.pushEvent(e);	
				}
			}
		}
		{
			CALink caLink = caNet.getCALink(links2.get(links2.size()-1).getId());
			CAAgent[] particles = caLink.getParticles();
			ArrayList<Link> ll = new ArrayList<Link>(links2);
			Collections.reverse(ll);
			for (int i = 0; i < particles.length; i++) {
				CAAgent a = new CASimpleAgent(ll,1,Id.create(-i-1000, CAAgent.class),caLink);
				CASimAgentConstructEvent ee = new CASimAgentConstructEvent(0, a);
				em.processEvent(ee);
				a.materialize(i, -1);
				particles[i] = a;
				if (i == 0) {
					CAEvent e = new CAEvent(0, a,caLink, CAEventType.TTA);
					caNet.pushEvent(e);	
				}
			}
		}
//		em.addHandler(new CALinkMonitor(l2.getId(), l2rev.getId()));

		caNet.run();
	}

}
