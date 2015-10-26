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

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.facilities.ActivityFacility;
import org.matsim.testcases.MatsimTestUtils;

import edu.uci.ics.jung.graph.util.Pair;

public class DigicoreNetworkTest{
	@Rule public MatsimTestUtils utils = new MatsimTestUtils();

	
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
	@Test
	public void testAddArc(){
		DigicoreNetwork dn = new DigicoreNetwork();		
		
		Assert.assertEquals("Wrong number of nodes.", 0, dn.getVertexCount());
		Assert.assertEquals("Wrong number of coordinates.", 0, dn.getCoordinates().size());
		Assert.assertEquals("Wrong number of arcs.", 0, dn.getEdgeCount());
		Assert.assertEquals("Wrong number of weights.", 0, dn.getWeights().size());
	
		DigicoreActivity da1 = new DigicoreActivity("test_1", TimeZone.getTimeZone("GMT+2"), new Locale("en"));
		Coord c1 = new Coord(0.0, 0.0);
		da1.setCoord(c1);	
		Id<ActivityFacility> i1 = Id.create(1, ActivityFacility.class);
		da1.setFacilityId(i1);	
		DigicoreActivity da2 = new DigicoreActivity("test_2", TimeZone.getTimeZone("GMT+2"), new Locale("en"));
		Coord c2 = new Coord(1.0, 0.0);
		da2.setCoord(c2);
		Id<ActivityFacility> i2 = Id.create(2, ActivityFacility.class);
		da2.setFacilityId(i2);
		dn.addArc(da1, da2);
		Assert.assertEquals("Wrong number of nodes.", 2, dn.getVertexCount());
		Assert.assertEquals("Wrong number of coordinates.", 2, dn.getCoordinates().size());
		Assert.assertTrue("Couldn't find node 1.", dn.containsVertex(i1));
		Assert.assertEquals("Wrong coordinate for node 1.", c1, dn.getCoordinates().get(i1));
		Assert.assertTrue("Couldn't find node 2.", dn.containsVertex(i2));
		Assert.assertEquals("Wrong coordinate for node 2.", c2, dn.getCoordinates().get(Id.create(2, ActivityFacility.class)));
		
		Pair<Id<ActivityFacility>> p1 = new Pair<Id<ActivityFacility>>(i1, i2);
		Pair<String> t1 = new Pair<String>(da1.getType(), da2.getType());
		Tuple<Pair<Id<ActivityFacility>>, Pair<String>> tuple1 = new Tuple<Pair<Id<ActivityFacility>>, Pair<String>>(p1, t1);
		Assert.assertEquals("Wrong number of arcs.", 1, dn.getEdgeCount());
		Assert.assertEquals("Wrong number of weights.", 1, dn.getWeights().size());
		Assert.assertTrue("Couldn't find edge " + p1.toString(), dn.containsEdge(p1));
		Assert.assertEquals("Wrong weight for arc 1 --> 2.", 1, dn.getWeights().get(tuple1).intValue());


		DigicoreActivity da3 = new DigicoreActivity("test_3", TimeZone.getTimeZone("GMT+2"), new Locale("en"));
		Coord c3 = new Coord(1.0, 1.0);
		da3.setCoord(c3);
		Id<ActivityFacility> i3 = Id.create(3,  ActivityFacility.class);
		da3.setFacilityId(i3);
		dn.addArc(da1, da3);
		Assert.assertEquals("Wrong number of nodes.", 3, dn.getVertexCount());
		Assert.assertEquals("Wrong number of coordinates.", 3, dn.getCoordinates().size());
		Assert.assertTrue("Couldn't find node 3.", dn.containsVertex(i3));
		Assert.assertEquals("Wrong coordinate for node 3.", c3, dn.getCoordinates().get(i3));
		Pair<Id<ActivityFacility>> p2 = new Pair<Id<ActivityFacility>>(i1, i3);
		Pair<String> t2 = new Pair<String>(da1.getType(), da3.getType());
		Tuple<Pair<Id<ActivityFacility>>, Pair<String>> tuple2 = new Tuple<Pair<Id<ActivityFacility>>, Pair<String>>(p2, t2);
		Assert.assertEquals("Wrong number of arcs.", 2, dn.getEdgeCount());
		Assert.assertEquals("Wrong number of weights.", 2, dn.getWeights().size());
		Assert.assertTrue("Couldn't find edge " + p2.toString(), dn.containsEdge(p2));
		Assert.assertEquals("Wrong weight for arc 1 --> 3.", 1, dn.getWeights().get(tuple2).intValue());
		
		dn.addArc(da1, da3);
		Assert.assertEquals("Wrong number of nodes.", 3, dn.getVertexCount());
		Assert.assertEquals("Wrong number of coordinates.", 3, dn.getCoordinates().size());
		Assert.assertEquals("Wrong number of arcs.", 2, dn.getEdgeCount());
		Assert.assertEquals("Wrong number of weights.", 2, dn.getWeights().size());
		Assert.assertTrue("Couldn't find edge " + p2.toString(), dn.containsEdge(p2));
		Assert.assertEquals("Wrong weight for arc 1 --> 3.", 2, dn.getWeights().get(tuple2).intValue());
		
		dn.addArc(da3, da1);
		Pair<Id<ActivityFacility>> p3 = new Pair<Id<ActivityFacility>>(i3, i1);
		Pair<String> t3 = new Pair<String>(da3.getType(), da1.getType());
		Tuple<Pair<Id<ActivityFacility>>, Pair<String>> tuple3 = new Tuple<Pair<Id<ActivityFacility>>, Pair<String>>(p3, t3);
		Assert.assertEquals("Wrong number of nodes.", 3, dn.getVertexCount());
		Assert.assertEquals("Wrong number of coordinates.", 3, dn.getCoordinates().size());
		Assert.assertEquals("Wrong number of arcs.", 3, dn.getEdgeCount());
		Assert.assertEquals("Wrong number of weights.", 3, dn.getWeights().size());
		Assert.assertTrue("Couldn't find edge " + p3.toString(), dn.containsEdge(p3));
		Assert.assertEquals("Wrong weight for arc 1 --> 3.", 2, dn.getWeights().get(tuple2).intValue());
		Assert.assertEquals("Wrong weight for arc 3 --> 1.", 1, dn.getWeights().get(tuple3).intValue());
		
		DigicoreActivity da4 = new DigicoreActivity("test_4", TimeZone.getTimeZone("GMT+2"), new Locale("en"));
		Coord c4 = new Coord(1.0, 0.0);
		da4.setCoord(c4);
		Id<ActivityFacility> i4 = Id.create(4,  ActivityFacility.class);
		da4.setFacilityId(i4);
		dn.addArc(da4, da1);
		dn.addArc(da4, da1);
		dn.addArc(da4, da1);
		Pair<Id<ActivityFacility>> p4 = new Pair<Id<ActivityFacility>>(i4, i1);
		Pair<String> t4 = new Pair<String>(da4.getType(), da1.getType());
		Tuple<Pair<Id<ActivityFacility>>, Pair<String>> tuple4 = new Tuple<Pair<Id<ActivityFacility>>, Pair<String>>(p4, t4);
		Assert.assertEquals("Wrong number of nodes.", 4, dn.getVertexCount());
		Assert.assertEquals("Wrong number of coordinates.", 4, dn.getCoordinates().size());
		Assert.assertTrue("Couldn't find node 4.", dn.containsVertex(i4));
		Assert.assertEquals("Wrong coordinate for node 4.", c4, dn.getCoordinates().get(i4));
		
		Assert.assertEquals("Wrong number of arcs.", 4, dn.getEdgeCount());
		Assert.assertEquals("Wrong number of weights.", 4, dn.getWeights().size());
		Assert.assertTrue("Couldn't find edge " + p4.toString(), dn.containsEdge(p4));
		Assert.assertEquals("Wrong weight for arc 4 --> 1.", 3, dn.getWeights().get(tuple4).intValue());
	}
	
	
	@Test
	public void testNumberOfActivtyTypes(){
		DigicoreNetwork dn = new DigicoreNetwork();
		
		/* Create all location 1 activities. */
		DigicoreActivity da1 = new DigicoreActivity("test1", TimeZone.getTimeZone("GMT+2"), new Locale("en"));
		da1.setCoord(new Coord((double) 0, (double) 0));
		da1.setFacilityId(Id.create(1,  ActivityFacility.class));
		DigicoreActivity da2 = new DigicoreActivity("test1", TimeZone.getTimeZone("GMT+2"), new Locale("en"));
		da2.setCoord(new Coord((double) 0, (double) 0));
		da2.setFacilityId(Id.create(3,  ActivityFacility.class));
		
		/* Create location 2 activities. */
		DigicoreActivity da3 = new DigicoreActivity("test2", TimeZone.getTimeZone("GMT+2"), new Locale("en"));
		da3.setCoord(new Coord((double) 1, (double) 0));
		da3.setFacilityId(Id.create(2,  ActivityFacility.class));
		DigicoreActivity da4 = new DigicoreActivity("test2", TimeZone.getTimeZone("GMT+2"), new Locale("en"));
		da4.setCoord(new Coord((double) 1, (double) 0));
		da4.setFacilityId(Id.create(2,  ActivityFacility.class));
		
		dn.addArc(da1, da3);
		dn.addArc(da2, da4);
		
		/* Check that the right number of activity types are captured/recorded. */
		Assert.assertEquals("Wrong number of activity types.", 2, dn.getActivityTypes().size());
		Assert.assertTrue("Network does not contain `test1' activity type", dn.getActivityTypes().contains("test1"));
		Assert.assertTrue("Network does not contain `test2' activity type", dn.getActivityTypes().contains("test2"));		
	}
	
	
	@Test
	public void testGetDensity(){
		DigicoreNetwork dn = buildSmallNetwork();
		Assert.assertEquals("Wrong density calculated.", 4.0/12.0, dn.getDensity(), MatsimTestUtils.EPSILON);
	}
	
	
	@Test
	public void testGetMinMaxArcWeights(){
		DigicoreNetwork dn = buildSmallNetwork();
		
		int[] minMax = dn.getMinMaxEdgeWeights();
		Assert.assertEquals("Wrong minimum edge weight.", 1, minMax[0]);
		Assert.assertEquals("Wrong maximum edge weight.", 3, minMax[1]);
	}
	
	
	@Test
	public void testGetMultiplexEdgeWeight(){
		DigicoreNetwork dn = buildMultiplexNetwork();
		Id<ActivityFacility> id1 = Id.create(1,  ActivityFacility.class);
		Id<ActivityFacility> id2 = Id.create(2,  ActivityFacility.class);
		
		Assert.assertEquals("Wrong weight for 1a-2 type edges.", 1, dn.getMultiplexEdgeWeight(id1, "1a", id2, "2"));
		Assert.assertEquals("Wrong weight for 1b-2 type edges.", 2, dn.getMultiplexEdgeWeight(id1, "1b", id2, "2"));
	}
	
	
	/**
	 * We build multiplex networks. The weights should take that into account.
	 */
	@Test
	public void testGetEdgeWeight(){
		DigicoreNetwork dn = buildMultiplexNetwork();
		
		Assert.assertEquals("Wrong total weight.", 3, dn.getEdgeWeight(
				Id.create(1,  ActivityFacility.class), 
				Id.create(2,  ActivityFacility.class))
		);
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
		da1a.setCoord(new Coord(0.0, 0.0));
		da1a.setFacilityId(Id.create(1,  ActivityFacility.class));	
		
		DigicoreActivity da1b = new DigicoreActivity("1b", TimeZone.getTimeZone("GMT+2"), new Locale("en"));
		da1b.setCoord(new Coord(0.0, 0.0));
		da1b.setFacilityId(Id.create(1,  ActivityFacility.class));

		DigicoreActivity da2 = new DigicoreActivity("2", TimeZone.getTimeZone("GMT+2"), new Locale("en"));
		da2.setCoord(new Coord(2.0, 0.0));
		da2.setFacilityId(Id.create(2,  ActivityFacility.class));
		
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
		da1.setCoord(new Coord(0.0, 0.0));
		da1.setFacilityId(Id.create(1,  ActivityFacility.class));	
		
		DigicoreActivity da2 = new DigicoreActivity("test", TimeZone.getTimeZone("GMT+2"), new Locale("en"));
		da2.setCoord(new Coord(0.0, 1.0));
		da2.setFacilityId(Id.create(2,  ActivityFacility.class));
		
		DigicoreActivity da3 = new DigicoreActivity("test", TimeZone.getTimeZone("GMT+2"), new Locale("en"));
		da3.setCoord(new Coord(1.0, 1.0));
		da3.setFacilityId(Id.create(3,  ActivityFacility.class));
		
		DigicoreActivity da4 = new DigicoreActivity("test", TimeZone.getTimeZone("GMT+2"), new Locale("en"));
		da4.setCoord(new Coord(1.0, 0.0));
		da4.setFacilityId(Id.create(4,  ActivityFacility.class));

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

