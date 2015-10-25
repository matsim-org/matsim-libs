/* *********************************************************************** *
 * project: org.matsim.*
 * SparseGraphFactory2.java
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
package org.matsim.contrib.socnetgen.sna.graph;

import junit.framework.TestCase;

/**
 * 
 * @author illenberger
 *
 */
public class GraphProjectionTest extends TestCase {

	public void test1() {
		SparseGraphBuilder sparseBuilder = new SparseGraphBuilder();
		
		SparseGraph sparseGraph = sparseBuilder.createGraph();
		
		SparseVertex vertex1 = sparseBuilder.addVertex(sparseGraph);
		SparseVertex vertex2 = sparseBuilder.addVertex(sparseGraph);
		SparseVertex vertex3 = sparseBuilder.addVertex(sparseGraph);
		SparseVertex vertex4 = sparseBuilder.addVertex(sparseGraph);
		
		SparseEdge edge1 = sparseBuilder.addEdge(sparseGraph, vertex1, vertex2);
		SparseEdge edge2 = sparseBuilder.addEdge(sparseGraph, vertex2, vertex3);
		SparseEdge edge3 = sparseBuilder.addEdge(sparseGraph, vertex3, vertex4);
		SparseEdge edge4 = sparseBuilder.addEdge(sparseGraph, vertex4, vertex1);
		
		SparseGraphProjectionBuilder<SparseGraph, SparseVertex, SparseEdge> projBuilder = new SparseGraphProjectionBuilder<SparseGraph, SparseVertex, SparseEdge>();
		
		GraphProjection<SparseGraph, SparseVertex, SparseEdge> graphProj = projBuilder.decorateGraph(sparseGraph);
		
		assertEquals(4, graphProj.getVertices().size());
		assertEquals(4, graphProj.getEdges().size());
		
		assertEquals(vertex1, graphProj.getVertex(vertex1).getDelegate());
		assertEquals(vertex2, graphProj.getVertex(vertex2).getDelegate());
		assertEquals(vertex3, graphProj.getVertex(vertex3).getDelegate());
		assertEquals(vertex4, graphProj.getVertex(vertex4).getDelegate());
		
		for(SparseVertex v : sparseGraph.getVertices()) {
			boolean found = false;
		
			for(VertexDecorator<SparseVertex> vd : graphProj.getVertices()) {
				if(v == vd.getDelegate()) {
					found = true;
					break;
				}
			}
			
			assertTrue(found);
		}
		
		VertexDecorator<SparseVertex> vd1 = graphProj.getVertex(vertex1);
		VertexDecorator<SparseVertex> vd2 = graphProj.getVertex(vertex2);
		VertexDecorator<SparseVertex> vd3 = graphProj.getVertex(vertex3);
		VertexDecorator<SparseVertex> vd4 = graphProj.getVertex(vertex4);
		
		assertEquals(edge1, graphProj.getEdge(vd1, vd2).getDelegate());
		assertEquals(edge2, graphProj.getEdge(vd2, vd3).getDelegate());
		assertEquals(edge3, graphProj.getEdge(vd3, vd4).getDelegate());
		assertEquals(edge4, graphProj.getEdge(vd4, vd1).getDelegate());
	}
}
