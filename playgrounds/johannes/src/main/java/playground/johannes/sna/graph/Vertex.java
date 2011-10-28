/* *********************************************************************** *
 * project: org.matsim.*
 * Vertex.java
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

package playground.johannes.sna.graph;

import java.util.List;
import java.util.RandomAccess;


/**
 * Basic representation of a vertex.
 * 
 * @author illenberger
 * 
 */
public interface Vertex {

	/**
	 * Returns the list of edges connected to this vertex. Although, the
	 * returned collection is a list, it must not contain duplicate entries. The
	 * list implementation should implement the {@link RandomAccess}
	 * interface to allow fast iterating over the collection.
	 * 
	 * @return the list of edges connected to this vertex.
	 */
	public List<? extends Edge> getEdges();

	/**
	 * Returns the list of adjacent vertices. Although, the returned collection
	 * is a list, it must not contain duplicate entries. The list implementation
	 * should implement the {@link RandomAccess} interface to allow fast
	 * iterating over the collection.
	 * 
	 * @return the list of adjacent vertices.
	 */
	public List<? extends Vertex> getNeighbours();

}
