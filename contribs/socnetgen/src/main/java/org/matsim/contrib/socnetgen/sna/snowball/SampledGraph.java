/* *********************************************************************** *
 * project: org.matsim.*
 * SampledGraph.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package org.matsim.contrib.socnetgen.sna.snowball;

import org.matsim.contrib.socnetgen.sna.graph.Graph;

import java.util.Set;


/**
 * Representation of a snowball sampled graph.
 * 
 * @author illenberger
 *
 */
public interface SampledGraph extends Graph {

	/**
	 * @see {@link Graph#getVertices()}
	 */
	public Set<? extends SampledVertex> getVertices();

	/**
	 * @see {@link Graph#getEdges()}
	 */
	public Set<? extends SampledEdge> getEdges();

}
