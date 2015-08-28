/* *********************************************************************** *
 * project: org.matsim.*
 * SpatialGraphKMLWriterTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.johannes.sna.graph.spatial.io;

import junit.framework.TestCase;


import org.junit.Rule;
import org.junit.Test;
import org.matsim.testcases.MatsimTestUtils;
import playground.johannes.sna.TestCaseUtils;
import playground.johannes.sna.graph.spatial.SpatialGraph;
import playground.johannes.sna.graph.spatial.io.KMLVertexDescriptor;
import playground.johannes.sna.graph.spatial.io.SpatialGraphKMLWriter;
import playground.johannes.sna.graph.spatial.io.SpatialGraphMLReader;

/**
 * @author jillenberger
 *
 */
public class SpatialGraphKMLWriterTest {

	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();


	/*
	 * This is no real test, it just runs the writer.
	 */
	@Test
	public void test() {
		SpatialGraphMLReader reader = new SpatialGraphMLReader();
		SpatialGraph graph = reader.readGraph(TestCaseUtils.getPackageInputDirecoty(this.getClass()) + "SpatialGraph.k7.graphml.gz");
		
		SpatialGraphKMLWriter writer = new SpatialGraphKMLWriter();
		writer.setDrawEdges(false);
		writer.setKmlVertexDetail(new KMLVertexDescriptor(graph));
		String output = utils.getOutputDirectory() + "tempgraph.kmz";
		writer.write(graph, output);		
	}
}
