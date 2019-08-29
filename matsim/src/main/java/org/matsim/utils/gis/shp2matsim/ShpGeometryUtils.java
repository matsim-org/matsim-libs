/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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

package org.matsim.utils.gis.shp2matsim;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

public class ShpGeometryUtils {
	
	public static List<Geometry> loadShapeFile(String shapeFile) {
		List<Geometry> geometries = new ArrayList<>();

		Collection<SimpleFeature> features = null;
		if (new File(shapeFile).exists()) {
			features = ShapeFileReader.getAllFeatures(shapeFile);	
		} else {
			try {
				features = ShapeFileReader.getAllFeatures(new URL(shapeFile));
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
		}
		
		if (features == null) throw new RuntimeException("Aborting...");
		
		for (SimpleFeature feature : features) {
			geometries.add( (Geometry) feature.getDefaultGeometry() );
		}
		return geometries;
	}

	public static boolean isCoordInGeometries( Coord coord, List<Geometry> geometries ) {
		Point p = MGC.coord2Point(coord);
		
		for (Geometry geometry : geometries) {
			if (p.within(geometry)) {
				return true;
			}
		}
		return false;
	}

}
