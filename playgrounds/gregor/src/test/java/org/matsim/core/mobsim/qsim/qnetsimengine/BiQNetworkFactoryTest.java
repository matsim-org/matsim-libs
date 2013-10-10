/* *********************************************************************** *
 * project: org.matsim.*
 * BiQNetworkFactoryTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package org.matsim.core.mobsim.qsim.qnetsimengine;

import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.testcases.MatsimTestCase;

public class BiQNetworkFactoryTest extends MatsimTestCase{

	@Test
	public void testBiQNetworkFactory() {
		Config c = ConfigUtils.createConfig();
		
		Scenario sc = ScenarioUtils.createScenario(c);
		
		Network net = sc.getNetwork();
		NetworkFactory fac = sc.getNetwork().getFactory();
		Node n1 = fac.createNode(new IdImpl(0), new CoordImpl(0,0));
		net.addNode(n1);
		Node n2 = fac.createNode(new IdImpl(1), new CoordImpl(10,0));
		net.addNode(n2);
		Link l = fac.createLink(new IdImpl(2), n1, n2);
		net.addLink(l);
		
		
		BiQNetworkFactory biQFac = new BiQNetworkFactory();
		QNetwork qnet = new QNetwork(sc.getNetwork(), biQFac);
		
		QNetsimEngine engine = new QNetsimEngine(new QSim(sc,new EventsManagerImpl()));
		
		qnet.initialize(engine);
		
		
	}
}
