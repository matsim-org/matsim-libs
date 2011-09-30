/* *********************************************************************** *
 * project: org.matsim.*
 * Plansgenerator.java
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
package playground.droeder.data.graph.comparison;

import static org.junit.Assert.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.geometry.CoordImpl;

import playground.droeder.data.graph.MatchingEdge;
import playground.droeder.data.graph.MatchingNode;


/**
 * @author droeder
 *
 */
public class EdgeCompareTest {
	private Double deltaDist;
	private Double deltaPhi;
	private Double maxRelLengthDiff;
	
	@Before
	public void init(){
		this.deltaDist = 20.0;
		this.deltaPhi = (Math.PI / 4);
		this.maxRelLengthDiff = 0.1;
	}

	@Test
	public void testHorizontal(){
		MatchingNode refN1, refN2, candN1, candN2;
		
		refN1 = new MatchingNode(new IdImpl("refN1"), new CoordImpl(0,0));
		refN2 = new MatchingNode(new IdImpl("refN2"), new CoordImpl(0,100));
		MatchingEdge ref = new MatchingEdge(new IdImpl("ref"), refN1, refN2);
		MatchingEdge refRev = new MatchingEdge(new IdImpl("refRev"), refN2, refN1);
		
		candN1 = new MatchingNode(new IdImpl("candN1"), new CoordImpl(-1,10));
		candN2 = new MatchingNode(new IdImpl("candN2"), new CoordImpl(99, 10));
		MatchingEdge cand = new MatchingEdge(new IdImpl("cand"), candN1, candN2);
		MatchingEdge candRev = new MatchingEdge(new IdImpl("candRev"), candN2, candN1);
		
		EdgeCompare ec;
		ec = new EdgeCompare(ref, cand);
		assertEquals("wrong av Distance", 10, ec.getAvDist(), 0.0);
		
	}
	
	@Test
	public void testVertical(){
		
	}
	
	@Test
	public void testNoMatch(){
		
	}
	
	@Test
	public void testSomeEdge(){
		
	}
	
	@After
	public void endTest(){
		
	}

}
