/* *********************************************************************** *
 * project: org.matsim.*
 * SparseGraphMLReaderTest.java
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
package org.matsim.contrib.socnetgen.sna.graph.io;


import org.matsim.contrib.socnetgen.sna.graph.SparseGraph;
import org.matsim.testcases.MatsimTestCase;

/**
 * @author illenberger
 *
 */
public class SparseGraphMLReaderTest extends MatsimTestCase {

	public void test() {
		SparseGraphMLReader reader = new SparseGraphMLReader();
		SparseGraph graph = reader.readGraph(getPackageInputDirectory() + "test.graphml.gz");
		
		assertEquals("The graph does not contain 1000 vertices.", 1000, graph.getVertices().size());
		assertEquals("the graph does not contain 49334 edges.", 49334, graph.getEdges().size());
	}
}
