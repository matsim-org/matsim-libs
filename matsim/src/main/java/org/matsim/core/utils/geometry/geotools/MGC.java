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

package org.matsim.core.utils.geometry.geotools;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

/**
 * Converter factory for various conversion from Geotools to MATSim and vice versa.
 *
 * @author laemmel
 *
 */
public class MGC {

	private final static Logger log = LogManager.getLogger(MGC.class);

	public static final GeometryFactory geoFac = new GeometryFactory();

	private final static Map<String, String> COORDINATE_REFERENCE_SYSTEMS = new HashMap<>();

	static {
		COORDINATE_REFERENCE_SYSTEMS.put(TransformationFactory.WGS84,
				"EPSG:4326");
		COORDINATE_REFERENCE_SYSTEMS.put(TransformationFactory.WGS84_UTM47S,
				"PROJCS[\"WGS_1984_UTM_Zone_47S\",GEOGCS[\"GCS_WGS_1984\",DATUM[\"D_WGS_1984\",SPHEROID[\"WGS_1984\",6378137.0,298.257223563]],PRIMEM[\"Greenwich\",0.0],UNIT[\"Degree\",0.0174532925199433]],PROJECTION[\"Transverse_Mercator\"],PARAMETER[\"False_Easting\",500000.0],PARAMETER[\"False_Northing\",10000000.0],PARAMETER[\"Central_Meridian\",99.0],PARAMETER[\"Scale_Factor\",0.9996],PARAMETER[\"Latitude_Of_Origin\",0.0],UNIT[\"Meter\",1.0]]");
		COORDINATE_REFERENCE_SYSTEMS.put(TransformationFactory.WGS84_UTM35S, // south-africa
				"PROJCS[\"WGS_1984_UTM_Zone_35S\",GEOGCS[\"GCS_WGS_1984\",DATUM[\"D_WGS_1984\",SPHEROID[\"WGS_1984\",6378137,298.257223563]],PRIMEM[\"Greenwich\",0],UNIT[\"Degree\",0.017453292519943295]],PROJECTION[\"Transverse_Mercator\"],PARAMETER[\"latitude_of_origin\",0],PARAMETER[\"central_meridian\",27],PARAMETER[\"scale_factor\",0.9996],PARAMETER[\"false_easting\",500000],PARAMETER[\"false_northing\",10000000],UNIT[\"Meter\",1]]");
		COORDINATE_REFERENCE_SYSTEMS.put(TransformationFactory.WGS84_UTM36S, // South Africa (eThekwini)
				"PROJCS[\"WGS_1984_UTM_Zone_36S\",GEOGCS[\"GCS_WGS_1984\",DATUM[\"D_WGS_1984\",SPHEROID[\"WGS_1984\",6378137.0,298.257223563]],PRIMEM[\"Greenwich\",0.0],UNIT[\"Degree\",0.0174532925199433]],PROJECTION[\"Transverse_Mercator\"],PARAMETER[\"False_Easting\",500000.0],PARAMETER[\"False_Northing\",10000000.0],PARAMETER[\"Central_Meridian\",33.0],PARAMETER[\"Scale_Factor\",0.9996],PARAMETER[\"Latitude_Of_Origin\",0.0],UNIT[\"Meter\",1.0]]");
		COORDINATE_REFERENCE_SYSTEMS.put(TransformationFactory.WGS84_UTM33N, // berlin
				"PROJCS[\"UTM Zone 33, Northern Hemisphere\",GEOGCS[\"WGS 84\",DATUM[\"WGS_1984\",SPHEROID[\"WGS 84\",6378137,298.257223563,AUTHORITY[\"EPSG\",\"7030\"]],TOWGS84[0,0,0,0,0,0,0],AUTHORITY[\"EPSG\",\"6326\"]],PRIMEM[\"Greenwich\",0,AUTHORITY[\"EPSG\",\"8901\"]],UNIT[\"degree\",0.0174532925199433,AUTHORITY[\"EPSG\",\"9108\"]],AUTHORITY[\"EPSG\",\"4326\"]],PROJECTION[\"Transverse_Mercator\"],PARAMETER[\"latitude_of_origin\",0],PARAMETER[\"central_meridian\",15],PARAMETER[\"scale_factor\",0.9996],PARAMETER[\"false_easting\",500000],PARAMETER[\"false_northing\",0],UNIT[\"Meter\",1]]");
		COORDINATE_REFERENCE_SYSTEMS.put(TransformationFactory.DHDN_GK4, // Berlin
				"EPSG:31468");
		COORDINATE_REFERENCE_SYSTEMS.put(TransformationFactory.WGS84_UTM29N, // Coimbra, Portugal
				"PROJCS[\"WGS_1984_UTM_Zone_29N\",GEOGCS[\"GCS_WGS_1984\",DATUM[\"D_WGS_1984\",SPHEROID[\"WGS_1984\",6378137,298.257223563]],PRIMEM[\"Greenwich\",0],UNIT[\"Degree\",0.017453292519943295]],PROJECTION[\"Transverse_Mercator\"],PARAMETER[\"latitude_of_origin\",0],PARAMETER[\"central_meridian\",-9],PARAMETER[\"scale_factor\",0.9996],PARAMETER[\"false_easting\",500000],PARAMETER[\"false_northing\",0.0],UNIT[\"Meter\",1]]");
        COORDINATE_REFERENCE_SYSTEMS.put(TransformationFactory.WGS84_UTM31N, // Barcelona, Spain
                "PROJCS[\"WGS_1984_UTM_Zone_31N\",GEOGCS[\"GCS_WGS_1984\",DATUM[\"D_WGS_1984\",SPHEROID[\"WGS_1984\",6378137,298.257223563]],PRIMEM[\"Greenwich\",0],UNIT[\"Degree\",0.017453292519943295]],PROJECTION[\"Transverse_Mercator\"],PARAMETER[\"latitude_of_origin\",0],PARAMETER[\"central_meridian\",3],PARAMETER[\"scale_factor\",0.9996],PARAMETER[\"false_easting\",500000],PARAMETER[\"false_northing\",0],UNIT[\"Meter\",1]]");
		COORDINATE_REFERENCE_SYSTEMS.put(TransformationFactory.CH1903_LV03_GT, "PROJCS[\"Hotine_Oblique_Mercator_Azimuth_Center\",GEOGCS[\"Bessel" +
				"1841\",DATUM[\"D_unknown\",SPHEROID[\"bessel\",6377397.155,299.1528128]]" +
				",PRIMEM[\"Greenwich\",0],UNIT[\"Degree\",0.017453292519943295]],PROJECTION[" +
				"\"Hotine_Oblique_Mercator_Azimuth_Center\"],PARAMETER[\"latitude_of_center\",46.95240555555556]" +
				",PARAMETER[\"longitude_of_center\",7.439583333333333],PARAMETER[\"azimuth\",90],PARAMETER[" +
				"\"scale_factor\",1],PARAMETER[\"false_easting\",600000],PARAMETER[\"false_northing\",200000],UNIT[\"Meter\",1]]");
		COORDINATE_REFERENCE_SYSTEMS.put(TransformationFactory.CH1903_LV03_Plus_GT, "PROJCS[\"Hotine_Oblique_Mercator_Azimuth_Center\",GEOGCS[\"Bessel" +
				"1841\",DATUM[\"D_unknown\",SPHEROID[\"bessel\",6377397.155,299.1528128]]" +
				",PRIMEM[\"Greenwich\",0],UNIT[\"Degree\",0.017453292519943295]],PROJECTION[" +
				"\"Hotine_Oblique_Mercator_Azimuth_Center\"],PARAMETER[\"latitude_of_center\",46.95240555555556]" +
				",PARAMETER[\"longitude_of_center\",7.439583333333333],PARAMETER[\"azimuth\",90],PARAMETER[" +
				"\"scale_factor\",1],PARAMETER[\"false_easting\",2600000],PARAMETER[\"false_northing\",1200000],UNIT[\"Meter\",1]]");
		COORDINATE_REFERENCE_SYSTEMS.put(TransformationFactory.WGS84_Albers, // South Africa (Africa Albers equal area conic)
				"PROJCS[\"Africa_Albers_Equal_Area_Conic\",GEOGCS[\"GCS_WGS_1984\",DATUM[\"D_WGS_1984\",SPHEROID[\"WGS_1984\",6378137.0,298.257223563]],PRIMEM[\"Greenwich\",0.0],UNIT[\"Degree\",0.0174532925199433]],PROJECTION[\"Albers\"],PARAMETER[\"False_Easting\",0.0],PARAMETER[\"False_Northing\",0.0],PARAMETER[\"Central_Meridian\",25.0],PARAMETER[\"Standard_Parallel_1\",20.0],PARAMETER[\"Standard_Parallel_2\",-23.0],PARAMETER[\"Latitude_Of_Origin\",0.0],UNIT[\"Meter\",1.0]]");
		COORDINATE_REFERENCE_SYSTEMS.put(TransformationFactory.WGS84_SA_Albers, // South Africa (Adapted version of Africa Albers equal area conic)
				"PROJCS[\"South_Africa_Albers_Equal\",GEOGCS[\"GCS_WGS_1984\",DATUM[\"D_WGS_1984\",SPHEROID[\"WGS_1984\",6378137.0,298.257223563]],PRIMEM[\"Greenwich\",0.0],UNIT[\"Degree\",0.0174532925199433]],PROJECTION[\"Albers\"],PARAMETER[\"False_Easting\",0.0],PARAMETER[\"False_Northing\",0.0],PARAMETER[\"Central_Meridian\",24.0],PARAMETER[\"Standard_Parallel_1\",-18.0],PARAMETER[\"Standard_Parallel_2\",-32.0],PARAMETER[\"Latitude_Of_Origin\",0.0],UNIT[\"Meter\",1.0]]");
		COORDINATE_REFERENCE_SYSTEMS.put(TransformationFactory.HARTEBEESTHOEK94_LO19, // South Africa adaption of Transverse Mercator - 17 degrees
				"PROJCS[\"Transverse_Mercator\", GEOGCS[\"GCS_WGS_1984\", DATUM[\"D_Hartebeesthoek_1994\", SPHEROID[\"WGS_1984\", 6378137, 298.257223563]], PRIMEM[\"Greenwich\", 0], UNIT[\"Degree\", 0.017453292519943295]], PROJECTION[\"Transverse_Mercator\"], PARAMETER[\"latitude_of_origin\", 0], PARAMETER[\"central_meridian\", 19], PARAMETER[\"scale_factor\", 1], PARAMETER[\"false_easting\", 0], PARAMETER[\"false_northing\", 0], UNIT[\"Meter\", 1]]");
		COORDINATE_REFERENCE_SYSTEMS.put(TransformationFactory.HARTEBEESTHOEK94_LO25, // South Africa adaption of Transverse Mercator - 25 degrees
				"PROJCS[\"Transverse_Mercator\", GEOGCS[\"GCS_WGS_1984\", DATUM[\"D_Hartebeesthoek_1994\", SPHEROID[\"WGS_1984\", 6378137, 298.257223563]], PRIMEM[\"Greenwich\", 0], UNIT[\"Degree\", 0.017453292519943295]], PROJECTION[\"Transverse_Mercator\"], PARAMETER[\"latitude_of_origin\", 0], PARAMETER[\"central_meridian\", 25], PARAMETER[\"scale_factor\", 1], PARAMETER[\"false_easting\", 0], PARAMETER[\"false_northing\", 0], UNIT[\"Meter\", 1]]");
		COORDINATE_REFERENCE_SYSTEMS.put(TransformationFactory.HARTEBEESTHOEK94_LO29, // South Africa adaption of Transverse Mercator - 29 degrees
				"PROJCS[\"Transverse_Mercator\", GEOGCS[\"GCS_WGS_1984\", DATUM[\"D_Hartebeesthoek_1994\", SPHEROID[\"WGS_1984\", 6378137, 298.257223563]], PRIMEM[\"Greenwich\", 0], UNIT[\"Degree\", 0.017453292519943295]], PROJECTION[\"Transverse_Mercator\"], PARAMETER[\"latitude_of_origin\", 0], PARAMETER[\"central_meridian\", 29], PARAMETER[\"scale_factor\", 1], PARAMETER[\"false_easting\", 0], PARAMETER[\"false_northing\", 0], UNIT[\"Meter\", 1]]");
		COORDINATE_REFERENCE_SYSTEMS.put(TransformationFactory.HARTEBEESTHOEK94_LO31, // South Africa adaption of Transverse Mercator - 31 degrees
				"PROJCS[\"Transverse_Mercator\", GEOGCS[\"GCS_WGS_1984\", DATUM[\"D_Hartebeesthoek_1994\", SPHEROID[\"WGS_1984\", 6378137, 298.257223563]], PRIMEM[\"Greenwich\", 0], UNIT[\"Degree\", 0.017453292519943295]], PROJECTION[\"Transverse_Mercator\"], PARAMETER[\"latitude_of_origin\", 0], PARAMETER[\"central_meridian\", 31], PARAMETER[\"scale_factor\", 1], PARAMETER[\"false_easting\", 0], PARAMETER[\"false_northing\", 0], UNIT[\"Meter\", 1]]");
		COORDINATE_REFERENCE_SYSTEMS.put(TransformationFactory.WGS84_UTM48N, // Singapore
				"PROJCS[\"WGS_1984_UTM_Zone_48N\",GEOGCS[\"GCS_WGS_1984\",DATUM[\"D_WGS_1984\",SPHEROID[\"WGS_1984\",6378137.0,298.257223563]],PRIMEM[\"Greenwich\",0.0],UNIT[\"Degree\",0.0174532925199433]],PROJECTION[\"Transverse_Mercator\"],PARAMETER[\"False_Easting\",500000.0],PARAMETER[\"False_Northing\",0.0],PARAMETER[\"Central_Meridian\",105.0],PARAMETER[\"Scale_Factor\",0.9996],PARAMETER[\"Latitude_Of_Origin\",0.0],UNIT[\"Meter\",1.0]]");
		COORDINATE_REFERENCE_SYSTEMS.put(TransformationFactory.WGS84_SVY21, // Singapore2
				"PROJCS[\"SVY21\",GEOGCS[\"GCS_WGS_1984\",DATUM[\"D_WGS_1984\",SPHEROID[\"WGS_1984\",6378137.0,298.257223563]],PRIMEM[\"Greenwich\",0.0],UNIT[\"Degree\",0.0174532925199433]],PROJECTION[\"Transverse_Mercator\"],PARAMETER[\"False_Easting\",28001.642],PARAMETER[\"False_Northing\",38744.572],PARAMETER[\"Central_Meridian\",103.8333333333333],PARAMETER[\"Scale_Factor\",1.0],PARAMETER[\"Latitude_Of_Origin\",1.366666666666667],UNIT[\"Meter\",1.0]]");
		COORDINATE_REFERENCE_SYSTEMS.put(TransformationFactory.NAD83_UTM17N, // Toronto, Canada - UTM_NAD1983_Zone17N
				"PROJCS[\"NAD_1983_UTM_Zone_17N\",GEOGCS[\"GCS_North_American_1983\",DATUM[\"D_North_American_1983\",SPHEROID[\"GRS_1980\",6378137.0,298.257222101]],PRIMEM[\"Greenwich\",0.0],UNIT[\"Degree\",0.0174532925199433]],PROJECTION[\"Transverse_Mercator\"],PARAMETER[\"False_Easting\",500000.0],PARAMETER[\"False_Northing\",0.0],PARAMETER[\"Central_Meridian\",-81.0],PARAMETER[\"Scale_Factor\",0.9996],PARAMETER[\"Latitude_Of_Origin\",0.0],UNIT[\"Meter\",1.0]]");
		COORDINATE_REFERENCE_SYSTEMS.put(TransformationFactory.WGS84_TM, //Singapore3
				"PROJCS[\"WGS_1984_Transverse_Mercator\",GEOGCS[\"GCS_WGS_1984\",DATUM[\"D_WGS_1984\",SPHEROID[\"WGS_1984\",6378137.0,298.257223563]],PRIMEM[\"Greenwich\",0.0],UNIT[\"Degree\",0.0174532925199433]],PROJECTION[\"Transverse_Mercator\"],PARAMETER[\"False_Easting\",28001.642],PARAMETER[\"False_Northing\",38744.572],PARAMETER[\"Central_Meridian\",103.8333333333333],PARAMETER[\"Scale_Factor\",1.0],PARAMETER[\"Latitude_Of_Origin\",1.366666666666667],UNIT[\"Meter\",1.0]]");
		COORDINATE_REFERENCE_SYSTEMS.put(TransformationFactory.PCS_ITRF2000_TM_UOS, // South Korea - but used by University of Seoul - probably a wrong one. !NEW!: Replaced by the correct one! TODO: probably needs to be renamed but since UOS use that already let's keep it.
				"PROJCS[\"Korean 1985 Katech(TM128)\",GEOGCS[\"GCS_Korean_Datum_1985\",DATUM[\"D_Korean_Datum_1985\",SPHEROID[\"Bessel_1841\",6377397.155,299.1528128]],PRIMEM[\"Greenwich\",0.0],UNIT[\"Degree\",0.0174532925199433]],PROJECTION[\"Transverse_Mercator\"],PARAMETER[\"False_Easting\",400000.0],PARAMETER[\"False_Northing\",600000.0],PARAMETER[\"Central_Meridian\",128.0],PARAMETER[\"Scale_Factor\",0.9999],PARAMETER[\"Latitude_Of_Origin\",38.0],UNIT[\"Meter\",1.0]]");
		COORDINATE_REFERENCE_SYSTEMS.put(TransformationFactory.DHDN_SoldnerBerlin, // Berlin
				"PROJCS[\"DHDN / Soldner Berlin\",GEOGCS[\"DHDN\",DATUM[\"Deutsches_Hauptdreiecksnetz\",SPHEROID[\"Bessel 1841\",6377397.155,299.1528128,AUTHORITY[\"EPSG\",\"7004\"]],AUTHORITY[\"EPSG\",\"6314\"]],PRIMEM[\"Greenwich\",0,AUTHORITY[\"EPSG\",\"8901\"]],UNIT[\"degree\",0.01745329251994328,AUTHORITY[\"EPSG\",\"9122\"]],AUTHORITY[\"EPSG\",\"4314\"]],UNIT[\"metre\",1,AUTHORITY[\"EPSG\",\"9001\"]],PROJECTION[\"Cassini_Soldner\"],PARAMETER[\"latitude_of_origin\",52.41864827777778],PARAMETER[\"central_meridian\",13.62720366666667],PARAMETER[\"false_easting\",40000],PARAMETER[\"false_northing\",10000],AUTHORITY[\"EPSG\",\"3068\"],AXIS[\"y\",EAST],AXIS[\"x\",NORTH]]");
		COORDINATE_REFERENCE_SYSTEMS.put(TransformationFactory.KROVAK, // Krovak EPSG5514
				"PROJCS[\"Czech GIS S-JTSK (Greenwich) / Krovak\",GEOGCS[\"Czech S-JTSK (Greenwich)\",DATUM[\"Czech S-JTSK\",SPHEROID[\"Bessel 1841\",6377397.155,299.1528128,AUTHORITY[\"EPSG\",\"7004\"]],TOWGS84[570.8,85.7,462.8,4.998,1.587,5.261,3.56]],PRIMEM[\"Greenwich\", 0.0, AUTHORITY[\"EPSG\",\"8901\"]],UNIT[\"degree\",0.0174532925199433]],PROJECTION[\"Krovak\",AUTHORITY[\"EPSG\",\"9819\"]],PARAMETER[\"latitude_of_center\",49.5],PARAMETER[\"longitude_of_center\",24.83333333333333],PARAMETER[\"azimuth\",0],PARAMETER[\"pseudo_standard_parallel_1\",0],PARAMETER[\"scale_factor\",0.9999],PARAMETER[\"false_easting\",0],PARAMETER[\"false_northing\",0],UNIT[\"Meter\",1],AXIS[\"X\",EAST],AXIS[\"Y\",NORTH],AUTHORITY[\"EPSG\",\"5514\"]]");
	}

