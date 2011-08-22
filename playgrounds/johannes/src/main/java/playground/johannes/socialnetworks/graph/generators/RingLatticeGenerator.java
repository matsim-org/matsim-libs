/* *********************************************************************** *
 * project: org.matsim.*
 * RingLatticeGenerator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.johannes.socialnetworks.graph.generators;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.contrib.sna.graph.Edge;
import org.matsim.contrib.sna.graph.Graph;
import org.matsim.contrib.sna.graph.GraphBuilder;
import org.matsim.contrib.sna.graph.Vertex;

/**
 * @author illenberger
 *
 */
public class RingLatticeGenerator<G extends Graph, V extends Vertex, E extends Edge> {

	private static final Logger logger = Logger.getLogger(RingLatticeGenerator.class);
	
	private final GraphBuilder<G, V, E> builder;
	
	public RingLatticeGenerator(GraphBuilder<G, V, E> builder) {
		this.builder = builder;
	}
	
	public G generate(int N, int k) {
		if(k >= N)
			throw new IllegalArgumentException("k must not be >= N!");
		
		G graph = builder.createGraph();
		
		List<V> vertices = new ArrayList<V>(N);
		for(int i = 0; i < N; i++) {
			vertices.add(builder.addVertex(graph));
		}
		
		int doubleEdges = 0;
		
		for(int i = 0; i < N; i++) {
			V source = vertices.get(i);
			for(int j = 0; j < k; j++) {
				int idx = i + j + 1;
				if(idx >= N)
					idx = idx - N;
				
				V target = vertices.get(idx);
				if(builder.addEdge(graph, source, target) == null)
					doubleEdges++;
			}
		}

		if(doubleEdges > 0)
			logger.debug(String.format("Rejected %1$s double edges.", doubleEdges));
		
		return graph;
	}

}
