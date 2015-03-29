/* *********************************************************************** *
 * project: org.matsim.*
 * ConvertOsmToMatsim.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

/**
 * 
 */
package org.matsim.core.mobsim.qsim.qnetsimengine;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;

import playground.southafrica.utilities.Header;

/**
 * Class to run waste collection vehicles.
 * 
 * @author jwjoubert
 */
public class WasteControler {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(WasteControler.class.toString(), args);
		
		Scenario sc = setupScenario();
		
		
		Header.printFooter();
	}
	
	private static Scenario setupScenario(){
		Config config = ConfigUtils.createConfig();
		Scenario sc = ScenarioUtils.createScenario(config);
		
		
		
		
		
		
		return sc;
	}
	
	
	private static Scenario buildNetwork(Scenario sc){
		Network network = sc.getNetwork();
		NetworkFactory nf = network.getFactory();
		
		/* Nodes */
		Node a = nf.createNode(Id.createNodeId("a"), new CoordImpl(0.0, 0.0)); network.addNode(a);
		Node b = nf.createNode(Id.createNodeId("b"), new CoordImpl(0.0, 0.0)); network.addNode(b);
		Node c = nf.createNode(Id.createNodeId("c"), new CoordImpl(0.0, 0.0)); network.addNode(c);
		Node d = nf.createNode(Id.createNodeId("d"), new CoordImpl(0.0, 0.0)); network.addNode(d);
		Node e = nf.createNode(Id.createNodeId("e"), new CoordImpl(0.0, 0.0)); network.addNode(e);
		Node f = nf.createNode(Id.createNodeId("f"), new CoordImpl(0.0, 0.0)); network.addNode(f);
		Node g = nf.createNode(Id.createNodeId("g"), new CoordImpl(0.0, 0.0)); network.addNode(g);
		Node h = nf.createNode(Id.createNodeId("h"), new CoordImpl(0.0, 0.0)); network.addNode(h);
		Node i = nf.createNode(Id.createNodeId("i"), new CoordImpl(0.0, 0.0)); network.addNode(i);
		Node j = nf.createNode(Id.createNodeId("j"), new CoordImpl(0.0, 0.0)); network.addNode(j);
		Node k = nf.createNode(Id.createNodeId("k"), new CoordImpl(0.0, 0.0)); network.addNode(k);
		Node l = nf.createNode(Id.createNodeId("l"), new CoordImpl(0.0, 0.0)); network.addNode(l);
		Node m = nf.createNode(Id.createNodeId("m"), new CoordImpl(0.0, 0.0)); network.addNode(m);
		Node n = nf.createNode(Id.createNodeId("n"), new CoordImpl(0.0, 0.0)); network.addNode(n);
		Node o = nf.createNode(Id.createNodeId("o"), new CoordImpl(0.0, 0.0)); network.addNode(o);
		Node p = nf.createNode(Id.createNodeId("p"), new CoordImpl(0.0, 0.0)); network.addNode(p);
		Node q = nf.createNode(Id.createNodeId("q"), new CoordImpl(0.0, 0.0)); network.addNode(q);
		/**
		 * Build a small grid-like scenario with two-way streets.
		 * 
		 *          n-----o-----p-----q
		 * 			|     |     |     |
		 * 			|     |     |     |
		 * 			j-----k-----l-----m
		 * 			|     |     |     |
		 * 			|     |     |     |
		 * 			f-----g-----h-----i
		 * 			|     |     |     |
		 * 			|     |     |     |
		 *   a------b-----c-----d-----e 
		 * 
		 * Each link is 200 metres, and all links are required, except the lowest
		 * line, that is ab, bc, cd, de. The waste demand on each link is 10 units,
		 * so the total demand is 21 units. 
		 * 
		 * @param sc
		 * @return
		 */
		
		/* Links */
		Link ab = nf.createLink(Id.createLinkId("ab"), a, b); network.addLink(ab);
		Link bc = nf.createLink(Id.createLinkId("bc"), b, c); network.addLink(bc);
		Link cd = nf.createLink(Id.createLinkId("cd"), c, d); network.addLink(cd);
		Link de = nf.createLink(Id.createLinkId("de"), d, e); network.addLink(de);
		
		Link bf = nf.createLink(Id.createLinkId("bf"), b, f); network.addLink(bf);
		Link cg = nf.createLink(Id.createLinkId("cg"), c, g); network.addLink(cg);
		Link dh = nf.createLink(Id.createLinkId("dh"), d, h); network.addLink(dh);
		Link ei = nf.createLink(Id.createLinkId("ei"), e, i); network.addLink(ei);
		
		Link fg = nf.createLink(Id.createLinkId("ei"), e, i); network.addLink(ei);
		Link gh = nf.createLink(Id.createLinkId("ei"), e, i); network.addLink(ei);
		Link hi = nf.createLink(Id.createLinkId("ei"), e, i); network.addLink(ei);
		
		
		
		return sc;
	}

}
