/* *********************************************************************** *
 * project: org.matsim.*
 * SampledSpatialGraphFactory.java
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
package playground.johannes.socialnetworks.snowball2.spatial;

import org.matsim.api.core.v01.Coord;
import org.matsim.contrib.sna.graph.GraphFactory;


/**
 * @author illenberger
 *
 */
public class SampledSpatialGraphFactory implements GraphFactory<SampledSpatialSparseGraph, SampledSpatialSparseVertex, SampledSpatialSparseEdge>{

	public SampledSpatialSparseEdge createEdge() {
		return new SampledSpatialSparseEdge();
	}

	public SampledSpatialSparseGraph createGraph() {
		return new SampledSpatialSparseGraph();
	}

	public SampledSpatialSparseVertex createVertex() {
		throw new UnsupportedOperationException();
	}
	
	public SampledSpatialSparseVertex createVertex(Coord coord) {
		return new SampledSpatialSparseVertex(coord);
	}

}
