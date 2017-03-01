/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.michalm.barcelona;

import java.util.Collection;

import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.MultiPolygon;

public class BarcelonaZones {
	public static MultiPolygon readAgglomerationArea() {
		String agglomerationShpFile = "d:/PP-rad/Barcelona/data/GIS/BCN_polygon_UMT31N.shp";

		Collection<SimpleFeature> features = ShapeFileReader.getAllFeatures(agglomerationShpFile);
		if (features.size() != 1) {
			throw new RuntimeException();
		}

		return (MultiPolygon)features.iterator().next().getDefaultGeometry();
	}
}
