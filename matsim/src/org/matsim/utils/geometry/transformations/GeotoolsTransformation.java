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

import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.matsim.utils.geometry.CoordI;
import org.matsim.utils.geometry.CoordinateTransformationI;
import org.matsim.utils.geometry.MGC;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.opengis.spatialschema.geometry.MismatchedDimensionException;


import com.vividsolutions.jts.geom.Point;



/**
 * a transformation factory for various coordinate systems using the geotools  
 * 
 * @author laemmel
 *
 */
public class GeotoolsTransformation implements CoordinateTransformationI {
	
	private final static String WGS84 = "GEOGCS[\"WGS84\", DATUM[\"WGS84\", SPHEROID[\"WGS84\", 6378137.0, 298.257223563]], PRIMEM[\"Greenwich\", 0.0], UNIT[\"degree\",0.017453292519943295], AXIS[\"Longitude\",EAST], AXIS[\"Latitude\",NORTH]]";
	private final static String WGS84_UTM47S = "PROJCS[\"WGS_1984_UTM_Zone_47S\",GEOGCS[\"GCS_WGS_1984\",DATUM[\"D_WGS_1984\",SPHEROID[\"WGS_1984\",6378137.0,298.257223563]],PRIMEM[\"Greenwich\",0.0],UNIT[\"Degree\",0.0174532925199433]],PROJECTION[\"Transverse_Mercator\"],PARAMETER[\"False_Easting\",500000.0],PARAMETER[\"False_Northing\",10000000.0],PARAMETER[\"Central_Meridian\",99.0],PARAMETER[\"Scale_Factor\",0.9996],PARAMETER[\"Latitude_Of_Origin\",0.0],UNIT[\"Meter\",1.0]]";
	
	private MathTransform transform;
	
	public GeotoolsTransformation(String from, String to) {
		CoordinateReferenceSystem sourceCRS = getCRS(from);
		CoordinateReferenceSystem targetCRS = getCRS(to);
		
		try {
			this.transform = CRS.findMathTransform(sourceCRS, targetCRS,true);
		} catch (FactoryException e) {
			e.printStackTrace();
			System.exit(-1);	
		}
	}

	/* (non-Javadoc)
	 * @see org.matsim.utils.geometry.CoordinateTransformationI#transform(org.matsim.utils.geometry.CoordI)
	 */
	public CoordI transform(CoordI coord) {
			
		Point p = null;
		try {
			p = (Point) JTS.transform(MGC.coord2Point(coord),transform);
		} catch (MismatchedDimensionException e) {
			e.printStackTrace();
			System.exit(-1);
		} catch (TransformException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		return MGC.point2Coord(p);
	}
	
	private CoordinateReferenceSystem getCRS(String crsString){
		String wkt_CRS;
		if (TransformationFactory.WGS84.equals(crsString)) {
			wkt_CRS = WGS84;
		} else if (TransformationFactory.WGS84_UTM47S.equals(crsString)) {
			wkt_CRS = WGS84_UTM47S;
		} else {
			throw new IllegalArgumentException("Coordinate system " + crsString + " is not known!");
		}
		
		CoordinateReferenceSystem crs = null;
		try {
			crs =  CRS.parseWKT(wkt_CRS);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
		return crs;
	}

}
