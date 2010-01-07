/* *********************************************************************** *
 * project: org.matsim.*
 * Population2SpatialGraphTest.java
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
package playground.johannes.graph;

import org.matsim.testcases.MatsimTestCase;

import playground.johannes.socialnetworks.graph.spatial.SpatialSparseGraph;
import playground.johannes.socialnetworks.graph.spatial.io.Population2SpatialGraph;
import playground.johannes.socialnetworks.spatial.CRSUtils;

/**
 * @author illenberger
 *
 */
public class Population2SpatialGraphTest extends MatsimTestCase {

	private static final String PLANS_FILE = "plans.01.xml.gz";
	
	private static final int numVertex = 2827;
	
	public void testRead() {
		Population2SpatialGraph reader = new Population2SpatialGraph(CRSUtils.getCRS(21781));
		SpatialSparseGraph graph = reader.read(getPackageInputDirectory() + PLANS_FILE);
		
		assertEquals(numVertex, graph.getVertices().size());
	}
}
