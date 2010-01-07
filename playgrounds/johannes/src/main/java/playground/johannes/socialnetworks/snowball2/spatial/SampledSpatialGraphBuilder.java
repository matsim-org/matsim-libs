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
package playground.johannes.socialnetworks.snowball2.spatial;

import org.matsim.contrib.sna.graph.AbstractSparseGraphBuilder;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Point;



/**
 * @author illenberger
 *
 */
public class SampledSpatialGraphBuilder extends AbstractSparseGraphBuilder<SampledSpatialSparseGraph, SampledSpatialSparseVertex, SampledSpatialSparseEdge> {
	
	public SampledSpatialGraphBuilder(CoordinateReferenceSystem crs) {
		super(new SampledSpatialGraphFactory(crs));
	}

	@Override
	public SampledSpatialSparseVertex addVertex(SampledSpatialSparseGraph graph) {
		throw new UnsupportedOperationException();
	}
	
	public SampledSpatialSparseVertex addVertex(SampledSpatialSparseGraph graph, Point point) {
		SampledSpatialSparseVertex vertex = ((SampledSpatialGraphFactory)getFactory()).createVertex(point);
		if(insertVertex(graph, vertex))
			return vertex;
		else
			return null;
	}
}
