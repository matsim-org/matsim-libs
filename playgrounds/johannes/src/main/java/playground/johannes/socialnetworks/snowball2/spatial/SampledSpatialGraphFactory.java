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

import org.apache.log4j.Logger;
import org.matsim.contrib.sna.graph.GraphFactory;
import org.opengis.metadata.Identifier;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Point;


/**
 * @author illenberger
 *
 */
public class SampledSpatialGraphFactory implements GraphFactory<SampledSpatialSparseGraph, SampledSpatialSparseVertex, SampledSpatialSparseEdge>{

	private static final Logger logger = Logger.getLogger(SampledSpatialGraphFactory.class);
	
	private final CoordinateReferenceSystem crs;
	
	private final int SRID;
	
	public SampledSpatialGraphFactory(CoordinateReferenceSystem crs) {
		this.crs = crs;
		/*
		 * Randomly get one identifier.
		 */
		int code;
		Identifier identifier = (Identifier)(crs.getIdentifiers().iterator().next()); 
		if(identifier == null) {
			logger.warn("Coordinate reference system has no identifier. Setting SRID to 0.");
			code = 0;
		} else {
			code = Integer.parseInt(identifier.getCode());
		}
		SRID = code;
	}
	
	public SampledSpatialSparseEdge createEdge() {
		return new SampledSpatialSparseEdge();
	}

	public SampledSpatialSparseGraph createGraph() {
		return new SampledSpatialSparseGraph(crs);
	}

	public SampledSpatialSparseVertex createVertex() {
		throw new UnsupportedOperationException();
	}
	
	public SampledSpatialSparseVertex createVertex(Point point) {
		point.setSRID(SRID);
		return new SampledSpatialSparseVertex(point);
	}

}
