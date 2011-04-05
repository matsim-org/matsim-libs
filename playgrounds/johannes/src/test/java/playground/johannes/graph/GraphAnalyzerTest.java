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

import java.util.Map;

import junit.framework.TestCase;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.matsim.contrib.sna.graph.Graph;
import org.matsim.contrib.sna.graph.analysis.DegreeTask;
import org.matsim.contrib.sna.graph.analysis.GraphAnalyzer;
import org.matsim.contrib.sna.graph.analysis.TransitivityTask;
import org.matsim.contrib.sna.graph.io.SparseGraphMLReader;

import playground.johannes.socialnetworks.graph.analysis.TopologyAnalyzerTask;

/**
 * @author illenberger
 *
 */
public class GraphAnalyzerTest extends TestCase {

	private static final String INPUT_FILE = "../../contrib/sna/test/input/org/matsim/contrib/sna/graph/spatial/io/SpatialGraph.k7.graphml.gz";
	
	public void test() {
		SparseGraphMLReader reader = new SparseGraphMLReader();
		Graph graph = reader.readGraph(INPUT_FILE);
		
		Map<String, DescriptiveStatistics> stats = GraphAnalyzer.analyze(graph, new TopologyAnalyzerTask());
		
		assertEquals(7.1462, stats.get(DegreeTask.KEY).getMean(), 0.0001);
		assertEquals(19.0, stats.get(DegreeTask.KEY).getMax());
		assertEquals(0.0, stats.get(DegreeTask.KEY).getMin());
		
		assertEquals(0.0018, stats.get("r_k").getMean(), 0.0001);
		
		assertEquals(0.0008, stats.get(TransitivityTask.KEY).getMean(), 0.0001);
		assertEquals(1.0, stats.get(TransitivityTask.KEY).getMax());
		assertEquals(0.0, stats.get(TransitivityTask.KEY).getMin());
		assertEquals(0.0008, stats.get("c_global").getMean(), 0.0001);
	}
}
