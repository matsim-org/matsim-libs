/* *********************************************************************** *
 * project: org.matsim.*
 * RiskCostControlerTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.evacuation.riskaversion;

import java.util.ArrayList;
import java.util.List;

import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.NetworkFactory;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.network.TimeVariantLinkFactory;
import org.matsim.core.network.NetworkChangeEvent.ChangeType;
import org.matsim.core.network.NetworkChangeEvent.ChangeValue;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.testcases.MatsimTestCase;



public class RiskCostFromNetworkChangeEventsTest extends MatsimTestCase{
	

	
	
	public void testRiskCostCalculatorNoCostsForEqualRiskLinks() {
		NetworkFactory nf = new NetworkFactory();
		nf.setLinkFactory(new TimeVariantLinkFactory());
		NetworkLayer net = new NetworkLayer(nf);
		
		NodeImpl n0 = net.createNode(new IdImpl(0), new CoordImpl(0.,0.));
		NodeImpl n1 = net.createNode(new IdImpl(1), new CoordImpl(0.,1.));
		NodeImpl n2 = net.createNode(new IdImpl(2), new CoordImpl(1.,0.));
		NodeImpl n3 = net.createNode(new IdImpl(3), new CoordImpl(1.,1.));
		NodeImpl n4 = net.createNode(new IdImpl(4), new CoordImpl(.5,.5));
		
		LinkImpl l0 = net.createLink(new IdImpl(0), n0, n1, 10., 5., 8., 5.4321);
		LinkImpl l1 = net.createLink(new IdImpl(1), n1, n2, 10., 5., 8., 5.4321);
		LinkImpl l2 = net.createLink(new IdImpl(2), n2, n3, 10., 5., 8., 5.4321);
		LinkImpl l3 = net.createLink(new IdImpl(3), n3, n4, 10., 5., 8., 5.4321);
		LinkImpl l4 = net.createLink(new IdImpl(4), n4, n0, 2.5, 5., 8., 5.4321);
		
		List<NetworkChangeEvent> nc = new ArrayList<NetworkChangeEvent>();
		
		
		//link fs change:
		// l2 -> 3:10
		// l0,l1 -> 3:20
		// n2 -> 3:10
		// n0,n1 -> 3:20
		// n0 -> n1 up
		// n1 -> n2 down
		// n2 -> n3 up
		// n3 -> n1 down
	
		
		//first event at 3:20am ==> 106800
		NetworkChangeEvent e0 = new NetworkChangeEvent(3 * 3600 + 60 * 20);
		e0.addLink(l0);
		e0.addLink(l1);
		e0.setFreespeedChange(new ChangeValue(ChangeType.ABSOLUTE,0.));
		nc.add(e0);
		
		//second event at 3:30am ==> 106200
		NetworkChangeEvent e1 = new NetworkChangeEvent(3 * 3600 + 60 * 30);
		e1.addLink(l1);
		e1.addLink(l2);
		e1.setFreespeedChange(new ChangeValue(ChangeType.ABSOLUTE,0.));		
		nc.add(e1);
		
		//third event at 3:10am ==> 107400
		NetworkChangeEvent e2 = new NetworkChangeEvent(3 * 3600 + 60 * 10);
		e2.addLink(l2);
		e2.setFreespeedChange(new ChangeValue(ChangeType.ABSOLUTE,0.));		
		nc.add(e2);
		
		net.setNetworkChangeEvents(nc);
		
		RiskCostFromNetworkChangeEvents rcc = new RiskCostFromNetworkChangeEvents(net,false);
		assertEquals("Risk cost:" , 0. , rcc.getLinkRisk(l0));
		assertEquals("Risk cost:" , 107400. * 10., rcc.getLinkRisk(l1));
		assertEquals("Risk cost:" , 0. , rcc.getLinkRisk(l2));
		assertEquals("Risk cost:" , 0. , rcc.getLinkRisk(l3));
		assertEquals("Risk cost:" , 106800. * 2.5, rcc.getLinkRisk(l4));
		
	}
	
	public void testRiskCostCalculatorChargeEqualRiskLinks() {
		NetworkFactory nf = new NetworkFactory();
		nf.setLinkFactory(new TimeVariantLinkFactory());
		NetworkLayer net = new NetworkLayer(nf);
		
		NodeImpl n0 = net.createNode(new IdImpl(0), new CoordImpl(0.,0.));
		NodeImpl n1 = net.createNode(new IdImpl(1), new CoordImpl(0.,1.));
		NodeImpl n2 = net.createNode(new IdImpl(2), new CoordImpl(1.,0.));
		NodeImpl n3 = net.createNode(new IdImpl(3), new CoordImpl(1.,1.));
		NodeImpl n4 = net.createNode(new IdImpl(4), new CoordImpl(.75,.75));
		NodeImpl n5 = net.createNode(new IdImpl(5), new CoordImpl(.25,.25));
		
		LinkImpl l0 = net.createLink(new IdImpl(0), n0, n1, 2.5, 5., 8., 5.4321);
		LinkImpl l1 = net.createLink(new IdImpl(1), n1, n2, 3.5, 5., 8., 5.4321);
		LinkImpl l2 = net.createLink(new IdImpl(2), n2, n3, 4.5, 5., 8., 5.4321);
		LinkImpl l3 = net.createLink(new IdImpl(3), n3, n4, 10., 5., 8., 5.4321);
		LinkImpl l4 = net.createLink(new IdImpl(4), n4, n5, 10., 5., 8., 5.4321);
		LinkImpl l5 = net.createLink(new IdImpl(5), n5, n0, 5.5, 5., 8., 5.4321);
		
		List<NetworkChangeEvent> nc = new ArrayList<NetworkChangeEvent>();
		
		
		//link fs change:
		// l2 -> 3:10
		// l0,l1 -> 3:20
		// n2 -> 3:10
		// n0,n1 -> 3:20
		// n0 -> n1 up
		// n1 -> n2 down
		// n2 -> n3 up
		// n3 -> n1 down
	
		
		//first event at 3:20am ==> 106800
		NetworkChangeEvent e0 = new NetworkChangeEvent(3 * 3600 + 60 * 20);
		e0.addLink(l0);
		e0.addLink(l1);
		e0.setFreespeedChange(new ChangeValue(ChangeType.ABSOLUTE,0.));
		nc.add(e0);
		
		//second event at 3:30am ==> 106200
		NetworkChangeEvent e1 = new NetworkChangeEvent(3 * 3600 + 60 * 30);
		e1.addLink(l1);
		e1.addLink(l2);
		e1.setFreespeedChange(new ChangeValue(ChangeType.ABSOLUTE,0.));		
		nc.add(e1);
		
		//third event at 3:10am ==> 107400
		NetworkChangeEvent e2 = new NetworkChangeEvent(3 * 3600 + 60 * 10);
		e2.addLink(l2);
		e2.setFreespeedChange(new ChangeValue(ChangeType.ABSOLUTE,0.));		
		nc.add(e2);
		
		net.setNetworkChangeEvents(nc);
		
		RiskCostFromNetworkChangeEvents rcc = new RiskCostFromNetworkChangeEvents(net,true);
		
		assertEquals("Risk cost:" , 106800. * 2.5, rcc.getLinkRisk(l0));
		assertEquals("Risk cost:" , 107400. * 3.5, rcc.getLinkRisk(l1));
		assertEquals("Risk cost:" , 107400. * 4.5, rcc.getLinkRisk(l2));
		assertEquals("Risk cost:" , 0. , rcc.getLinkRisk(l3));
		assertEquals("Risk cost:" , 0. , rcc.getLinkRisk(l4));
		assertEquals("Risk cost:" , 106800. * 5.5, rcc.getLinkRisk(l5));
		
	}
	
}
