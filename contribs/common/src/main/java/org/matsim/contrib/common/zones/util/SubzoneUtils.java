/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package org.matsim.contrib.common.zones.util;

import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.geometry.jts.GeometryCollector;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.TopologyException;
import org.locationtech.jts.geom.prep.PreparedPolygon;
import org.locationtech.jts.geom.util.PolygonExtracter;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.common.zones.Zone;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SubzoneUtils {
	public static Map<Id<Zone>, List<Polygon>> extractSubzonePolygons(Map<Id<Zone>, Zone> zones,
																	  Collection<SimpleFeature> subzonePattern) {
		Map<Id<Zone>, List<Polygon>> polygonsByZone = new HashMap<>();
		int topologyExceptionCount = 0;

		for (Zone z : zones.values()) {
			PreparedPolygon zonePoly = z.getPreparedGeometry();
			GeometryCollector geometryCollector = new GeometryCollector();

			for (SimpleFeature f : subzonePattern) {
				Geometry featureGeometry = (Geometry)f.getDefaultGeometry();

				try {
					geometryCollector.add(zonePoly.getGeometry().intersection(featureGeometry));
				} catch (TopologyException e) {
					topologyExceptionCount++;
				}
			}

			@SuppressWarnings("unchecked")
			List<Polygon> polygons = PolygonExtracter.getPolygons(geometryCollector.collect());
			polygonsByZone.put(z.getId(), polygons);
		}

		// TODO check out if geometries overlay one another!!!

		if (topologyExceptionCount > 0) {
			System.err.println(topologyExceptionCount + " ignored TopologyExceptions");
		}

		return polygonsByZone;
	}
}
