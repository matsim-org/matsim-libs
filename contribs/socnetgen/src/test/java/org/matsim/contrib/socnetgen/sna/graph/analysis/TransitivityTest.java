/* *********************************************************************** *
 * project: org.matsim.*
 * TransitivityTest.java
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

import gnu.trove.TObjectDoubleHashMap;
import junit.framework.TestCase;
import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.matsim.contrib.socnetgen.sna.graph.SparseGraph;
import org.matsim.contrib.socnetgen.sna.graph.SparseGraphBuilder;
import org.matsim.contrib.socnetgen.sna.graph.SparseVertex;
import org.matsim.contrib.socnetgen.sna.graph.Vertex;

import java.util.Set;

/**
 * @author jillenberger
 */
public class TransitivityTest extends TestCase {

	@SuppressWarnings("unchecked")
	public void test() {
		SparseGraphBuilder builder = new SparseGraphBuilder();
		SparseGraph graph = builder.createGraph();

		SparseVertex v1 = builder.addVertex(graph);
		SparseVertex v2 = builder.addVertex(graph);
		SparseVertex v3 = builder.addVertex(graph);
		SparseVertex v4 = builder.addVertex(graph);

		builder.addEdge(graph, v1, v2);
		builder.addEdge(graph, v1, v4);
		builder.addEdge(graph, v1, v3);
		builder.addEdge(graph, v2, v4);

		Transitivity degree = Transitivity.getInstance();

		DescriptiveStatistics distr = degree.localClusteringDistribution(graph.getVertices());
		assertEquals(7/12.0, distr.getMean(), 0.01);
		assertEquals(0.0, distr.getMin());
		assertEquals(1.0, distr.getMax());

		TObjectDoubleHashMap<Vertex> values = (TObjectDoubleHashMap<Vertex>) degree.localClusteringCoefficients((Set<? extends Vertex>)graph.getVertices());

		assertEquals(1/3.0, values.get(v1));
		assertEquals(1.0, values.get(v2));
		assertEquals(1.0, values.get(v4));
		assertEquals(0.0, values.get(v3));

		assertEquals(3/5.0, degree.globalClusteringCoefficient(graph));
	}
}