	/**
	 * Converts a MATSim {@link org.matsim.api.core.v01.Coord} into a Geotools <code>Coordinate</code>
	 * @param coord MATSim coordinate
	 * @return Geotools coordinate
	 */
	public static Coordinate coord2Coordinate(final Coord coord) {
		return new Coordinate(coord.getX(), coord.getY());
	}

	/**
	 * Converts a Geotools <code>Coordinate</code> into a MATSim {@link org.matsim.api.core.v01.Coord}
	 * @param coord MATSim coordinate
	 * @return Geotools coordinate
	 */
	public static Coord coordinate2Coord(final Coordinate coord) {
		return new Coord(coord.x, coord.y);
	}

	/**
	 * Converts a MATSim {@link org.matsim.api.core.v01.Coord} into a Geotools <code>Point</code>
	 * @param coord MATSim coordinate
	 * @return Geotools point
	 */
	public static Point coord2Point(final Coord coord) {
		return geoFac.createPoint(coord2Coordinate(coord));
	}

	/**
	 * Converts a Geotools coordinate into a <code>Point</code>
	 * @return Geotools point
	 */
	public static Point coordinate2Point(Coordinate coordinate) {
		return geoFac.createPoint(coordinate);
	}

	/**
	 * Converts a Geotools <code>Point</code> into a MATSim {@link org.matsim.api.core.v01.Coord}
	 * @param point Geotools point
	 * @return MATSim coordinate
	 */
	public static Coord point2Coord(final Point point) {
		return new Coord(point.getX(), point.getY());
	}

