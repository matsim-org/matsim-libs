/* *********************************************************************** *
 * project: org.matsim.*
 * Graph.java
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

import java.util.Set;


/**
 * Basic representation of a mathematical graph.
 * 
 * @author illenberger
 * 
 */
public interface Graph {

	/**
	 * Returns the set of vertices.
	 * 
	 * @return the set of vertices.
	 */
	public Set<? extends Vertex> getVertices();

	/**
	 * Returns the set of edges.
	 * 
	 * @return the set of edges.
	 */
	public Set<? extends Edge> getEdges();

}
