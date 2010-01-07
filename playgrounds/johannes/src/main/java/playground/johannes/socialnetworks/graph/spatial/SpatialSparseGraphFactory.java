/* *********************************************************************** *
 * project: org.matsim.*
 * SpatialSparseGraphFactory.java
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

import org.matsim.contrib.sna.graph.GraphFactory;

import com.vividsolutions.jts.geom.Point;


/**
 * @author illenberger
 *
 */
public class SpatialSparseGraphFactory implements GraphFactory<SpatialSparseGraph, SpatialSparseVertex, SpatialSparseEdge> {

	public SpatialSparseEdge createEdge() {
		return new SpatialSparseEdge();
	}

	public SpatialSparseGraph createGraph() {
		return new SpatialSparseGraph();
	}

	public SpatialSparseVertex createVertex() {
		return new SpatialSparseVertex(null);
	}
	
	public SpatialSparseVertex createVertex(Point point) {
		return new SpatialSparseVertex(point);
	}

}
