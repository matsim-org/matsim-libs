/* *********************************************************************** *
 * project: org.matsim.*
 * DegreeTest.java
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
package org.matsim.contrib.socnetgen.sna.graph.analysis;

import gnu.trove.iterator.TObjectDoubleIterator;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import junit.framework.TestCase;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.matsim.contrib.socnetgen.sna.graph.SparseGraph;
import org.matsim.contrib.socnetgen.sna.graph.SparseGraphBuilder;
import org.matsim.contrib.socnetgen.sna.graph.SparseVertex;
import org.matsim.contrib.socnetgen.sna.graph.Vertex;


/**
 * @author illenberger
 *
 */
public class DegreeTest extends TestCase {

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
		
		Degree degree = Degree.getInstance();
		
		DescriptiveStatistics stats = degree.statistics(graph.getVertices());
		assertEquals(2.33, stats.getMean(), 0.01);
		assertEquals(2.0, stats.getMin());
		assertEquals(3.0, stats.getMax());
		
		TObjectDoubleHashMap<? extends Vertex> values = degree.values(graph.getVertices());
		TObjectDoubleIterator<? extends Vertex> it = values.iterator();
		
		int count2 = 0;
		int count3 = 0;
		for(int i = 0; i < values.size(); i++) {
			it.advance();
			if(it.value() == 2)
				count2++;
			else if (it.value() == 3)
				count3++;
		}
		
		assertEquals(4, count2);
		assertEquals(2, count3);
		
		assertEquals(-0.166, degree.assortativity(graph), 0.001);
	}
}
