/* *********************************************************************** *
 * project: org.matsim.*
 * TrianglesTest.java
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

import junit.framework.TestCase;

import org.matsim.contrib.sna.graph.SparseGraph;
import org.matsim.contrib.sna.graph.SparseGraphBuilder;
import org.matsim.contrib.sna.graph.SparseVertex;

import playground.johannes.socialnetworks.graph.analysis.Triangles;
import playground.johannes.socialnetworks.statistics.Distribution;

/**
 * @author illenberger
 *
 */
public class TrianglesTest extends TestCase {

	public void test() {
		SparseGraphBuilder builder = new SparseGraphBuilder();
		SparseGraph graph = builder.createGraph();
		
		SparseVertex v1 = builder.addVertex(graph);
		SparseVertex v2 = builder.addVertex(graph);
		SparseVertex v3 = builder.addVertex(graph);
		SparseVertex v4 = builder.addVertex(graph);
		SparseVertex v5 = builder.addVertex(graph);
		SparseVertex v6 = builder.addVertex(graph);
		
		builder.addEdge(graph, v1, v2);
		builder.addEdge(graph, v2, v3);
		builder.addEdge(graph, v3, v4);
		builder.addEdge(graph, v4, v5);
		builder.addEdge(graph, v5, v6);
		builder.addEdge(graph, v6, v1);
		
		builder.addEdge(graph, v2, v5);
		builder.addEdge(graph, v3, v5);
		builder.addEdge(graph, v1, v5);
		
		Triangles triangles = new Triangles();
		
		Distribution distr = triangles.localClusteringDistribution(graph.getVertices());
		assertEquals(0.733, distr.mean(), 0.001);
		assertEquals(0.4, distr.min());
		assertEquals(1.0, distr.max());
		
		assertEquals(0.416, triangles.globalClusteringCoefficient(graph));
		
	}
}
