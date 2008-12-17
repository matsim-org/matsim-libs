/* *********************************************************************** *
 * project: org.matsim.*
 * SparseVertex.java
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

import java.util.ArrayList;
import java.util.List;
import java.util.RandomAccess;

/**
 * Representation of a vertex without any further attributes.
 * 
 * @author illenberger
 * 
 */
public class SparseVertex implements Vertex {

	private ArrayList<SparseEdge> edges;

	private ArrayList<SparseVertex> neighbours;

	/**
	 * Creates a new isolated vertex.
	 */
	protected SparseVertex() {
		edges = new ArrayList<SparseEdge>();
		neighbours = new ArrayList<SparseVertex>();
	}

	/**
	 * Returns the list of edges connected to this vertex. The list implements
	 * the {@link RandomAccess} interface.
	 * 
	 * @return the list of edges connected to this vertex.
	 */
	public List<? extends SparseEdge> getEdges() {
		return edges;
	}

	/**
	 * Returns the list of adjacent vertices. The list implements the
	 * {@link RandomAccess} interface.
	 * 
	 * @return the list of adjacent vertices.
	 */

	public List<? extends SparseVertex> getNeighbours() {
		return neighbours;
	}

	/**
	 * Connects this vertex to an edge. This method does not check if <tt>e</tt>
	 * is already connected to this vertex!
	 * 
	 * @param e
	 *            the edge this vertex is to be connected to.
	 */
	void addEdge(SparseEdge e) {
		edges.add(e);
		neighbours.add(e.getOpposite(this));
	}

	/**
	 * Calls {@link ArrayList#trimToSize()} on all internal list.
	 */
	void optimize() {
		edges.trimToSize();
		neighbours.trimToSize();
	}
}
