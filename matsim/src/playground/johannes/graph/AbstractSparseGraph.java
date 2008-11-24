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
public abstract class AbstractSparseGraph implements Graph {

	private LinkedHashSet<SparseVertex> vertices;
	
	private LinkedHashSet<SparseEdge> edges;
	
	public AbstractSparseGraph() {
		vertices = new LinkedHashSet<SparseVertex>();
		edges = new LinkedHashSet<SparseEdge>();
	}
	
	public Set<? extends SparseEdge> getEdges() {
		return edges;
	}

	public Set<? extends SparseVertex> getVertices() {
		return vertices;
	}

	protected boolean insertVertex(SparseVertex v) {
		return vertices.add(v);
	}
	
	protected boolean insertEdge(SparseEdge e, SparseVertex v1, SparseVertex v2) {
		if(!v1.getNeighbours().contains(v2)) {
			v1.addEdge(e);
			v2.addEdge(e);
			return edges.add(e);
		} else 
			return false;
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
