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
package org.matsim.contrib.socnetgen.sna.graph.spatial.io;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.contrib.socnetgen.sna.graph.spatial.SpatialGraph;
import org.matsim.core.utils.misc.CRCChecksum;
import org.matsim.testcases.MatsimTestCase;

import java.io.IOException;


/**
 * @author illenberger
 *
 */
public class SpatialGraphMLIoTest extends MatsimTestCase
{
	@Test
	public void test() {
		final String OUTPUT_FILE = getOutputDirectory() + "tmpgraph.graphml";

		SpatialGraphMLReader reader = new SpatialGraphMLReader();
		
		SpatialGraph graph = reader.readGraph(getPackageInputDirectory() + "SpatialGraph.k7.graphml.gz");
		
		Assert.assertEquals(7187, graph.getVertices().size(),0.00001);
		Assert.assertEquals(25680, graph.getEdges().size(),0.00001);
		
		SpatialGraphMLWriter writer = new SpatialGraphMLWriter();
		try {
			writer.write(graph, OUTPUT_FILE);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		long reference = CRCChecksum.getCRCFromFile(getPackageInputDirectory() + "SpatialGraph.k7.graphml.gz");
		long actual = CRCChecksum.getCRCFromFile(OUTPUT_FILE);
		
		Assert.assertEquals(reference, actual);
	}
}