	/**
	 * Converts x, y Coordinates to Geotools
	 * @param x
	 * @param y
	 * @return
	 */
	public static Point xy2Point(final double x, final double y) {
		return geoFac.createPoint(new Coordinate(x, y));
	}

	/**
	 * Generates a Geotools <code>CoordinateReferenceSystem</code> from a coordinate system <code>String</code>. The coordinate system
	 * can either be specified as shortened names, as defined in {@link TransformationFactory}, as EPSG Code or as
	 * Well-Known-Text (WKT) as supported by the GeoTools.
	 * @param wktOrAuthorityCodeOrShorthandName
	 * @return crs
	 */
	public static CoordinateReferenceSystem getCRS(final String wktOrAuthorityCodeOrShorthandName) {
		String wktOrAuthorityCode = COORDINATE_REFERENCE_SYSTEMS.get(wktOrAuthorityCodeOrShorthandName);
		if (wktOrAuthorityCode == null) {
			wktOrAuthorityCode = wktOrAuthorityCodeOrShorthandName;
		}
		CoordinateReferenceSystem crs;
		try {
			crs = CRS.parseWKT(wktOrAuthorityCode);
		} catch (FactoryException fe) {
			try {
				log.warn("Assuming that coordinates are in longitude first notation, i.e. (longitude, latitude).");
				crs = CRS.decode(wktOrAuthorityCode, true);
			} catch (FactoryException e) {
				throw new IllegalArgumentException(e);
			}
		}
		return crs;
	}

	/**
	 * Guesses the  Universal Transverse Mercator (UTM) zone for a given WGS 84 coordinate, returns the corresponding
	 * EPSG code
	 * @param lon the longitude of the WGS 84 coordinate
	 * @param lat the latitude of the WGS 84 coordinate
	 * @return EPSG code
	 */
	public static String getUTMEPSGCodeForWGS84Coordinate(final double lon, final double lat) {
		int utmZone = (int) (Math.ceil((180+lon) / 6)+0.5);
		String epsgCode = null;
		if (lat > 0 ) { //northern hemisphere
		  epsgCode = "EPSG:" + (32600 + utmZone);
		} else { //southern hemisphere
		  epsgCode = "EPSG:" + (32700 + utmZone);
		}
		return epsgCode;
	}
}
