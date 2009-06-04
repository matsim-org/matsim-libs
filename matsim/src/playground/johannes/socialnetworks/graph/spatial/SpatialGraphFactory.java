/* *********************************************************************** *
 * project: org.matsim.*
 * SpatialGraphFactory.java
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

import org.matsim.api.basic.v01.Coord;
import org.matsim.core.utils.geometry.CoordImpl;

import playground.johannes.socialnetworks.graph.GraphFactory;

/**
 * @author illenberger
 *
 */
public class SpatialGraphFactory implements GraphFactory<SpatialGraph, SpatialVertex, SpatialEdge> {

	public SpatialEdge addEdge(SpatialGraph g, SpatialVertex v1, SpatialVertex v2) {
		SpatialEdge e = new SpatialEdge(v1, v2);
		if(g.insertEdge(e, v1, v2))
			return e;
		else
			return null;
	}

	public SpatialVertex addVertex(SpatialGraph g) {
		SpatialVertex v = new SpatialVertex(new CoordImpl(0.0, 0.0));
		if(g.insertVertex(v))
			return v;
		else
			return null;
	}

	public SpatialVertex addVertex(SpatialGraph g, Coord c) {
		SpatialVertex v = new SpatialVertex(c);
		if(g.insertVertex(v))
			return v;
		else
			return null;
	}
	
	public SpatialGraph createGraph() {
		return new SpatialGraph();
	}

}
