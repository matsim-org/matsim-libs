/* *********************************************************************** *
 * project: org.matsim.*
 * SpatialEdge.java
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
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.GeodeticCalculator;
import org.matsim.contrib.common.gis.CRSUtils;
import org.matsim.contrib.socnetgen.sna.graph.SparseEdge;
import org.matsim.contrib.socnetgen.sna.graph.Vertex;
import org.matsim.core.utils.collections.Tuple;
import org.opengis.referencing.operation.TransformException;


/**
 * Implementation of {@link SpatialEdge} following the definitions of {@link SparseEdge}.
 * 
 * @author illenberger
 *
 */
public class SpatialSparseEdge extends SparseEdge implements SpatialEdge {

	/**
	 * Returns the orthodromic distance (great-circle distance) between both
	 * vertices. This method is rather slow. For performance critical tasks you
	 * might want to use {@link GeodeticCalculator} directly.
	 * 
	 * @return the orthodromic distance (great-circle distance) distance between
	 *         both vertices or <tt>Double.NaN</tt> if an error during
	 *         transformation of coordinate systems occurred.
	 */
	public double length() {
		Point p1 = getVertices().getFirst().getPoint();
		Point p2 = getVertices().getSecond().getPoint();

		if(p1 == null || p2 == null)
			return Double.NaN;
		
		if(p1.getSRID() == p2.getSRID()) {
			try {
				return JTS.orthodromicDistance(p1.getCoordinate(), p2.getCoordinate(), CRSUtils.getCRS(p1.getSRID()));
			} catch (TransformException e) {
				e.printStackTrace();
				return Double.NaN;
			}			
		} else {
			throw new RuntimeException("Incompatible coordinate reference systems.");
		}

	}
	
	/**
	 * @see {@link SparseEdge#getOpposite(Vertex)}
	 */
	@Override
	public SpatialSparseVertex getOpposite(Vertex v) {
		return (SpatialSparseVertex) super.getOpposite(v);
	}

	/**
	 * @see {@link SparseEdge#getVertices()}
	 */
	@SuppressWarnings("unchecked")
	@Override
	public Tuple<? extends SpatialSparseVertex, ? extends SpatialSparseVertex> getVertices() {
		return (Tuple<? extends SpatialSparseVertex, ? extends SpatialSparseVertex>) super.getVertices();
	}

}
