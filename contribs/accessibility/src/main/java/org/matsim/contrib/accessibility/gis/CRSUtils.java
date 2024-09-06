/* *********************************************************************** *
 * project: org.matsim.*
 * CRSUtils.java
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
package org.matsim.contrib.accessibility.gis;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.geotools.api.metadata.Identifier;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.NoSuchAuthorityCodeException;
import org.geotools.api.referencing.crs.CRSAuthorityFactory;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.operation.MathTransform;
import org.geotools.api.referencing.operation.TransformException;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;

/**
 * Utility-class providing functionality related to coordinate reference
 * systems.
 * 
 * @author illenberger
 * 
 */
final class CRSUtils {

	private static final Logger logger = LogManager.getLogger(CRSUtils.class);

	private static final Map<Integer, CoordinateReferenceSystem> crsMappings = new ConcurrentHashMap<Integer, CoordinateReferenceSystem>();

	private static GeometryFactory geoFactory;

	/**
	 * Retrieves the coordinate reference system from the EPSG database.
	 * 
	 * @param srid
	 *            the spatial reference id.
	 * 
	 * @return a coordinate reference system.
	 */
	public static CoordinateReferenceSystem getCRS(int srid) {
		CoordinateReferenceSystem crs = crsMappings.get(srid);
		if (crs == null) {
			/*
			 * Force longitude/latitude order.
			 */
			CRSAuthorityFactory factory = CRS.getAuthorityFactory(true);
			try {
				crs = factory.createCoordinateReferenceSystem("EPSG:" + srid);
			} catch (NoSuchAuthorityCodeException e) {
				logger.warn(e.getLocalizedMessage());
			} catch (FactoryException e) {
				e.printStackTrace();
			}
		}

		return crs;
	}

	/**
	 * Returns the spatial reference id for a given coordinate reference system.
	 * If the coordinate reference system has multiple identifiers one is
	 * randomly selected.
	 * 
	 * @param crs
	 *            a coordinate reference system.
	 * 
	 * @return the spatial reference id for the coordinate reference system or
	 *         <tt>0</tt> if the coordinate reference system has no identifiers.
	 */
	public static int getSRID(CoordinateReferenceSystem crs) {
		/*
		 * Randomly get one identifier.
		 */
		Identifier identifier = crs.getIdentifiers().iterator().next();
		if (identifier == null) {
			return 0;
		} else {
			return Integer.parseInt(identifier.getCode());
		}
	}

	/**
	 * Determines the transformation from the coordinate reference system of
	 * <tt>source</tt> to the one of <tt>target</tt>.
	 * 
	 * @param source
	 *            a geometry.
	 * @param target
	 *            a geometry.
	 * @return a transformation or <tt>null</tt> if the transformation could not
	 *         be determined.
	 */
	public static MathTransform findTransform(Geometry source, Geometry target) {
		CoordinateReferenceSystem sourceCRS = getCRS(source.getSRID());
		CoordinateReferenceSystem targetCRS = getCRS(target.getSRID());

		try {
            return CRS.findMathTransform(sourceCRS, targetCRS);
		} catch (FactoryException e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Creates a new point that is a transformed copy of the original point.
	 * 
	 * @param point
	 *            a the original point.
	 * @param transform
	 *            the transformation to be applied.
	 * @return a new transformed point.
	 */
	public static Point transformPoint(Point point, MathTransform transform) {
		if (geoFactory == null)
			geoFactory = new GeometryFactory();

		double[] points = new double[] { point.getCoordinate().x, point.getCoordinate().y };
		try {
			transform.transform(points, 0, points, 0, 1);
		} catch (TransformException e) {
			e.printStackTrace();
		}
		Point p = geoFactory.createPoint(new Coordinate(points[0], points[1]));
		return p;
	}
}
