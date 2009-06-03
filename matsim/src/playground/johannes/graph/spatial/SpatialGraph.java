/* *********************************************************************** *
 * project: org.matsim.*
 * SpatialGraph.java
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
package playground.johannes.graph.spatial;

import java.util.Set;

import playground.johannes.graph.AbstractSparseGraph;

/**
 * @author illenberger
 *
 */
public class SpatialGraph extends AbstractSparseGraph {

	@SuppressWarnings("unchecked")
	@Override
	public Set<? extends SpatialEdge> getEdges() {
		return (Set<? extends SpatialEdge>) super.getEdges();
	}

	@SuppressWarnings("unchecked")
	@Override
	public Set<? extends SpatialVertex> getVertices() {
		return (Set<? extends SpatialVertex>) super.getVertices();
	}

}
