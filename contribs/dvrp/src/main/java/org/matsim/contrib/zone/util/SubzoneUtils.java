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

package org.matsim.contrib.zone.util;

import java.util.*;

import org.geotools.geometry.jts.GeometryCollector;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.zone.Zone;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.geom.util.PolygonExtracter;


public class SubzoneUtils
{
    public static Map<Id<Zone>, List<Polygon>> extractSubzonePolygons(Map<Id<Zone>, Zone> zones,
            Collection<SimpleFeature> subzonePattern)
    {
        Map<Id<Zone>, List<Polygon>> polygonsByZone = new HashMap<>();
        int topologyExceptionCount = 0;

        for (Zone z : zones.values()) {
            MultiPolygon zoneMultiPoly = z.getMultiPolygon();
            GeometryCollector geometryCollector = new GeometryCollector();

            for (SimpleFeature f : subzonePattern) {
                Geometry featureGeometry = (Geometry)f.getDefaultGeometry();

                try {
                    geometryCollector.add(zoneMultiPoly.intersection(featureGeometry));
                }
                catch (TopologyException e) {
                    topologyExceptionCount++;
                }
            }

            @SuppressWarnings("unchecked")
            List<Polygon> polygons = PolygonExtracter.getPolygons(geometryCollector.collect());
            polygonsByZone.put(z.getId(), polygons);
        }

        //TODO check out if geometries overlay one another!!!

        if (topologyExceptionCount > 0) {
            System.err.println(topologyExceptionCount + " ignored TopologyExceptions");
        }

        return polygonsByZone;
    }
}
