/* *********************************************************************** *
 * project: org.matsim.*
 * SpatialGraphMLIoTest.java
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

import java.io.IOException;

import junit.framework.TestCase;

import org.matsim.core.utils.misc.CRCChecksum;

import playground.johannes.sna.TestCaseUtils;
import playground.johannes.sna.graph.spatial.SpatialGraph;


/**
 * @author illenberger
 *
 */
public class SpatialGraphMLIoTest extends TestCase {

	private static final String INPUT_FILE = TestCaseUtils.getPackageInputDirecoty(SpatialGraphMLIoTest.class) + "SpatialGraph.k7.graphml.gz";
	
	private static final String OUTPUT_FILE = TestCaseUtils.getOutputDirectory() + "tmpgraph.graphml";
	
	public void test() {
		SpatialGraphMLReader reader = new SpatialGraphMLReader();
		
		SpatialGraph graph = reader.readGraph(INPUT_FILE);
		
		assertEquals(7187, graph.getVertices().size());
		assertEquals(25680, graph.getEdges().size());
		
		SpatialGraphMLWriter writer = new SpatialGraphMLWriter();
		try {
			writer.write(graph, OUTPUT_FILE);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		double reference = CRCChecksum.getCRCFromFile(INPUT_FILE);
		double actual = CRCChecksum.getCRCFromFile(OUTPUT_FILE);
		
		assertEquals(reference, actual);
	}
}
