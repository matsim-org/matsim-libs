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

import org.matsim.contrib.sna.gis.CRSUtils;
import org.matsim.contrib.sna.graph.GraphFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Point;

/**
 * Implementation of {@link GraphFactory} to create new
 * {@link SampledSpatialSparseGraph}, {@link SampledSpatialSparseVertex} and
 * {@link SampledSpatialSparseEdge}.
 * 
 * @author illenberger
 * 
 */
public class SampledSpatialGraphFactory	implements
		GraphFactory<SampledSpatialSparseGraph, SampledSpatialSparseVertex, SampledSpatialSparseEdge> {

	private final CoordinateReferenceSystem crs;
	
	private final int SRID;

	/**
	 * Creates a new sampled spatial sparse graph factory with the given
	 * coordinate reference system.
	 * 
	 * @param crs
	 *            a coordinate reference system.
	 */
	public SampledSpatialGraphFactory(CoordinateReferenceSystem crs) {
		this.crs = crs;
		SRID = CRSUtils.getSRID(crs);
	}
	
	/**
	 * @see {@link GraphFactory#createEdge()}
	 */
	public SampledSpatialSparseEdge createEdge() {
		return new SampledSpatialSparseEdge();
	}

	/**
	 * @see {@link GraphFactory#createGraph()}
	 */
	public SampledSpatialSparseGraph createGraph() {
		return new SampledSpatialSparseGraph(crs);
	}

	/**
	 * @throws {@link UnsupportedOperationException}. Use {@link #createVertex(Point)} instead.
	 */
	public SampledSpatialSparseVertex createVertex() {
		throw new UnsupportedOperationException();
	}
	
	/**
	 * Creates a new undetected and unsampled vertex located at <tt>point</tt>. The spatial reference id
	 * is the to the one of the factory's coordinate reference system.
	 * 
	 * @param point
	 *            the point in space where the vertex is located.
	 * @return a new spatial vertex.
	 */
	public SampledSpatialSparseVertex createVertex(Point point) {
		point.setSRID(SRID);
		return new SampledSpatialSparseVertex(point);
	}

}
