/* *********************************************************************** *
 * project: org.matsim.*
 * SpatialGraphMLHelper.java
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
package org.matsim.contrib.sna.graph.spatial.io;

import org.apache.log4j.Logger;
import org.matsim.contrib.sna.gis.CRSUtils;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.xml.sax.Attributes;


import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

/**
 * Utility-class for de- and encoding GraphML data.
 * 
 * @author illenberger
 *
 */
public class SpatialGraphML {
	
	private static final Logger logger = Logger.getLogger(SpatialGraphML.class);

	public static final String COORD_X_ATTR = "x";
	
	public static final String COORD_Y_ATTR = "y";
	
	public static final String SRID_ATTR = "srid";
	
	private static final GeometryFactory geometryFactory = new GeometryFactory();
	
	/**
	 * Creates a point out of the data in <tt>attrs</tt>.
	 * 
	 * @param attrs
	 *            attribute data which is expected to store xy-coordinate
	 *            information.
	 * 
	 * @return a new point.
	 */
	public static Point newPoint(Attributes attrs) {
		double x = Double.parseDouble(attrs.getValue(COORD_X_ATTR));
		double y = Double.parseDouble(attrs.getValue(COORD_Y_ATTR));
		
		return geometryFactory.createPoint(new Coordinate(x, y));
	}
	
	/**
	 * Retrieves the coordinate reference system from the information stored in
	 * <tt>attrs</tt>.
	 * 
	 * @param attrs
	 *            attribute data which is expected to store the SRID.
	 * 
	 * @return a coordinate reference system.
	 */
	public static CoordinateReferenceSystem newCRS(Attributes attrs) {
		String value = attrs.getValue(SRID_ATTR);
		if(value == null) {
			logger.warn("No SRID attribute specified. Assuming CH1903LV03.");
			value = "21781";
		}
		int code = Integer.parseInt(value);
		return CRSUtils.getCRS(code);
	}
}
