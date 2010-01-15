/* *********************************************************************** *
 * project: org.matsim.*
 * SparseGraphTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

import org.matsim.contrib.sna.graph.Edge;
import org.matsim.contrib.sna.graph.SparseEdge;
import org.matsim.contrib.sna.graph.SparseGraph;
import org.matsim.contrib.sna.graph.SparseGraphBuilder;
import org.matsim.contrib.sna.graph.SparseVertex;

import junit.framework.TestCase;

/**
 * @author illenberger
 *
 */
public class SparseGraphTest extends TestCase {

	public void test() {
		SparseGraphBuilder builder = new SparseGraphBuilder();
		
		SparseGraph graph = new SparseGraph();
		
		SparseVertex vertex1 = builder.addVertex(graph);
		SparseVertex vertex2 = builder.addVertex(graph);
		SparseVertex vertex3 = builder.addVertex(graph);
		SparseVertex vertex4 = builder.addVertex(graph);
		
		SparseEdge edge1 = builder.addEdge(graph, vertex1, vertex2);
//		SparseEdge edge2 = builder.addEdge(graph, vertex2, vertex3);
//		SparseEdge edge3 = builder.addEdge(graph, vertex3, vertex4);
//		SparseEdge edge4 = builder.addEdge(graph, vertex4, vertex1);
		
		assertEquals(4, graph.getVertices().size());
		assertEquals(4, graph.getEdges().size());
		
		assertEquals(vertex2, vertex1.getNeighbours().get(0));
		assertEquals(vertex4, vertex1.getNeighbours().get(1));
		assertEquals(vertex3, vertex2.getNeighbours().get(1));
		assertEquals(vertex4, vertex3.getNeighbours().get(1));
		assertEquals(vertex1, vertex4.getNeighbours().get(1));
		
		boolean flag = true;
		try {
			flag = builder.removeVertex(graph, vertex1);
		} catch (RuntimeException e) {
			flag = false;
		}
		assertEquals(false, flag);
		
		flag = builder.removeEdge(graph, edge1);
		assertEquals(true, flag);
		
		Edge edge5 = builder.addEdge(graph, vertex2, vertex3);
		assertEquals(null, edge5);
		
		
	}
}
