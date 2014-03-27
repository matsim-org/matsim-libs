/* *********************************************************************** *
 * project: org.matsim.*
 * BiDQ.java
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

package playground.gregor.bidirpeds.bidq;

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
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;

import playground.gregor.sim2d_v4.debugger.eventsbaseddebugger.EventBasedVisDebuggerEngine;
import playground.gregor.sim2d_v4.scenario.Sim2DConfig;
import playground.gregor.sim2d_v4.scenario.Sim2DConfigUtils;
import playground.gregor.sim2d_v4.scenario.Sim2DScenario;
import playground.gregor.sim2d_v4.scenario.Sim2DScenarioUtils;

public class BiDQ {
	
	
	
	public static void main(String [] args) {
		//MATSim scenario including network
		Config conf = ConfigUtils.createConfig();
		conf.global().setCoordinateSystem("EPSG:3395");
		Scenario sc = ScenarioUtils.createScenario(conf);
		Sim2DConfig conf2d = Sim2DConfigUtils.createConfig();
		Sim2DScenario sc2d = Sim2DScenarioUtils.createSim2dScenario(conf2d);
		sc.addScenarioElement(Sim2DScenario.ELEMENT_NAME,sc2d);
		Network net = sc.getNetwork();
		NetworkFactory fac = net.getFactory();
		Node n0 = fac.createNode(new IdImpl("0"), new CoordImpl(0,0));
		Node n1 = fac.createNode(new IdImpl("1"), new CoordImpl(10,0));
		Node n2 = fac.createNode(new IdImpl("2"), new CoordImpl(10,10));
		Node n3 = fac.createNode(new IdImpl("3"), new CoordImpl(0,10));
		net.addNode(n0);net.addNode(n1);net.addNode(n2);net.addNode(n3);
		Link l0 = fac.createLink(new IdImpl("0"), n0, n1);
		Link l1 = fac.createLink(new IdImpl("1"), n1, n2);
		Link l2 = fac.createLink(new IdImpl("2"), n2, n3);
		Link l3 = fac.createLink(new IdImpl("3"), n3, n0);
		l0.setNumberOfLanes(1);l1.setNumberOfLanes(1);l2.setNumberOfLanes(1);l3.setNumberOfLanes(3);
		l0.setLength(10);l1.setLength(10);l2.setLength(10);l3.setLength(10);
		l0.setFreespeed(1.34);l1.setFreespeed(1.34);l2.setFreespeed(1.34);l3.setFreespeed(1.34);
		net.addLink(l3);net.addLink(l2);net.addLink(l1);net.addLink(l0);
		
		
		

		//visualization stuff
		EventsManager em = new EventsManagerImpl();
		EventBasedVisDebuggerEngine vis = new EventBasedVisDebuggerEngine(sc);
		em.addHandler(vis);
		
		
	}

}
