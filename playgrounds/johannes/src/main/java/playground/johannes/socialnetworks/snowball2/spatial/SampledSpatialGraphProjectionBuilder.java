/* *********************************************************************** *
 * project: org.matsim.*
 * SampledSpatialGraphProjectionBuilder.java
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
package playground.johannes.socialnetworks.snowball2.spatial;

import org.matsim.contrib.sna.snowball.spatial.SampledSpatialEdge;
import org.matsim.contrib.sna.snowball.spatial.SampledSpatialGraph;
import org.matsim.contrib.sna.snowball.spatial.SampledSpatialVertex;

import playground.johannes.socialnetworks.graph.spatial.SpatialGraphProjectionBuilder;

import com.vividsolutions.jts.geom.Geometry;

/**
 * @author illenberger
 *
 */
public class SampledSpatialGraphProjectionBuilder <G extends SampledSpatialGraph, V extends SampledSpatialVertex, E extends SampledSpatialEdge> extends
												SpatialGraphProjectionBuilder<G, V, E> {

	public SampledSpatialGraphProjectionBuilder() {
		super(new SampledSpatialGraphProjectionFactory<G, V, E>());
	}
	
	@Override
	public SampledSpatialGraphProjection<G, V, E> decorate(G delegate,
			Geometry geometry) {
		return (SampledSpatialGraphProjection<G, V, E>) super.decorate(delegate, geometry);
	}

}
