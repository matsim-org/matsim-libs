/* *********************************************************************** *
 * project: org.matsim.*
 * SpatialGraph.java
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

import org.geotools.referencing.CRS;
import org.matsim.api.core.v01.Coord;
import org.matsim.contrib.common.gis.CRSUtils;
import org.matsim.contrib.socnetgen.sna.graph.SparseGraph;
import org.matsim.contrib.socnetgen.sna.graph.SparseVertex;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import java.util.Set;


/**
 * Implementation of {@link SpatialGraph} following the definitions of {@link SparseGraph}.
 * 
 * @author illenberger
 *
 */
public class SpatialSparseGraph extends SparseGraph implements SpatialGraph {

	private CoordinateReferenceSystem crs;
	
	/**
	 * Creates a new spatial sparse graph with the given coordinate reference
	 * system.
	 * 
	 * @param crs
	 *            a coordinate reference system.
	 */
	public SpatialSparseGraph(CoordinateReferenceSystem crs) {
		this.crs = crs;
	}
	
	/**
	 * @see {@link SparseGraph#getEdges()}
	 */
	@SuppressWarnings("unchecked")
	@Override
	public Set<? extends SpatialSparseEdge> getEdges() {
		return (Set<? extends SpatialSparseEdge>) super.getEdges();
	}

	/**
	 * @see {@link SparseGraph#getVertices()}
	 */
	@SuppressWarnings("unchecked")
	@Override
	public Set<? extends SpatialSparseVertex> getVertices() {
		return (Set<? extends SpatialSparseVertex>) super.getVertices();
	}

	/**
	 * @see {@link SparseGraph#getEdge(SparseVertex, SparseVertex)}
	 */
	@Override
	public SpatialSparseEdge getEdge(SparseVertex v_i, SparseVertex v_j) {
		return (SpatialSparseEdge) super.getEdge(v_i, v_j);
	}

	/**
	 * @deprecated will be replaced by something link getEnvelope() from geotools.
	 * @return
	 */
	public double[] getBounds() {
		double[] bounds = new double[4];
		
		double xmin = Double.MAX_VALUE;
		double ymin = Double.MAX_VALUE;
		double xmax = - Double.MAX_VALUE;
		double ymax = - Double.MAX_VALUE;
		
		for(SpatialSparseVertex v : getVertices()) {
			Coord c = v.getCoordinate();
			xmin = Math.min(xmin, c.getX());
			ymin = Math.min(ymin, c.getY());
			xmax = Math.max(xmax, c.getX());
			ymax = Math.max(ymax, c.getY());
		}
		
		bounds[0] = xmin;
		bounds[1] = ymin;
		bounds[2] = xmax;
		bounds[3] = ymax;
		
		return bounds;
	}

	/**
	 * @see {@link SpatialGraph#getCoordinateReferenceSysten()}
	 */
	@Override
	public CoordinateReferenceSystem getCoordinateReferenceSysten() {
		return crs;
	}
	
	/**
	 * Transforms the graph to the given coordinate reference system.
	 * 
	 * @param newCRS the new coordinate reference system.
	 */
	public void transformToCRS(CoordinateReferenceSystem newCRS) {
		try {
			MathTransform transform = CRS.findMathTransform(crs, newCRS);
			int srid = CRSUtils.getSRID(newCRS);
			if(srid == 0) {
				throw new RuntimeException("Cannot obtain SRID form coordinate reference system.");
			}
			
			for (SpatialVertex vertex : getVertices()) {
				if(vertex.getPoint() != null) {
				double[] points = new double[] { vertex.getPoint().getCoordinate().x,
						vertex.getPoint().getCoordinate().y };
				transform.transform(points, 0, points, 0, 1);
				vertex.getPoint().getCoordinate().x = points[0];
				vertex.getPoint().getCoordinate().y = points[1];
				vertex.getPoint().setSRID(srid);
			}
			}
			
			crs = newCRS;
		} catch (FactoryException e) {
			e.printStackTrace();
		} catch (TransformException e) {
			e.printStackTrace();
			/*
			 * Graph is probably in an inconsistent state. Better exit.
			 */
			System.exit(-1);
		}
	}
}
