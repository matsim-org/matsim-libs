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
package org.matsim.contrib.socnetgen.sna.graph.spatial;

import com.vividsolutions.jts.geom.Point;
import org.apache.log4j.Logger;
import org.matsim.contrib.common.gis.CRSUtils;
import org.matsim.contrib.socnetgen.sna.graph.GraphFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;


/**
 * Implementation of GraphFactory to create instances of SpatialSparseGraph,
 * SpatialSparseVertex and SpatilaSparseEdge.
 * 
 * @author illenberger
 *
 */
public class SpatialSparseGraphFactory implements GraphFactory<SpatialSparseGraph, SpatialSparseVertex, SpatialSparseEdge> {

	private static final Logger logger = Logger.getLogger(SpatialSparseGraphFactory.class);
	
	private final CoordinateReferenceSystem crs;
	
	private final int SRID;
	
	/**
	 * Creates a new factory where all spatial information refers to the
	 * coordinated reference system <tt>crs</tt>.
	 * 
	 * @param crs a coordinate reference system.
	 */
	public SpatialSparseGraphFactory(CoordinateReferenceSystem crs) {
		this.crs = crs;
		SRID = CRSUtils.getSRID(crs);
		if(SRID == 0)
			logger.warn("Coordinate reference system has no SRID. Setting SRID to 0.");
	}
	
	/**
	 * @see {@link GraphFactory#createEdge()}
	 */
	@Override
	public SpatialSparseEdge createEdge() {
		return new SpatialSparseEdge();
	}

	/**
	 * @see {@link GraphFactory#createGraph()}
	 */
	@Override
	public SpatialSparseGraph createGraph() {
		return new SpatialSparseGraph(crs);
	}

	/**
	 * @throws {@link UnsupportedOperationException}. Use {@link #createVertex(Point)} instead.
	 */
	@Override
	public SpatialSparseVertex createVertex() {
		throw new UnsupportedOperationException("Cannot create a vertex without spatial information.");
	}
	
	/**
	 * Creates a new vertex located at <tt>point</tt>. The spatial reference id
	 * is the to the one of the factory's coordinate reference system.
	 * 
	 * @param point
	 *            the point in space where the vertex is located.
	 * @return a new spatial vertex.
	 */
	public SpatialSparseVertex createVertex(Point point) {
		if(point != null)
			point.setSRID(SRID);
		return new SpatialSparseVertex(point);
	}

	@Override
	public SpatialSparseGraph copyGraph(SpatialSparseGraph graph) {
		throw new UnsupportedOperationException("Seems like someone is using this method...");
	}

	@Override
	public SpatialSparseVertex copyVertex(SpatialSparseVertex vertex) {
		throw new UnsupportedOperationException("Seems like someone is using this method...");
	}

	@Override
	public SpatialSparseEdge copyEdge(SpatialSparseEdge edge) {
		throw new UnsupportedOperationException("Seems like someone is using this method...");
	}

}
