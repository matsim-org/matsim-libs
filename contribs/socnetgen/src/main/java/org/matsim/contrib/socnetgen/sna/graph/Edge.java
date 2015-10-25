/* *********************************************************************** *
 * project: org.matsim.*
 * Edge.java
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
 * Basic representation of an undirected and unweighted edge.
 * 
 * @author illenberger
 * 
 */
public interface Edge {

	/**
	 * Returns a tuple of vertices connected to this edge. The order of the
	 * vertices is arbitrary.
	 * 
	 * @return a tuple of vertices connected to this edge.
	 */
	public Tuple<? extends Vertex, ? extends Vertex> getVertices();

	/**
	 * Returns the vertex that is opposing to vertex <tt>v</tt>.
	 * 
	 * @param v
	 *            a vertex connected to this edge.
	 * @return the vertex that is opposing to vertex <tt>v</tt>, or
	 *         <tt>null</tt> if <tt>v</tt> is not connected to this edge.
	 */
	public Vertex getOpposite(Vertex v);

}
