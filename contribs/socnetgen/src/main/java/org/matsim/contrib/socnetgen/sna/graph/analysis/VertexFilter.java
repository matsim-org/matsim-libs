/* *********************************************************************** *
 * project: org.matsim.*
 * VertexPartition.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package org.matsim.contrib.socnetgen.sna.graph.analysis;

import org.matsim.contrib.socnetgen.sna.graph.Vertex;

import java.util.Set;

/**
 * Representation of a generic algorithm that modifies an existing set of
 * vertices or creates a modified copy of a set.
 * 
 * @author illenberger
 * 
 */
public interface VertexFilter<V extends Vertex> {

	/**
	 * Applies a modification to <tt>vertices</tt> or creates and returns a
	 * modified copy of <tt>vertices</tt>.
	 * 
	 * @param vertices
	 *            a set of vertices
	 * @return a modified copy of vertices.
	 */
	public Set<V> apply(Set<V> vertices);

}
