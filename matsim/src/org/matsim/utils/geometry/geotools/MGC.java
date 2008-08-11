/* *********************************************************************** *
 * project: org.matsim.*
 * MGC.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.utils.geometry.geotools;

import org.geotools.referencing.CRS;
import org.matsim.utils.geometry.Coord;
import org.matsim.utils.geometry.CoordImpl;
import org.matsim.utils.geometry.transformations.TransformationFactory;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.util.HashMap;
import java.util.Map;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

/**
 * Converter factory for various conversion from Geotools to MATSim and vice versa.
 *
 * @author laemmel
 *
 */
public class MGC {

	public static final GeometryFactory geoFac = new GeometryFactory();

	private final static Map<String, String> transformations = new HashMap<String, String>();

	static {
		transformations.put(TransformationFactory.WGS84,
		"GEOGCS[\"WGS84\", DATUM[\"WGS84\", SPHEROID[\"WGS84\", 6378137.0, 298.257223563]], PRIMEM[\"Greenwich\", 0.0], UNIT[\"degree\",0.017453292519943295], AXIS[\"Longitude\",EAST], AXIS[\"Latitude\",NORTH]]");
		transformations.put(TransformationFactory.WGS84_UTM47S,
		"PROJCS[\"WGS_1984_UTM_Zone_47S\",GEOGCS[\"GCS_WGS_1984\",DATUM[\"D_WGS_1984\",SPHEROID[\"WGS_1984\",6378137.0,298.257223563]],PRIMEM[\"Greenwich\",0.0],UNIT[\"Degree\",0.0174532925199433]],PROJECTION[\"Transverse_Mercator\"],PARAMETER[\"False_Easting\",500000.0],PARAMETER[\"False_Northing\",10000000.0],PARAMETER[\"Central_Meridian\",99.0],PARAMETER[\"Scale_Factor\",0.9996],PARAMETER[\"Latitude_Of_Origin\",0.0],UNIT[\"Meter\",1.0]]");
		transformations.put(TransformationFactory.WGS84_UTM35S, // south-africa
		"PROJCS[\"WGS_1984_UTM_Zone_35S\",GEOGCS[\"GCS_WGS_1984\",DATUM[\"D_WGS_1984\",SPHEROID[\"WGS_1984\",6378137,298.257223563]],PRIMEM[\"Greenwich\",0],UNIT[\"Degree\",0.017453292519943295]],PROJECTION[\"Transverse_Mercator\"],PARAMETER[\"latitude_of_origin\",0],PARAMETER[\"central_meridian\",27],PARAMETER[\"scale_factor\",0.9996],PARAMETER[\"false_easting\",500000],PARAMETER[\"false_northing\",10000000],UNIT[\"Meter\",1]]");
	}



	/**
	 * Converts a MATSim {@link org.matsim.utils.geometry.Coord} into a Geotools <code>Coordinate</code>
	 * @param coord MATSim coordinate
	 * @return Geotools coordinate
	 */
	public static final Coordinate coord2Coordinate(final Coord coord) {
		return new Coordinate(coord.getX(), coord.getY());
	}

	/**
	 * Converts a Geotools <code>Coordinate</code> into a MATSim {@link org.matsim.utils.geometry.Coord}
	 * @param coord MATSim coordinate
	 * @return Geotools coordinate
	 */
	public static final Coord coordinate2Coord(final Coordinate coord) {
		return new CoordImpl(coord.x, coord.y);
	}

	/**
	 * Converts a MATSim {@link org.matsim.utils.geometry.Coord} into a Geotools <code>Point</code>
	 * @param coord MATSim coordinate
	 * @return Geotools point
	 */
	public static final Point coord2Point(final Coord coord) {
		return geoFac.createPoint(coord2Coordinate(coord));
	}

	/**
	 * Converts a Geotools <code>Point</code> into a MATSim {@link org.matsim.utils.geometry.Coord}
	 * @param point Geotools point
	 * @return MATSim coordinate
	 */
	public static final Coord point2Coord(final Point point) {
		return new CoordImpl(point.getX(), point.getY());
	}


	/**
	 * Generates a Geotools <code>CoordinateReferenceSystem</code> from a coordinate system <code>String</code>. The coordinate system 
	 * can either be specified as shortened names, as defined in {@link TransformationFactory}, or as
	 * Well-Known-Text (WKT) as supported by the GeoTools.
	 * @param crsString
	 * @return crs
	 */
	public static final CoordinateReferenceSystem getCRS(final String crsString) {
		String wkt_CRS = transformations.get(crsString);
		if (wkt_CRS == null) {
			wkt_CRS = crsString;
		}

		try {
			return CRS.parseWKT(wkt_CRS);
		} catch (FactoryException e) {
			throw new IllegalArgumentException(e);
		}
	}
}
