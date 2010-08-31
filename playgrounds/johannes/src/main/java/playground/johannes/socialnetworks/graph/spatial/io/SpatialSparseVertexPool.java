/* *********************************************************************** *
 * project: org.matsim.*
 * SpatialSparseVertexPool.java
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
package playground.johannes.socialnetworks.graph.spatial.io;

import java.util.LinkedList;
import java.util.Set;

import org.matsim.contrib.sna.graph.spatial.SpatialSparseGraphFactory;
import org.matsim.contrib.sna.graph.spatial.SpatialSparseVertex;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Point;

/**
 * @author jillenberger
 *
 */
public class SpatialSparseVertexPool extends SpatialSparseGraphFactory {

	private LinkedList<Point> points;
	
	/**
	 * @param crs
	 */
	public SpatialSparseVertexPool(Set<Point> points, CoordinateReferenceSystem crs) {
		super(crs);
		this.points = new LinkedList<Point>(points);
	}

	@Override
	public SpatialSparseVertex createVertex() {
		Point p = points.poll();
		if(p == null)
			return null;
		else
			return super.createVertex(p);
	}

}
