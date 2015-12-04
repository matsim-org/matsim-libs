/* *********************************************************************** *
 * project: org.matsim.*
 * ComponentsTest.java
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

import junit.framework.TestCase;
import org.matsim.contrib.socnetgen.sna.graph.SparseGraph;
import org.matsim.contrib.socnetgen.sna.graph.SparseGraphBuilder;
import org.matsim.contrib.socnetgen.sna.graph.SparseVertex;
import org.matsim.contrib.socnetgen.sna.graph.Vertex;

import java.util.List;
import java.util.Set;


/**
 * @author illenberger
 *
 */
public class ComponentsTest extends TestCase {

	public void test() {
		SparseGraphBuilder builder = new SparseGraphBuilder();
		SparseGraph graph = builder.createGraph();
		
		for(int i = 1; i < 10; i += 2) {
			makeComponent(graph, builder, i);
		}
		
		Components components = new Components();
		assertEquals(5, components.countComponents(graph));
		
		List<Set<Vertex>> sets = components.components(graph);
		for(int i = 0; i < sets.size(); i++) {
			assertEquals(((sets.size() - i - 1) * 2) + 1, sets.get(i).size());
		}
	}
	
	private static void makeComponent(SparseGraph graph, SparseGraphBuilder builder, int size) {
		if(size > 0) {
			SparseVertex v_i = builder.addVertex(graph);
			for(int i = 1; i < size; i++) {
				SparseVertex v_j = builder.addVertex(graph);
				builder.addEdge(graph, v_i, v_j);
				v_i = v_j;
			}
		}
	}
}
