/* *********************************************************************** *
 * project: org.matsim.*
 * DijkstraTest.java
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
package playground.johannes.sna.graph.matrix;

import gnu.trove.TIntArrayList;
import junit.framework.TestCase;
import org.matsim.contrib.socnetgen.sna.graph.SparseGraph;
import org.matsim.contrib.socnetgen.sna.graph.SparseGraphBuilder;
import org.matsim.contrib.socnetgen.sna.graph.SparseVertex;
import org.matsim.contrib.socnetgen.sna.graph.matrix.AdjacencyMatrix;
import org.matsim.contrib.socnetgen.sna.graph.matrix.Dijkstra;


/**
 * @author illenberger
 *
 */
public class DijkstraTest extends TestCase {

	public void test() {
		SparseGraphBuilder builder = new SparseGraphBuilder();
		SparseGraph graph = builder.createGraph();
		
		SparseVertex[] vertices = new SparseVertex[8];
		for(int i = 0; i < vertices.length; i++) {
			vertices[i] = builder.addVertex(graph);
		}
		
		builder.addEdge(graph, vertices[0], vertices[1]);
		builder.addEdge(graph, vertices[1], vertices[2]);
		builder.addEdge(graph, vertices[1], vertices[3]);
		builder.addEdge(graph, vertices[1], vertices[4]);
		builder.addEdge(graph, vertices[2], vertices[6]);
		builder.addEdge(graph, vertices[3], vertices[5]);
		builder.addEdge(graph, vertices[4], vertices[5]);
		builder.addEdge(graph, vertices[5], vertices[6]);
		builder.addEdge(graph, vertices[6], vertices[7]);
		
		AdjacencyMatrix<?> y = new AdjacencyMatrix<SparseVertex>(graph);
		Dijkstra dijkstra = new Dijkstra(y);
		
		dijkstra.run(0, -1);
		
		TIntArrayList path = dijkstra.getPath(0, 7);
		
		TIntArrayList expected = new TIntArrayList(4);
		expected.add(1);
		expected.add(2);
		expected.add(6);
		expected.add(7);
		
		assertEquals(expected, path);
		
		TIntArrayList[] tree = dijkstra.getSpanningTree();
		TIntArrayList predecessors = new TIntArrayList();
		predecessors.add(3);
		predecessors.add(4);
		assertEquals(predecessors, tree[5]);
		
		dijkstra.run(0, 6);
		assertNull(dijkstra.getPath(0, 7));
		
		assertEquals(new TIntArrayList(), dijkstra.getPath(0, 0));
	}
}
