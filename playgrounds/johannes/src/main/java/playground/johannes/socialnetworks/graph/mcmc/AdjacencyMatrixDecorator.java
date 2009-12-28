/* *********************************************************************** *
 * project: org.matsim.*
 * SNAdjacencyMatrix.java
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
package playground.johannes.socialnetworks.graph.mcmc;

import java.util.ArrayList;
import java.util.List;

import org.matsim.contrib.sna.graph.Edge;
import org.matsim.contrib.sna.graph.Graph;
import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.core.utils.collections.Tuple;


/**
 * @author illenberger
 *
 */
public class AdjacencyMatrixDecorator<V extends Vertex> extends AdjacencyMatrix {

	private List<V> vertices;
	
	@SuppressWarnings("unchecked")
	public AdjacencyMatrixDecorator(Graph g) {
		super();
		vertices = new ArrayList<V>(g.getVertices().size());
		
		int idx = 0;
		for(Vertex v : g.getVertices()) {
			vertices.add((V)v);
			addVertex();
			idx++;
		}
		
		for(Edge e : g.getEdges()) {
			Tuple<? extends Vertex, ? extends Vertex> p = e.getVertices();
			int i = vertices.indexOf(p.getFirst());
			int j = vertices.indexOf(p.getSecond());
			
			if(i > -1 && j > -1) {
				addEdge(i, j);
			} else {
				throw new IllegalArgumentException(String.format("Indices i=%1$s, j=%2$s not allowed!", i, j));
			}
		}
	}
	
	public V getVertex(int i) {
		return vertices.get(i);
	}
}
