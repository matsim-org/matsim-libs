/* *********************************************************************** *
 * project: org.matsim.*
 * SparseEdge.java
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
package org.matsim.contrib.socnetgen.sna.graph;

import org.matsim.core.utils.collections.Tuple;

/**
 * Representation of an undirected and unweighted edge.
 * 
 * @author illenberger
 * 
 */
public class SparseEdge implements Edge {

	private Tuple<? extends SparseVertex, ? extends SparseVertex> vertices;

	/**
	 * Creates a new edge with <tt>v1</tt> and <tt>v2</tt> as its end points.
	 * 
	 * @param v1
	 *            one of the two vertices the edge is to be connected to.
	 * @param v2
	 *            one of the two vertices the edge is to be connected to.
	 *            @deprecated
	 */
	@Deprecated
	public SparseEdge(SparseVertex v1, SparseVertex v2) {
		vertices = new Tuple<SparseVertex, SparseVertex>(v1, v2);
	}

	/**
	 * Creates an orphaned edge.
	 */
	protected SparseEdge() {
	}
	
	/**
	 * @see {@link Edge#getOpposite(Vertex)}
	 */
	public SparseVertex getOpposite(Vertex v) {
		if (vertices.getFirst().equals(v))
			return vertices.getSecond();
		else if (vertices.getSecond().equals(v))
			return vertices.getFirst();
		else
			return null;
	}

	/**
	 * Sets the end points of this edge to <tt>vertices</tt>.
	 * 
	 * @param vertices the end points of this edge.
	 */
	void setVertices(Tuple<? extends SparseVertex, ? extends SparseVertex> vertices) {
		this.vertices = vertices;
	}
	
	/**
	 * @see {@link Edge#getVertices()}
	 */
	public Tuple<? extends SparseVertex, ? extends SparseVertex> getVertices() {
		return vertices;
	}

}
