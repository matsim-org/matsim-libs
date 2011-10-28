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
package playground.johannes.sna.graph;

import java.util.LinkedHashSet;
import java.util.Set;


/**
 * Representation of an undirected and unweighted mathematical sparse graph.
 * 
 * @author illenberger
 * 
 */
public class SparseGraph implements Graph {

	private LinkedHashSet<SparseVertex> vertices;

	private LinkedHashSet<SparseEdge> edges;

	/**
	 * Creates an empty graph.
	 */
	public SparseGraph() {
		vertices = new LinkedHashSet<SparseVertex>();
		edges = new LinkedHashSet<SparseEdge>();
	}

	/**
	 * Returns the set of vertices. It is discouraged to do modification to the
	 * returned set. To modify a graph use {@link SparseGraphBuilder}, otherwise
	 * graph consistency can not be guaranteed.
	 * 
	 * @see {@link Graph#getVertices()}
	 */
	public Set<? extends SparseVertex> getVertices() {
		return vertices;
	}

	/**
	 * Returns the set of edges. It is discouraged to do modification to the
	 * returned set. To modify a graph use {@link SparseGraphBuilder}, otherwise
	 * graph consistency can not be guaranteed.
	 * 
	 * @see {@link Graph#getEdges()}
	 */
	public Set<? extends SparseEdge> getEdges() {
		return edges;
	}

	/**
	 * Inserts a vertex into this graph if it is not already part of the graph.
	 * Do not use this call directly. Use
	 * {@link SparseGraphBuilder#addVertex(SparseGraph)}.
	 * 
	 * @param v
	 *            the vertex to be inserted.
	 * @return <tt>true</tt> if the vertex is inserted into this graph,
	 *         <tt>false</tt> otherwise.
	 */
	boolean insertVertex(SparseVertex v) {
		return vertices.add(v);
	}

	/**
	 * Inserts an edge into this graph if it is not already part of the graph.
	 * Do not use this call directly. Use
	 * {@link SparseGraphBuilder#addEdge(SparseGraph, SparseVertex, SparseVertex)}.
	 * 
	 * @param e
	 *            the edge to be inserted.
	 * @return <tt>true</tt> if the edge is inserted into this graph,
	 *         <tt>false</tt> otherwise.
	 */
	boolean insertEdge(SparseEdge e) {
		return edges.add(e);
	}

	/**
	 * Removes a vertex from the graph. Do not use this call directly. Use
	 * {@link SparseGraphBuilder#removeVertex(SparseGraph, SparseVertex)}.
	 * 
	 * @param v
	 *            the vertex to be removed from the graph.
	 * 
	 * @return <tt>true</tt> if the vertex has been removed from the graph,
	 *         <tt>false</tt> if <tt>v</tt> is not part of this graph.
	 */
	boolean removeVertex(SparseVertex v) {
		return vertices.remove(v);
	}

	/**
	 * Removes an edge from the graph. Do not use this call directly. Use
	 * {@link SparseGraphBuilder#removeEdge(SparseGraph, SparseEdge)}.
	 * 
	 * @param e
	 *            the edge to be removed.
	 * @return <tt>true</tt> if the edge has been removed from the graph,
	 *         <tt>false</tt> if <tt>e</tt> is not part of this graph.
	 */
	boolean removeEdge(SparseEdge e) {		
		return edges.remove(e);
	}
	
	/**
	 * Optimizes the internal storage structure.
	 */
	public void optimize() {
		for (SparseVertex v : vertices)
			v.optimize();
	}

	/**
	 * Returns the edge connecting vertex <tt>v_i</tt> and <tt>v_j</tt>. If
	 * there are multiple edges connecting <tt>v_i</tt> and <tt>v_j</tt> it is
	 * not defined which edge is returned.
	 * 
	 * @param v_i
	 *            a vertex.
	 * @param v_j
	 *            a vertex.
	 * @return the edge connecting vertex <tt>v_i</tt> and <tt>v_j</tt>, or
	 *         <tt>null</tt> if <tt>v_i</tt> and <tt>v_j</tt> are not connected
	 *         with each other.
	 */
	public SparseEdge getEdge(SparseVertex v_i, SparseVertex v_j) {
		SparseEdge e = null;
		int cnt = v_i.getEdges().size();
		for(int i = 0; i < cnt; i++) {
			e = v_i.getEdges().get(i);
			if(e.getOpposite(v_i) == v_j) {
				return e;
			}
		}
		
		return null;
	}

	/**
	 * Returns a short description for this graph.
	 * 
	 * @return a short description for this grpah.
	 */
	@Override
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
