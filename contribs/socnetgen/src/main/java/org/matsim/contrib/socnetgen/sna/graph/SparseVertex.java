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
package org.matsim.contrib.socnetgen.sna.graph;

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
	 * @see {@link Vertex#getEdges()}
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
	 * @see {@link Vertex#getNeighbours()}.
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
	 * Disconnects this vertex from an edge.
	 * 
	 * @param e
	 *            the edge this vertex is to be disconnected from.
	 * @return <tt>true</tt> if this vertex has been disconnected from
	 *         <tt>e</tt>, <tt>false</tt> if this vertex is not connected
	 *         to edge <tt>e</tt>.
	 */
	boolean removeEdge(SparseEdge e) {
		boolean removedEdge = edges.remove(e);
		boolean removedNeighbour = neighbours.remove(e.getOpposite(this));
		
		if(removedEdge && removedNeighbour)
			return true;
		else if(!removedEdge && !removedNeighbour)
			return false;
		else
			/*
			 * Actually, this should never happen. Think about, if this is really necessary.
			 */
			throw new RuntimeException("Graph connectivity appears to be inconsitent!");
	}
	
	/**
	 * Calls {@link ArrayList#trimToSize()} on all internal lists.
	 */
	void optimize() {
		edges.trimToSize();
		neighbours.trimToSize();
	}
}
