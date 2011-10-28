/* *********************************************************************** *
 * project: org.matsim.*
 * SpatialSparseGraphBuilder.java
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
package playground.johannes.sna.graph.spatial;

import org.opengis.referencing.crs.CoordinateReferenceSystem;

import playground.johannes.sna.graph.AbstractSparseGraphBuilder;
import playground.johannes.sna.graph.GraphFactory;

import com.vividsolutions.jts.geom.Point;


/**
 * An extension to AbstractSparseGraphBuilder to build SpatialSparseGraphs.
 * 
 * @author illenberger
 *
 */
public class SpatialSparseGraphBuilder extends AbstractSparseGraphBuilder<SpatialSparseGraph, SpatialSparseVertex, SpatialSparseEdge> {
	
	/**
	 * @param factory
	 */
	public SpatialSparseGraphBuilder(GraphFactory<SpatialSparseGraph, SpatialSparseVertex, SpatialSparseEdge> factory) {
		super(factory);
		// TODO Auto-generated constructor stub
	}

	/**
	 * Creates a new builder where all spatial information refers to the
	 * coordinated reference system <tt>crs</tt>.
	 * 
	 * @param crs
	 *            a coordinate reference system.
	 */
	public SpatialSparseGraphBuilder(CoordinateReferenceSystem crs) {
		super(new SpatialSparseGraphFactory(crs));
	}

	/**
	 * Creates and adds a new vertex to a graph. The spatial reference id of
	 * <tt>point</tt> will be set to the one of the builders spatial reference
	 * system.
	 * 
	 * @param graph
	 *            the graph a new vertex should be inserted in.
	 * @param point
	 *            the point in space where the vertex is located.
	 * 
	 * @return the newly inserted vertex, or <tt>null</tt> is the insertion of a
	 *         vertex would violate the definition of the underlying graph.
	 */
	public SpatialSparseVertex addVertex(SpatialSparseGraph graph, Point point) {
		SpatialSparseVertex vertex = ((SpatialSparseGraphFactory)getFactory()).createVertex(point);
		if(insertVertex(graph, vertex))
			return vertex;
		else
			return null;
	}
}
