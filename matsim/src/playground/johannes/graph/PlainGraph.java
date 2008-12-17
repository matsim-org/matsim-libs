/* *********************************************************************** *
 * project: org.matsim.*
 * PlainGraph.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

/**
 * Simple implementation of an {@link AbstractSparseGraph} without any further
 * vertex or edge attributes.
 * 
 * @author illenberger
 * 
 */
public class PlainGraph extends AbstractSparseGraph {

	/**
	 * Inserts a new vertex into this graph.
	 * 
	 * @return the inserted vertex.
	 */
	public SparseVertex addVertex() {
		SparseVertex v = new SparseVertex();
		if (insertVertex(v))
			return v;
		else
			return null;
	}

	/**
	 * Inserts a new edge into this graph.
	 * 
	 * @param v1
	 *            one of the two vertices the edge is to be connected to.
	 * @param v2
	 *            one of the two vertices the edge is to be connected to.
	 * @return the inserted edge, or <tt>null</tt> if <tt>v1</tt> and
	 *         <tt>v2</tt> are already connected by an edge.
	 */
	public SparseEdge addEdge(SparseVertex v1, SparseVertex v2) {
		SparseEdge e = new SparseEdge(v1, v2);
		if (insertEdge(e, v1, v2))
			return e;
		else
			return null;
	}

}
