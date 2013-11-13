/* *********************************************************************** *
 * project: org.matsim.*
 * DigicoreNetworkTest.java
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

package playground.southafrica.freight.digicore.containers;


import java.util.Locale;
import java.util.TimeZone;

import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.testcases.MatsimTestCase;

import playground.southafrica.freight.digicore.containers.DigicoreActivity;
import playground.southafrica.freight.digicore.containers.DigicoreNetwork;
import edu.uci.ics.jung.graph.util.Pair;

public class DigicoreNetworkTest extends MatsimTestCase {

	
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
	public void testAddArc(){
		DigicoreNetwork dn = new DigicoreNetwork();		
		
		assertEquals("Wrong number of nodes.", 0, dn.getVertexCount());
		assertEquals("Wrong number of coordinates.", 0, dn.getCoordinates().size());
		assertEquals("Wrong number of arcs.", 0, dn.getEdgeCount());
		assertEquals("Wrong number of weights.", 0, dn.getWeights().size());
	
		DigicoreActivity da1 = new DigicoreActivity("test_1", TimeZone.getTimeZone("GMT+2"), new Locale("en"));
		CoordImpl c1 = new CoordImpl(0.0, 0.0);
		da1.setCoord(c1);	
		Id i1 = new IdImpl(1);
		da1.setFacilityId(i1);	
		DigicoreActivity da2 = new DigicoreActivity("test_2", TimeZone.getTimeZone("GMT+2"), new Locale("en"));
		CoordImpl c2 = new CoordImpl(1.0, 0.0);
		da2.setCoord(c2);
		Id i2 = new IdImpl(2);
		da2.setFacilityId(i2);
		dn.addArc(da1, da2);
		assertEquals("Wrong number of nodes.", 2, dn.getVertexCount());
		assertEquals("Wrong number of coordinates.", 2, dn.getCoordinates().size());
		assertTrue("Couldn't find node 1.", dn.containsVertex(i1));
		assertEquals("Wrong coordinate for node 1.", c1, dn.getCoordinates().get(i1));
		assertTrue("Couldn't find node 2.", dn.containsVertex(i2));
		assertEquals("Wrong coordinate for node 2.", c2, dn.getCoordinates().get(new IdImpl(2)));
		
		Pair<Id> p1 = new Pair<Id>(i1, i2);
		Pair<String> t1 = new Pair<String>(da1.getType(), da2.getType());
		Tuple<Pair<Id>, Pair<String>> tuple1 = new Tuple<Pair<Id>, Pair<String>>(p1, t1);
		assertEquals("Wrong number of arcs.", 1, dn.getEdgeCount());
		assertEquals("Wrong number of weights.", 1, dn.getWeights().size());
		assertTrue("Couldn't find edge " + p1.toString(), dn.containsEdge(p1));
		assertEquals("Wrong weight for arc 1 --> 2.", 1, dn.getWeights().get(tuple1).intValue());


		DigicoreActivity da3 = new DigicoreActivity("test_3", TimeZone.getTimeZone("GMT+2"), new Locale("en"));
		CoordImpl c3 = new CoordImpl(1.0, 1.0);
		da3.setCoord(c3);
		Id i3 = new IdImpl(3);
		da3.setFacilityId(i3);
		dn.addArc(da1, da3);
		assertEquals("Wrong number of nodes.", 3, dn.getVertexCount());
		assertEquals("Wrong number of coordinates.", 3, dn.getCoordinates().size());
		assertTrue("Couldn't find node 3.", dn.containsVertex(i3));
		assertEquals("Wrong coordinate for node 3.", c3, dn.getCoordinates().get(i3));
		Pair<Id> p2 = new Pair<Id>(i1, i3);
		Pair<String> t2 = new Pair<String>(da1.getType(), da3.getType());
		Tuple<Pair<Id>, Pair<String>> tuple2 = new Tuple<Pair<Id>, Pair<String>>(p2, t2);
		assertEquals("Wrong number of arcs.", 2, dn.getEdgeCount());
		assertEquals("Wrong number of weights.", 2, dn.getWeights().size());
		assertTrue("Couldn't find edge " + p2.toString(), dn.containsEdge(p2));
		assertEquals("Wrong weight for arc 1 --> 3.", 1, dn.getWeights().get(tuple2).intValue());
		
		dn.addArc(da1, da3);
		assertEquals("Wrong number of nodes.", 3, dn.getVertexCount());
		assertEquals("Wrong number of coordinates.", 3, dn.getCoordinates().size());
		assertEquals("Wrong number of arcs.", 2, dn.getEdgeCount());
		assertEquals("Wrong number of weights.", 2, dn.getWeights().size());
		assertTrue("Couldn't find edge " + p2.toString(), dn.containsEdge(p2));
		assertEquals("Wrong weight for arc 1 --> 3.", 2, dn.getWeights().get(tuple2).intValue());
		
		dn.addArc(da3, da1);
		Pair<Id> p3 = new Pair<Id>(i3, i1);
		Pair<String> t3 = new Pair<String>(da3.getType(), da1.getType());
		Tuple<Pair<Id>, Pair<String>> tuple3 = new Tuple<Pair<Id>, Pair<String>>(p3, t3);
		assertEquals("Wrong number of nodes.", 3, dn.getVertexCount());
		assertEquals("Wrong number of coordinates.", 3, dn.getCoordinates().size());
		assertEquals("Wrong number of arcs.", 3, dn.getEdgeCount());
		assertEquals("Wrong number of weights.", 3, dn.getWeights().size());
		assertTrue("Couldn't find edge " + p3.toString(), dn.containsEdge(p3));
		assertEquals("Wrong weight for arc 1 --> 3.", 2, dn.getWeights().get(tuple2).intValue());
		assertEquals("Wrong weight for arc 3 --> 1.", 1, dn.getWeights().get(tuple3).intValue());
		
		DigicoreActivity da4 = new DigicoreActivity("test_4", TimeZone.getTimeZone("GMT+2"), new Locale("en"));
		CoordImpl c4 = new CoordImpl(1.0, 0.0);
		da4.setCoord(c4);
		IdImpl i4 = new IdImpl(4);
		da4.setFacilityId(i4);
		dn.addArc(da4, da1);
		dn.addArc(da4, da1);
		dn.addArc(da4, da1);
		Pair<Id> p4 = new Pair<Id>(i4, i1);
		Pair<String> t4 = new Pair<String>(da4.getType(), da1.getType());
		Tuple<Pair<Id>, Pair<String>> tuple4 = new Tuple<Pair<Id>, Pair<String>>(p4, t4);
		assertEquals("Wrong number of nodes.", 4, dn.getVertexCount());
		assertEquals("Wrong number of coordinates.", 4, dn.getCoordinates().size());
		assertTrue("Couldn't find node 4.", dn.containsVertex(i4));
		assertEquals("Wrong coordinate for node 4.", c4, dn.getCoordinates().get(i4));
		
		assertEquals("Wrong number of arcs.", 4, dn.getEdgeCount());
		assertEquals("Wrong number of weights.", 4, dn.getWeights().size());
		assertTrue("Couldn't find edge " + p4.toString(), dn.containsEdge(p4));
		assertEquals("Wrong weight for arc 4 --> 1.", 3, dn.getWeights().get(tuple4).intValue());
		
	}
	
	
	public void testNumberOfActivtyTypes(){
		DigicoreNetwork dn = new DigicoreNetwork();
		
		/* Create all location 1 activities. */
		DigicoreActivity da1 = new DigicoreActivity("test1", TimeZone.getTimeZone("GMT+2"), new Locale("en"));
		da1.setCoord(new CoordImpl(0, 0));
		da1.setFacilityId(new IdImpl("1"));
		DigicoreActivity da2 = new DigicoreActivity("test1", TimeZone.getTimeZone("GMT+2"), new Locale("en"));
		da2.setCoord(new CoordImpl(0, 0));
		da2.setFacilityId(new IdImpl("1"));
		
		/* Create location 2 activities. */
		DigicoreActivity da3 = new DigicoreActivity("test2", TimeZone.getTimeZone("GMT+2"), new Locale("en"));
		da3.setCoord(new CoordImpl(1, 0));
		da3.setFacilityId(new IdImpl("2"));
		DigicoreActivity da4 = new DigicoreActivity("test2", TimeZone.getTimeZone("GMT+2"), new Locale("en"));
		da4.setCoord(new CoordImpl(1, 0));
		da4.setFacilityId(new IdImpl("2"));
		
		dn.addArc(da1, da3);
		dn.addArc(da2, da4);
		
		/* Check that the right number of activity types are captured/recorded. */
		assertEquals("Wrong number of activity types.", 2, dn.getActivityTypes().size());
		assertTrue("Network does not contain `test1' activity type", dn.getActivityTypes().contains("test1"));
		assertTrue("Network does not contain `test2' activity type", dn.getActivityTypes().contains("test2"));		
	}
	
	
	public void testGetDensity(){
		DigicoreNetwork dn = buildSmallNetwork();
		assertEquals("Wrong density calculated.", 4.0/12.0, dn.getDensity());
	}
	
	
	public void testGetMinMaxArcWeights(){
		DigicoreNetwork dn = buildSmallNetwork();
		
		int[] minMax = dn.getMinMaxEdgeWeights();
		assertEquals("Wrong minimum edge weight.", 1, minMax[0]);
		assertEquals("Wrong maximum edge weight.", 3, minMax[1]);
	}
	
	
	public void testGetMultiplexEdgeWeight(){
		DigicoreNetwork dn = buildMultiplexNetwork();
		Id id1 = new IdImpl("1");
		Id id2 = new IdImpl("2");
		
		assertEquals("Wrong weight for 1a-2 type edges.", 1, dn.getMultiplexEdgeWeight(id1, "1a", id2, "2"));
		assertEquals("Wrong weight for 1b-2 type edges.", 2, dn.getMultiplexEdgeWeight(id1, "1b", id2, "2"));
	}
	
	
	/**
	 * We build multiplex networks. The weights should take that into account.
	 */
	public void testGetEdgeWeight(){
		DigicoreNetwork dn = buildMultiplexNetwork();
		
		assertEquals("Wrong total weight.", 3, dn.getEdgeWeight(new IdImpl(1), new IdImpl(2)));
	}
	
	
	/**
	 * the following multiplex graph is used.
	 *   
	 * 1a --- type1 (w:1) ----.
	 * 1b \                   _\|
	 *     --- type2 (w:2) ---> 2  
	 * @return
	 */
	private DigicoreNetwork buildMultiplexNetwork(){
		DigicoreNetwork dn = new DigicoreNetwork();

		DigicoreActivity da1a = new DigicoreActivity("1a", TimeZone.getTimeZone("GMT+2"), new Locale("en"));		
		da1a.setCoord(new CoordImpl(0.0, 0.0));						
		da1a.setFacilityId(new IdImpl(1));	
		
		DigicoreActivity da1b = new DigicoreActivity("1b", TimeZone.getTimeZone("GMT+2"), new Locale("en"));
		da1b.setCoord(new CoordImpl(0.0, 0.0));
		da1b.setFacilityId(new IdImpl(1));

		DigicoreActivity da2 = new DigicoreActivity("2", TimeZone.getTimeZone("GMT+2"), new Locale("en"));
		da2.setCoord(new CoordImpl(2.0, 0.0));
		da2.setFacilityId(new IdImpl(2));
		
		dn.addArc(da1a, da2);
		dn.addArc(da1b, da2);
		dn.addArc(da1b, da2);
		
		return dn;
	}

	
	/**
	 * The following little graph is used:
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

