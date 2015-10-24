/* *********************************************************************** *
 * project: org.matsim.*
 * GraphAnalyzerTest.java
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

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.contrib.socnetgen.sna.graph.Graph;
import org.matsim.contrib.socnetgen.sna.graph.analysis.DegreeTask;
import org.matsim.contrib.socnetgen.sna.graph.analysis.GraphAnalyzer;
import org.matsim.contrib.socnetgen.sna.graph.analysis.TopologyAnalyzerTask;
import org.matsim.contrib.socnetgen.sna.graph.analysis.TransitivityTask;
import org.matsim.contrib.socnetgen.sna.graph.io.SparseGraphMLReader;
import org.matsim.testcases.MatsimTestUtils;

import java.util.Map;

/**
 * @author illenberger
 *
 */
public class GraphAnalyzerTest /* extends TestCase */ {
	@Rule
	MatsimTestUtils utils = new MatsimTestUtils() ;
	
//	private static final String INPUT_FILE = "../../contrib/sna/test/input/org/matsim/contrib/sna/graph/spatial/io/SpatialGraph.k7.graphml.gz";
	
	@Test
	public void test() {
		final String INPUT_FILE = utils.getPackageInputDirectory() + "/SpatialGraph.k7.graphml.gz" ;
		
		SparseGraphMLReader reader = new SparseGraphMLReader();
		Graph graph = reader.readGraph(INPUT_FILE);
		
		Map<String, DescriptiveStatistics> stats = GraphAnalyzer.analyze(graph, new TopologyAnalyzerTask());
		
		Assert.assertEquals(7.1462, stats.get(DegreeTask.KEY).getMean(), 0.0001);
		Assert.assertEquals(19.0, stats.get(DegreeTask.KEY).getMax());
		Assert.assertEquals(0.0, stats.get(DegreeTask.KEY).getMin());
		
		Assert.assertEquals(0.0018, stats.get("r_k").getMean(), 0.0001);
		
		Assert.assertEquals(0.0008, stats.get(TransitivityTask.KEY).getMean(), 0.0001);
		Assert.assertEquals(1.0, stats.get(TransitivityTask.KEY).getMax());
		Assert.assertEquals(0.0, stats.get(TransitivityTask.KEY).getMin());
		Assert.assertEquals(0.0008, stats.get("c_global").getMean(), 0.0001);
	}
}
