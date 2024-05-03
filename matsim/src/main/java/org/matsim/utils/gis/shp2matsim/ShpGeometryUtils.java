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
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygonal;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;
import org.locationtech.jts.geom.prep.PreparedPolygon;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.GeoFileReader;

public class ShpGeometryUtils {
	public static List<Geometry> loadGeometries(URL url) {
		return GeoFileReader.getAllFeatures(url)
				.stream()
				.map(sf -> (Geometry)sf.getDefaultGeometry())
				.collect(Collectors.toList());
	}

	public static List<PreparedGeometry> loadPreparedGeometries(URL url) {
		PreparedGeometryFactory factory = new PreparedGeometryFactory();
		return GeoFileReader.getAllFeatures(url)
				.stream()
				.map(sf -> factory.create((Geometry)sf.getDefaultGeometry()))
				.collect(Collectors.toList());
	}

	public static List<PreparedPolygon> loadPreparedPolygons(URL url) {
		return GeoFileReader.getAllFeatures(url)
			.stream()
			.map(sf -> new PreparedPolygon((Polygonal)sf.getDefaultGeometry()))
			.collect(Collectors.toList());
	}

	public static boolean isCoordInGeometries(Coord coord, List<Geometry> geometries) {
		Point point = MGC.coord2Point(coord);
		return geometries.stream().anyMatch(g -> g.contains(point));
	}

	public static boolean isCoordInPreparedGeometries(Coord coord, List<PreparedGeometry> geometries) {
		Point point = MGC.coord2Point(coord);
		return geometries.stream().anyMatch(g -> g.contains(point));
	}
}
