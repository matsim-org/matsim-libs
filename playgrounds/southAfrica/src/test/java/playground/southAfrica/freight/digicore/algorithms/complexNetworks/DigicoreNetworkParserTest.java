/* *********************************************************************** *
 * project: org.matsim.*
 * DigicoreNetworkParserTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.southAfrica.freight.digicore.algorithms.complexNetworks;


import java.io.IOException;
import java.util.Locale;
import java.util.TimeZone;

import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.testcases.MatsimTestCase;

import playground.southafrica.freight.digicore.algorithms.complexNetwork.DigicoreNetworkParser;
import playground.southafrica.freight.digicore.containers.DigicoreActivity;
import playground.southafrica.freight.digicore.containers.DigicoreNetwork;
import edu.uci.ics.jung.graph.util.Pair;

public class DigicoreNetworkParserTest extends MatsimTestCase {

	public void testParseNetwork(){
		DigicoreNetwork dn1 = buildSmallNetwork();

		DigicoreNetworkParser dnp = new DigicoreNetworkParser();
		DigicoreNetwork dn2 = null;
		try {
			dn2 = dnp.parseNetwork(getClassInputDirectory() + "network.txt.gz");
		} catch (IOException e) {
			fail("Should not have caught an IOException in reading input file.");
		}
		
		/* Test nodes. */
		for(Id node : dn1.getVertices()){
			assertTrue("Couldn't find node " + node.toString() + " in output network.", dn2.containsVertex(node));
		}
		for(Id node : dn2.getVertices()){
			assertTrue("Couldn't find node " + node.toString() + " in input network.", dn1.containsVertex(node));
			assertTrue("Wrong coordinate for node " + node.toString(), dn1.getCoordinates().get(node).equals(dn2.getCoordinates().get(node)));
		}
		
		for(Pair<Id> arc : dn1.getEdges()){
			assertTrue("Couldn't find arc " + arc.toString() + " in output network.", dn2.containsEdge(arc));
		}
		for(Pair<Id> arc : dn2.getEdges()){
			assertTrue("Couldn't find arc " + arc.toString() + " in input network.", dn1.containsEdge(arc));
			assertTrue("Wrong weight for arc " + arc.toString(), dn1.getWeights().get(arc).equals(dn2.getWeights().get(arc)));
		}		
	}

	/**
	 * the following little graph is used:
	 * 
	 *  2      -------> 3
	 *  ^     /       /
	 *  |   w:2      /
	 *  |   /      w:1
	 * w:1 /       /
	 *  | /       /
	 *  |/<-------
	 *  1 <--- w:3 ---- 4
	 */
	private DigicoreNetwork buildSmallNetwork(){
		DigicoreNetwork dn = new DigicoreNetwork();

		DigicoreActivity da1 = new DigicoreActivity("test", TimeZone.getTimeZone("GMT+2"), new Locale("en"));		
		da1.setCoord(new CoordImpl(0.0, 0.0));						
		da1.setFacilityId(new IdImpl(1));	
		
		DigicoreActivity da2 = new DigicoreActivity("test", TimeZone.getTimeZone("GMT+2"), new Locale("en"));
		da2.setCoord(new CoordImpl(0.0, 1.0));
		da2.setFacilityId(new IdImpl(2));
		
		DigicoreActivity da3 = new DigicoreActivity("test", TimeZone.getTimeZone("GMT+2"), new Locale("en"));
		da3.setCoord(new CoordImpl(1.0, 1.0));
		da3.setFacilityId(new IdImpl(3));
		
		DigicoreActivity da4 = new DigicoreActivity("test", TimeZone.getTimeZone("GMT+2"), new Locale("en"));
		da4.setCoord(new CoordImpl(1.0, 0.0));
		da4.setFacilityId(new IdImpl(4));

		dn.addArc(da1, da2);
		dn.addArc(da1, da3);
		dn.addArc(da1, da3);
		dn.addArc(da3, da1);
		dn.addArc(da4, da1);
		dn.addArc(da4, da1);
		dn.addArc(da4, da1);

		return dn;
	}

}

