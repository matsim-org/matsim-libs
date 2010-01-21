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
package org.matsim.contrib.sna.graph.spatial.io;

import junit.framework.TestCase;

import org.matsim.contrib.sna.TestCaseUtils;
import org.matsim.contrib.sna.graph.spatial.SpatialGraph;

/**
 * @author jillenberger
 *
 */
public class SpatialGraphKMLWriterTest extends TestCase {

	/*
	 * This is no real test, it just runs the writer.
	 */
	public void test() {
		SpatialGraphMLReader reader = new SpatialGraphMLReader();
		SpatialGraph graph = reader.readGraph(TestCaseUtils.getPackageInputDirecoty(this.getClass()) + "SpatialGraph.k7.graphml.gz");
		
		SpatialGraphKMLWriter writer = new SpatialGraphKMLWriter();
		writer.setDrawEdges(false);
		writer.setKmlVertexDetail(new KMLVertexDescriptor(graph));
		String output = TestCaseUtils.getOutputDirectory() + "tempgraph.kmz";
		writer.write(graph, output);		
	}
}
