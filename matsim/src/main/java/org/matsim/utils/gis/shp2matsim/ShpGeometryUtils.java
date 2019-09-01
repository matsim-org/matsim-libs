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

import java.net.URL;
import java.util.List;
import java.util.stream.Collectors;

import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;

public class ShpGeometryUtils {
	public static List<Geometry> loadGemetries(URL url) {
		return ShapeFileReader.getAllFeatures(url)
				.stream()
				.map(sf -> (Geometry)sf.getDefaultGeometry())
				.collect(Collectors.toList());
	}

	public static boolean isCoordInGeometries(Coord coord, List<Geometry> geometries) {
		return geometries.stream().anyMatch(MGC.coord2Point(coord)::within);
	}
}
