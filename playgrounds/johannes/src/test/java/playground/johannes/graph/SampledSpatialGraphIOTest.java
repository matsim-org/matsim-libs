/* *********************************************************************** *
 * project: org.matsim.*
 * SampledSpatialGraphIOTest.java
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
package playground.johannes.graph;

import java.io.IOException;

import org.matsim.contrib.sna.TestCaseUtils;
import org.matsim.core.utils.misc.CRCChecksum;

import playground.johannes.socialnetworks.snowball2.spatial.SampledSpatialGraph;
import playground.johannes.socialnetworks.snowball2.spatial.io.SampledSpatialGraphMLReader;
import playground.johannes.socialnetworks.snowball2.spatial.io.SampledSpatialGraphMLWriter;

import junit.framework.TestCase;

/**
 * @author jillenberger
 *
 */
public class SampledSpatialGraphIOTest extends TestCase {

	private static final String INPUT_FILE = TestCaseUtils.getPackageInputDirecoty(SampledSpatialGraphIOTest.class) + "sampledgraph.graphml.gz";
	
	private static final String OUTPUT_FILE = TestCaseUtils.getOutputDirectory() + "tmpgraph.graphml";

	public void test() throws IOException {
		SampledSpatialGraphMLReader reader = new SampledSpatialGraphMLReader();
		SampledSpatialGraph graph = reader.readGraph(INPUT_FILE);
		
		SampledSpatialGraphMLWriter writer = new SampledSpatialGraphMLWriter();
		writer.write(graph, OUTPUT_FILE);
		
		double reference = CRCChecksum.getCRCFromFile(INPUT_FILE);
		double actual = CRCChecksum.getCRCFromFile(OUTPUT_FILE);
		
		assertEquals(reference, actual);
	}
}
