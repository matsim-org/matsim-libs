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

import org.matsim.contrib.sna.graph.Graph;
import org.matsim.contrib.sna.graph.io.SparseGraphMLReader;

import playground.johannes.socialnetworks.graph.analysis.GraphAnalyzer;
import playground.johannes.socialnetworks.graph.analysis.SimpleGraphPropertyFactory;

/**
 * @author illenberger
 *
 */
public class GraphAnalyzerTest extends TestCase {

	private static final String INPUT_FILE = "../contrib/sna/test/input/org/matsim/contrib/sna/graph/spatial/io/SpatialGraph.k7.graphml.gz";
	
	public void test() {
		SparseGraphMLReader reader = new SparseGraphMLReader();
		Graph graph = reader.readGraph(INPUT_FILE);
		
		GraphAnalyzer analyzer = new GraphAnalyzer();
		Map<String, Double> stats = analyzer.analyze(graph, new SimpleGraphPropertyFactory());
		
		assertEquals(7.1462, stats.get(GraphAnalyzer.MEAN_DEGREE), 0.0001);
		assertEquals(19.0, stats.get(GraphAnalyzer.MAX_DEGREE));
		assertEquals(0.0, stats.get(GraphAnalyzer.MIN_DEGREE));
		
		assertEquals(0.0018, stats.get(GraphAnalyzer.DEGREE_CORRELATION), 0.0001);
		
		assertEquals(0.0008, stats.get(GraphAnalyzer.MEAN_LOCAL_CLUSTERING), 0.0001);
		assertEquals(1.0, stats.get(GraphAnalyzer.MAX_LOCAL_CLUSTERING));
		assertEquals(0.0, stats.get(GraphAnalyzer.MIN_LOCAL_CLUSTERING));
		assertEquals(0.0008, stats.get(GraphAnalyzer.GLOBAL_CLUSTERING_COEFFICIENT), 0.0001);
	}
}
