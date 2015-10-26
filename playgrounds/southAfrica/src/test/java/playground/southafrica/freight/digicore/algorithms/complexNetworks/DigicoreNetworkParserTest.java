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

package playground.southafrica.freight.digicore.algorithms.complexNetworks;


import java.io.IOException;
import java.util.Locale;
import java.util.TimeZone;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.misc.MatsimTestUtils;
import org.matsim.facilities.ActivityFacility;

import playground.southafrica.freight.digicore.algorithms.complexNetwork.DigicoreNetworkParser;
import playground.southafrica.freight.digicore.containers.DigicoreActivity;
import playground.southafrica.freight.digicore.containers.DigicoreNetwork;
import edu.uci.ics.jung.graph.util.Pair;

public class DigicoreNetworkParserTest{
	@Rule public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void testParseNetwork(){
		DigicoreNetwork dn1 = buildSmallNetwork();

		DigicoreNetworkParser dnp = new DigicoreNetworkParser();
		DigicoreNetwork dn2 = null;
		try {
			dn2 = dnp.parseNetwork(utils.getClassInputDirectory() + "network.txt.gz");
		} catch (IOException e) {
			Assert.fail("Should not have caught an IOException in reading input file.");
		}
		
		/* Test nodes. */
		for(Id<ActivityFacility> node : dn1.getVertices()){
			Assert.assertTrue("Couldn't find node " + node.toString() + " in output network.", dn2.containsVertex(node));
		}
		for(Id<ActivityFacility> node : dn2.getVertices()){
			Assert.assertTrue("Couldn't find node " + node.toString() + " in input network.", dn1.containsVertex(node));
			Assert.assertTrue("Wrong coordinate for node " + node.toString(), dn1.getCoordinates().get(node).equals(dn2.getCoordinates().get(node)));
		}
		
		/* Test edges. */
		for(Pair<Id<ActivityFacility>> arc : dn1.getEdges()){
			Assert.assertTrue("Couldn't find arc " + arc.toString() + " in output network.", dn2.containsEdge(arc));
		}
		for(Pair<Id<ActivityFacility>> arc : dn2.getEdges()){
			Assert.assertTrue("Couldn't find arc " + arc.toString() + " in input network.", dn1.containsEdge(arc));
			Assert.assertEquals("Wrong weight for arc " + arc.toString(), dn1.getMultiplexEdgeWeight(arc.getFirst(), "test", arc.getSecond(), "test"), dn2.getMultiplexEdgeWeight(arc.getFirst(), "test", arc.getSecond(), "test"));
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
		da1.setCoord(new Coord(0.0, 0.0));
		da1.setFacilityId(Id.create(1, ActivityFacility.class));	
		
		DigicoreActivity da2 = new DigicoreActivity("test", TimeZone.getTimeZone("GMT+2"), new Locale("en"));
		da2.setCoord(new Coord(0.0, 1.0));
		da2.setFacilityId(Id.create(2, ActivityFacility.class));
		
		DigicoreActivity da3 = new DigicoreActivity("test", TimeZone.getTimeZone("GMT+2"), new Locale("en"));
		da3.setCoord(new Coord(1.0, 1.0));
		da3.setFacilityId(Id.create(3, ActivityFacility.class));
		
		DigicoreActivity da4 = new DigicoreActivity("test", TimeZone.getTimeZone("GMT+2"), new Locale("en"));
		da4.setCoord(new Coord(1.0, 0.0));
		da4.setFacilityId(Id.create(4, ActivityFacility.class));

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

