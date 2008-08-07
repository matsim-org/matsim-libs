/* *********************************************************************** *
 * project: org.matsim.*
 * GeotoolsTransformation.java
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

package org.matsim.utils.geometry.transformations;

import java.util.HashMap;
import java.util.Map;

import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.matsim.utils.geometry.Coord;
import org.matsim.utils.geometry.CoordinateTransformation;
import org.matsim.utils.geometry.geotools.MGC;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.opengis.spatialschema.geometry.MismatchedDimensionException;

import com.vividsolutions.jts.geom.Point;

/**
 * A transformation factory for various coordinate systems using the GeoTools.
 *
 * @author laemmel
 */
public class GeotoolsTransformation implements CoordinateTransformation {

	private MathTransform transform;

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
	 * Creates a new coordinate transformation that makes use of GeoTools.
	 * The coordinate systems to translate from and to can either be specified as
	 * shortened names, as defined in {@link TransformationFactory}, or as
	 * Well-Known-Text (WKT) as supported by the GeoTools.
	 *
	 * @param from Specifies the origin coordinate reference system
	 * @param to Specifies the destination coordinate reference system
	 *
	 * @see <a href="http://geoapi.sourceforge.net/snapshot/javadoc/org/opengis/referencing/doc-files/WKT.html">WKT specifications</a>
	 */
	public GeotoolsTransformation(String from, String to) {
		CoordinateReferenceSystem sourceCRS = getCRS(from);
		CoordinateReferenceSystem targetCRS = getCRS(to);

		try {
			this.transform = CRS.findMathTransform(sourceCRS, targetCRS,true);
		} catch (FactoryException e) {
			throw new RuntimeException(e);
		}
	}

	public Coord transform(Coord coord) {
		Point p = null;
		try {
			p = (Point) JTS.transform(MGC.coord2Point(coord), transform);
		} catch (MismatchedDimensionException e) {
			throw new RuntimeException(e);
		} catch (TransformException e) {
			throw new RuntimeException(e);
		}
		return MGC.point2Coord(p);
	}

	private CoordinateReferenceSystem getCRS(String crsString) {
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
