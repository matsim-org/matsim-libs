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

import org.matsim.core.utils.misc.CRCChecksum;
import org.matsim.testcases.MatsimTestCase;

import playground.johannes.socialnetworks.snowball2.spatial.SampledSpatialGraph;
import playground.johannes.socialnetworks.snowball2.spatial.io.SampledSpatialGraphMLReader;
import playground.johannes.socialnetworks.snowball2.spatial.io.SampledSpatialGraphMLWriter;

/**
 * @author jillenberger
 *
 */
public class SampledSpatialGraphIOTest extends MatsimTestCase {

	public void test() throws IOException {

		final String INPUT_FILE = super.getPackageInputDirectory() + "sampledgraph.graphml.gz";

		final String OUTPUT_FILE = super.getOutputDirectory() + "tmpgraph.graphml";

		SampledSpatialGraphMLReader reader = new SampledSpatialGraphMLReader();
		SampledSpatialGraph graph = reader.readGraph(INPUT_FILE);

		SampledSpatialGraphMLWriter writer = new SampledSpatialGraphMLWriter();
		writer.write(graph, OUTPUT_FILE);

		double reference = CRCChecksum.getCRCFromFile(INPUT_FILE);
		double actual = CRCChecksum.getCRCFromFile(OUTPUT_FILE);

		assertEquals(reference, actual);
	}
}
