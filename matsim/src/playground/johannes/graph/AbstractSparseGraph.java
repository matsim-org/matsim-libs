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
 * Abstract representation of an undirected and unweighted mathematical graph
 * that does not allow multiple edges between two vertices. The abstract class
 * provides only read-access. Write-access is to be implemented by its
 * subclasses.
 * 
 * @author illenberger
 * 
 */
public abstract class AbstractSparseGraph implements Graph {

	private LinkedHashSet<SparseVertex> vertices;

	private LinkedHashSet<SparseEdge> edges;

	/**
	 * Creates an empty graph.
	 */
	public AbstractSparseGraph() {
		vertices = new LinkedHashSet<SparseVertex>();
		edges = new LinkedHashSet<SparseEdge>();
	}

	/**
	 * @see {@link Graph#getEdges()}
	 */
	public Set<? extends SparseEdge> getEdges() {
		return edges;
	}

	/**
	 * @see {@link Graph#getVertices()}
	 */
	public Set<? extends SparseVertex> getVertices() {
		return vertices;
	}

	/**
	 * Inserts a vertex into this graph if it is not already part of the graph.
	 * 
	 * @param v
	 *            the vertex to be inserted.
	 * @return <tt>true</tt> if the vertex is inserted into this graph,
	 *         <tt>false</tt> otherwise.
	 */
	protected boolean insertVertex(SparseVertex v) {
		return vertices.add(v);
	}

	/**
	 * Inserts a edge into this graph and assured correct connectivity. Multiple
	 * edges between one pair of vertices are not allowed.
	 * 
	 * @param e
	 *            the edge to be inserted.
	 * @param v1
	 *            one of the two vertices the edge is to be connected to.
	 * @param v2
	 *            one of the two vertices the edge is to be connected to.
	 * @return <tt>true</tt> if the edge is inserted into this graph,
	 *         <tt>false</tt> if <tt>e</tt> is already part of this graph or
	 *         <tt>v1</tt> and <tt>v2</tt> are already connected by an edge.
	 */
	protected boolean insertEdge(SparseEdge e, SparseVertex v1, SparseVertex v2) {
		if (!v1.getNeighbours().contains(v2)) {
			v1.addEdge(e);
			v2.addEdge(e);
			return edges.add(e);
		} else
			return false;
	}

	/**
	 * Optimizes the internal storage structure.
	 */
	public void optimize() {
		for (SparseVertex v : vertices)
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
