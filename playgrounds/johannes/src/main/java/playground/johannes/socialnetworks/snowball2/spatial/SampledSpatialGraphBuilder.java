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
 * Extension to {@link AbstractSparseGraphBuilder} to build sampled spatial sparse graphs.
 * 
 * @author illenberger
 *
 */
public class SampledSpatialGraphBuilder extends
		AbstractSparseGraphBuilder<SampledSpatialSparseGraph, SampledSpatialSparseVertex, SampledSpatialSparseEdge> {
	
	/**
	 * Creates a new builder where all spatial information refers to the
	 * coordinated reference system <tt>crs</tt>.
	 * 
	 * @param crs
	 *            a coordinate reference system.
	 */
	public SampledSpatialGraphBuilder(CoordinateReferenceSystem crs) {
		super(new SampledSpatialGraphFactory(crs));
	}

	@Override
	public SampledSpatialSparseVertex addVertex(SampledSpatialSparseGraph graph) {
		throw new UnsupportedOperationException();
	}
	
	/**
	 * Creates and adds a new undetected and unsampled vertex to a graph. The
	 * spatial reference id of <tt>point</tt> will be set to the one of the
	 * builders spatial reference system.
	 * 
	 * @param graph
	 *            the graph a new vertex should be inserted in.
	 * @param point
	 *            the point in space where the vertex is located.
	 * 
	 * @return the newly inserted vertex, or <tt>null</tt> is the insertion of a
	 *         vertex would violate the definition of the underlying graph.
	 */
	public SampledSpatialSparseVertex addVertex(SampledSpatialSparseGraph graph, Point point) {
		SampledSpatialSparseVertex vertex = ((SampledSpatialGraphFactory)getFactory()).createVertex(point);
		if(insertVertex(graph, vertex))
			return vertex;
		else
			return null;
	}
}
