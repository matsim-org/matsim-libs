/* *********************************************************************** *
 * project: org.matsim.*
 * AdjacencyMatrixTest.java
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

/**
 * 
 */
package org.matsim.contrib.socnetgen.sna.graph.matrix;

import junit.framework.TestCase;
import org.apache.commons.collections.CollectionUtils;
import org.matsim.contrib.socnetgen.sna.graph.*;
import org.matsim.contrib.socnetgen.sna.graph.analysis.Degree;
import org.matsim.contrib.socnetgen.sna.graph.generators.ErdosRenyiGenerator;

import java.util.List;

/**
 * @author illenberger
 *
 */
public class AdjacencyMatrixTest extends TestCase {

	public void testConvert() {
		SparseGraphBuilder builder = new SparseGraphBuilder();
		ErdosRenyiGenerator<SparseGraph, SparseVertex, SparseEdge> generator = new ErdosRenyiGenerator<SparseGraph, SparseVertex, SparseEdge>(builder);
		SparseGraph g1 = generator.generate(1000, 0.01, 0);
		
		AdjacencyMatrix<SparseVertex> m = new AdjacencyMatrix<SparseVertex>(g1);
		
		Graph g2 = m.getGraph(new SparseGraphBuilder());
		
		Degree degree = Degree.getInstance();
		assertEquals(g2.getVertices().size(), g1.getVertices().size());
		assertEquals(g2.getEdges().size(), g1.getEdges().size());
		assertEquals(degree.statistics(g2.getVertices()).getMean(), degree.statistics(g1.getVertices()).getMean());
		
		for(int i = 0; i < 100; i+=2) {
			m.addEdge(i, i+1);
		}
		
		for(int i = 0; i < 100; i+=2) {
			m.removeEdge(i+1, i);
		}
		
		Graph g3 = m.getGraph(new SparseGraphBuilder());
		assertEquals(g3.getEdges().size(), g1.getEdges().size());
		assertEquals(degree.statistics(g2.getVertices()).getMean(), degree.statistics(g1.getVertices()).getMean());
		
		for(int i = 0; i < 100; i+=2) {
			m.addEdge(i, i+1);
		}
		
		m.synchronizeEdges(g1, builder);
		assertEquals(m.countEdges(), g1.getEdges().size());
	}

	public void testCommonNeighbours() {
		AdjacencyMatrix<SparseVertex> m = new AdjacencyMatrix<SparseVertex>(true);
		SparseGraphBuilder builder = new SparseGraphBuilder();
		SparseGraph g = builder.createGraph();
		
		m.addVertex();
		SparseVertex v0 = builder.addVertex(g);
		
		m.addVertex();
		SparseVertex v1 = builder.addVertex(g);
		
		m.addVertex();
		SparseVertex v2 = builder.addVertex(g);
		
		m.addVertex();
		SparseVertex v3 = builder.addVertex(g);
		
		m.addVertex();
		SparseVertex v4 = builder.addVertex(g);
		
		m.addEdge(0, 1);
		builder.addEdge(g, v0, v1);
		
		m.addEdge(0, 2);
		builder.addEdge(g, v0, v2);
		
		m.addEdge(0, 3);
		builder.addEdge(g, v0, v3);
		
		m.addEdge(2, 1);
		builder.addEdge(g, v2, v1);
		
		m.addEdge(3, 1);
		builder.addEdge(g, v3, v1);
		
		m.addEdge(4, 1);
		builder.addEdge(g, v4, v1);
		
		assertEquals(m.getCommonNeighbors(0, 1), 2);
		assertEquals(m.getCommonNeighbors(1, 0), 2);
		assertEquals(countCommonNeighbours(v0, v1), 2);
		
		assertEquals(m.getCommonNeighbors(0, 4), 1);
		assertEquals(countCommonNeighbours(v0, v4), 1);
	}
	
	private int countCommonNeighbours(Vertex v1, Vertex v2) {
		List<? extends Vertex> n1set = v1.getNeighbours();
		List<? extends Vertex> n2set = v2.getNeighbours();
		
		return CollectionUtils.intersection(n1set, n2set).size();
	}
}
