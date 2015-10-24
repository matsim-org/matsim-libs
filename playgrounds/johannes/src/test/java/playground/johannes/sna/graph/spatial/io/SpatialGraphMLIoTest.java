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

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.contrib.socnetgen.sna.graph.spatial.SpatialGraph;
import org.matsim.contrib.socnetgen.sna.graph.spatial.io.SpatialGraphMLReader;
import org.matsim.contrib.socnetgen.sna.graph.spatial.io.SpatialGraphMLWriter;
import org.matsim.core.utils.misc.CRCChecksum;
import org.matsim.testcases.MatsimTestUtils;
import playground.johannes.sna.TestCaseUtils;

import java.io.IOException;


/**
 * @author illenberger
 *
 */
public class SpatialGraphMLIoTest  {
	@Rule public MatsimTestUtils utils = new MatsimTestUtils();

	private static final String INPUT_FILE = TestCaseUtils.getPackageInputDirecoty(SpatialGraphMLIoTest.class) + "SpatialGraph.k7.graphml.gz";
	
//	private static final String OUTPUT_FILE = TestCaseUtils.getOutputDirectory() + "tmpgraph.graphml";
	
	@Test
	public void test() {
		 final String OUTPUT_FILE = utils.getOutputDirectory() + "tmpgraph.graphml";

		SpatialGraphMLReader reader = new SpatialGraphMLReader();
		
		SpatialGraph graph = reader.readGraph(INPUT_FILE);
		
		Assert.assertEquals(7187, graph.getVertices().size(),0.00001);
		Assert.assertEquals(25680, graph.getEdges().size(),0.00001);
		
		SpatialGraphMLWriter writer = new SpatialGraphMLWriter();
		try {
			writer.write(graph, OUTPUT_FILE);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		long reference = CRCChecksum.getCRCFromFile(INPUT_FILE);
		long actual = CRCChecksum.getCRCFromFile(OUTPUT_FILE);
		
		Assert.assertEquals(reference, actual);
	}
}
