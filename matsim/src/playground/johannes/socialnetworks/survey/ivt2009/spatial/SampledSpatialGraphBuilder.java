/* *********************************************************************** *
 * project: org.matsim.*
 * SampledSpatialGraphBuilder.java
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
package playground.johannes.socialnetworks.survey.ivt2009.spatial;

import org.matsim.api.basic.v01.Coord;
import org.matsim.core.utils.geometry.CoordImpl;

import playground.johannes.socialnetworks.graph.GraphFactory;

/**
 * @author illenberger
 *
 */
public class SampledSpatialGraphBuilder implements GraphFactory<SampledSpatialGraph, SampledSpatialVertex, SampledSpatialEdge> {
	
	public SampledSpatialEdge addEdge(SampledSpatialGraph g, SampledSpatialVertex v1, SampledSpatialVertex v2) {
		SampledSpatialEdge e = new SampledSpatialEdge(v1, v2);
		if(g.insertEdge(e, v1, v2))
			return e;
		else
			return null;
	}

	public SampledSpatialVertex addVertex(SampledSpatialGraph g) {
		SampledSpatialVertex v = new SampledSpatialVertex(new CoordImpl(0.0, 0.0));
		if(g.insertVertex(v))
			return v;
		else
			return null;
	}

	public SampledSpatialVertex addVertex(SampledSpatialGraph g, Coord c) {
		SampledSpatialVertex v = new SampledSpatialVertex(c);
		if(g.insertVertex(v))
			return v;
		else
			return null;
	}
	
	public SampledSpatialGraph createGraph() {
		return new SampledSpatialGraph();
	}

}
