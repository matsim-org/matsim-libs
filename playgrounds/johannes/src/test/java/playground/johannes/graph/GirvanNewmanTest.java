/* *********************************************************************** *
 * project: org.matsim.*
 * GirvanNewmanTest.java
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import junit.framework.TestCase;

import org.matsim.contrib.sna.graph.GraphBuilder;
import org.matsim.contrib.sna.graph.SparseEdge;
import org.matsim.contrib.sna.graph.SparseGraph;
import org.matsim.contrib.sna.graph.SparseGraphBuilder;
import org.matsim.contrib.sna.graph.SparseVertex;
import org.matsim.contrib.sna.graph.Vertex;

import playground.johannes.socialnetworks.graph.analysis.GirvanNewmanAlgorithm;

/**
 * @author illenberger
 *
 */
public class GirvanNewmanTest extends TestCase {

	public void test() throws IOException {
		GraphBuilder<SparseGraph, SparseVertex, SparseEdge> factory = new SparseGraphBuilder();
		SparseGraph g = factory.createGraph();
		
		SparseVertex[] vertices = new SparseVertex[12];
		for(int i = 0; i < vertices.length; i++)
			vertices[i] = factory.addVertex(g);
		
		SparseEdge[] edges = new SparseEdge[11];
		
		edges[0] = factory.addEdge(g, vertices[0], vertices[1]);
		edges[1] = factory.addEdge(g, vertices[1], vertices[2]);
		edges[2] = factory.addEdge(g, vertices[1], vertices[4]);
		edges[3] = factory.addEdge(g, vertices[3], vertices[4]);
		edges[4] = factory.addEdge(g, vertices[4], vertices[5]);
		edges[5] = factory.addEdge(g, vertices[4], vertices[7]);
		edges[6] = factory.addEdge(g, vertices[6], vertices[7]);
		edges[7] = factory.addEdge(g, vertices[7], vertices[8]);
		edges[8] = factory.addEdge(g, vertices[7], vertices[10]);
		edges[9] = factory.addEdge(g, vertices[9], vertices[10]);
		edges[10] = factory.addEdge(g, vertices[10], vertices[11]);
		
		GirvanNewmanAlgorithm algo = new GirvanNewmanAlgorithm();
		List<Set<Set<Vertex>>> dendogram = algo.dendogram(g, Integer.MAX_VALUE, null);
		
		List<int[]> sizes = new ArrayList<int[]>(5);
		sizes.add(new int[]{6,6});
		sizes.add(new int[]{6,3,3});
		sizes.add(new int[]{3,3,3,3});
		sizes.add(new int[]{3,3,3,2,1});
		sizes.add(new int[]{3,3,2,2,1,1});
		
		for(int i = 0; i < sizes.size(); i++) {
			int[] size = sizes.get(i);
			Set<Set<Vertex>> clusters = dendogram.get(i);
			for(int k = 0; k < size.length; k++) {
				boolean found = false;
				Set<?> theCluster = null;
				for(Set<?> cluster : clusters) {
					if(cluster.size() == size[k]) {
						found = true;
						theCluster = cluster;
						break;
					}
				}
				if(found)
					clusters.remove(theCluster);
				
				assertTrue("Not cluster of appropriate size found", found);
			}
		}
	}
}
