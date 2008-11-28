/* *********************************************************************** *
 * project: org.matsim.*
 * AbstractLeastCostPathCalculatorTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package org.matsim.router;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.basic.v01.IdImpl;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.network.Node;
import org.matsim.router.util.LeastCostPathCalculator;
import org.matsim.router.util.LeastCostPathCalculator.Path;
import org.matsim.testcases.MatsimTestCase;
import org.xml.sax.SAXException;

/**
 * @author mrieser
 */
public abstract class AbstractLeastCostPathCalculatorTest extends MatsimTestCase {
	
	protected abstract LeastCostPathCalculator getLeastCostPathCalculator(final NetworkLayer network);
	
	public void testCalcLeastCostPath() throws SAXException, ParserConfigurationException, IOException {
		loadConfig(null);
		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).parse("test/scenarios/equil/network.xml");
		Node node12 = network.getNode(new IdImpl("12"));
		Node node15 = network.getNode(new IdImpl("15"));

		LeastCostPathCalculator routerAlgo = getLeastCostPathCalculator(network);
		Path path = routerAlgo.calcLeastCostPath(node12, node15, 8.0*3600);

		assertEquals("number of nodes wrong.", 4, path.nodes.size());
		assertEquals("number of links wrong.", 3, path.links.size());
		assertEquals(network.getNode(new IdImpl("12")), path.nodes.get(0));
		assertEquals(network.getNode(new IdImpl("13")), path.nodes.get(1));
		assertEquals(network.getNode(new IdImpl("14")), path.nodes.get(2));
		assertEquals(network.getNode(new IdImpl("15")), path.nodes.get(3));
		assertEquals(network.getLink(new IdImpl("20")), path.links.get(0));
		assertEquals(network.getLink(new IdImpl("21")), path.links.get(1));
		assertEquals(network.getLink(new IdImpl("22")), path.links.get(2));
	}
}
