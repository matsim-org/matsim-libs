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
import org.matsim.contrib.sna.graph.SparseEdge;
import org.matsim.contrib.sna.graph.SparseGraph;
import org.matsim.contrib.sna.graph.SparseVertex;


/**
 * @author illenberger
 *
 */
public class SpatialSparseGraph extends SparseGraph implements SpatialGraph {

	@SuppressWarnings("unchecked")
	@Override
	public Set<? extends SpatialSparseEdge> getEdges() {
		return (Set<? extends SpatialSparseEdge>) super.getEdges();
	}

	@SuppressWarnings("unchecked")
	@Override
	public Set<? extends SpatialSparseVertex> getVertices() {
		return (Set<? extends SpatialSparseVertex>) super.getVertices();
	}

//	@Override
//	protected boolean insertEdge(SparseEdge e) {
//		return super.insertEdge(e);
//	}
//
//	@Override
//	protected boolean insertVertex(SparseVertex v) {
//		return super.insertVertex(v);
//	}

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
