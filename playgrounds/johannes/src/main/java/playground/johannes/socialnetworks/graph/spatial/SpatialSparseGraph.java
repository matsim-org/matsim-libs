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
package playground.johannes.socialnetworks.graph.spatial;

import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.contrib.sna.graph.SparseGraph;
import org.matsim.contrib.sna.graph.SparseVertex;
import org.matsim.contrib.sna.graph.spatial.SpatialGraph;


/**
 * Implementation of {@link SpatialGraph} following the definitions of {@link SparseGraph}.
 * 
 * @author illenberger
 *
 */
public class SpatialSparseGraph extends SparseGraph implements SpatialGraph {

	/**
	 * @see {@link SparseGraph#getEdges()}
	 */
	@SuppressWarnings("unchecked")
	@Override
	public Set<? extends SpatialSparseEdge> getEdges() {
		return (Set<? extends SpatialSparseEdge>) super.getEdges();
	}

	/**
	 * @see {@link SparseGraph#getVertices()}
	 */
	@SuppressWarnings("unchecked")
	@Override
	public Set<? extends SpatialSparseVertex> getVertices() {
		return (Set<? extends SpatialSparseVertex>) super.getVertices();
	}

	/**
	 * @see {@link SparseGraph#getEdge(SparseVertex, SparseVertex)}
	 */
	@Override
	public SpatialSparseEdge getEdge(SparseVertex v_i, SparseVertex v_j) {
		return (SpatialSparseEdge) super.getEdge(v_i, v_j);
	}

	/**
	 * @deprecated will be replaced by something link getEnvelope() from geotools.
	 * @return
	 */
	public double[] getBounds() {
		double[] bounds = new double[4];
		
		double xmin = Double.MAX_VALUE;
		double ymin = Double.MAX_VALUE;
		double xmax = - Double.MAX_VALUE;
		double ymax = - Double.MAX_VALUE;
		
		for(SpatialSparseVertex v : getVertices()) {
			Coord c = v.getCoordinate();
			xmin = Math.min(xmin, c.getX());
			ymin = Math.min(ymin, c.getY());
			xmax = Math.max(xmax, c.getX());
			ymax = Math.max(ymax, c.getY());
		}
		
		bounds[0] = xmin;
		bounds[1] = ymin;
		bounds[2] = xmax;
		bounds[3] = ymax;
		
		return bounds;
	}
}
