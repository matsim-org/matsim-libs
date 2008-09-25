/* *********************************************************************** *
 * project: org.matsim.*
 * SparseGraph.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
package playground.johannes.graph;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * @author illenberger
 *
 */
public class SparseGraph<T> implements Graph {

	private LinkedHashSet<SparseVertex> vertices = new LinkedHashSet<SparseVertex>();
	
	private LinkedHashSet<SparseEdge<T>> edges = new LinkedHashSet<SparseEdge<T>>();
	
	public Set<? extends SparseEdge<T>> getEdges() {
		return edges;
	}

	public Set<? extends SparseVertex> getVertices() {
		return vertices;
	}

	public SparseVertex addVertex() {
		SparseVertex v = newVertex();
		vertices.add(v);
		return v;
	}
	
	public SparseEdge<T> addEdge(SparseVertex v1, SparseVertex v2) {
		SparseEdge<T> e = newEdge(v1, v2);
		v1.addEdge(e);
		v2.addEdge(e);
		edges.add(e);
		return e;
	}
	
	protected SparseVertex newVertex() {
		return new SparseVertex();
	}
	
	protected SparseEdge<T> newEdge(SparseVertex v1, SparseVertex v2) {
		return new SparseEdge<T>(v1, v2);
	}
	
	public void optimize() {
		for(SparseVertex v : vertices)
			v.optimize();
	}
	
	public String toString() {
		StringBuilder builder = new StringBuilder(60);
		builder.append("SparseGraph: ");
		builder.append(String.valueOf(vertices.size()));
		builder.append(" vertices, ");
		builder.append(String.valueOf(edges.size()));
		builder.append(" edges");
		return builder.toString();
	}
}
