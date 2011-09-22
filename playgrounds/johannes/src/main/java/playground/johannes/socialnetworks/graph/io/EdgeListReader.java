/* *********************************************************************** *
 * project: org.matsim.*
 * EdgeListReader.java
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
package playground.johannes.socialnetworks.graph.io;

import gnu.trove.TIntObjectHashMap;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.contrib.sna.graph.Edge;
import org.matsim.contrib.sna.graph.Graph;
import org.matsim.contrib.sna.graph.GraphBuilder;
import org.matsim.contrib.sna.graph.SparseEdge;
import org.matsim.contrib.sna.graph.SparseGraph;
import org.matsim.contrib.sna.graph.SparseGraphBuilder;
import org.matsim.contrib.sna.graph.SparseVertex;
import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.contrib.sna.graph.io.GraphMLWriter;

/**
 * @author illenberger
 *
 */
public class EdgeListReader<G extends Graph, V extends Vertex, E extends Edge> {
	
	private static final Logger logger = Logger.getLogger(EdgeListReader.class);

	private static final String COMMENT_PREFIX = "#";
	
	private GraphBuilder<G, V, E> builder;
	
	public EdgeListReader(GraphBuilder<G, V, E> builder) {
		this.builder = builder;
	}
	
	public G read(String file) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(file));
		String line;
		
		TIntObjectHashMap<V> vertices = new TIntObjectHashMap<V>();
		G graph = builder.createGraph();
		
		while((line = reader.readLine()) != null) {
			if(!line.startsWith(COMMENT_PREFIX)) {
				String[] tokens = line.split("\t");
				int idx_i = Integer.parseInt(tokens[0]);
				int idx_j = Integer.parseInt(tokens[1]);
				
				V v_i = vertices.get(idx_i);
				if(v_i == null) {
					v_i = builder.addVertex(graph);
					vertices.put(idx_i, v_i);
				}
				
				V v_j = vertices.get(idx_j);
				if(v_j == null) {
					v_j = builder.addVertex(graph);
					vertices.put(idx_j, v_j);
				}
				
				builder.addEdge(graph, v_i, v_j);
				
				if(graph.getEdges().size() % 10000 == 0)
					logger.info(String.format("Loading graph... %1$s vertices, %2$s edges.", graph.getVertices().size(), graph.getEdges().size()));
			}
		}
		
		return graph;
	}
	
	public static void main(String args[]) throws IOException {
		SparseGraphBuilder builder = new SparseGraphBuilder();
		EdgeListReader<SparseGraph, SparseVertex, SparseEdge> reader = new EdgeListReader<SparseGraph, SparseVertex, SparseEdge>(builder);
		SparseGraph graph = reader.read(args[0]);
		GraphMLWriter writer = new GraphMLWriter();
		writer.write(graph, args[1]);
	}
}
