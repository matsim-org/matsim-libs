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

package playground.michalm.zone.util;

import java.util.*;

import org.matsim.api.core.v01.Id;
import org.opengis.feature.simple.SimpleFeature;

import playground.michalm.zone.Zone;

import com.vividsolutions.jts.geom.*;


public class PolygonsByZoneUtils
{
    public static Map<Id, List<Polygon>> buildMap(Map<Id, Zone> zones,
            Collection<SimpleFeature> features)
    {
        Map<Id, List<Polygon>> polygonsByZone = new HashMap<Id, List<Polygon>>();

        for (Zone z : zones.values()) {
            Polygon zonePoly = z.getPolygon();
            List<Polygon> polyList = new ArrayList<Polygon>();

            for (SimpleFeature f : features) {
                Geometry featureGeometry = (Geometry)f.getDefaultGeometry();

                Geometry intersection = null;
                try {
                    intersection = zonePoly.intersection(featureGeometry);
                }
                catch (TopologyException e) {
                    System.err.println(e);
                }

                if (intersection != null) {
                    addPolygons(polyList, intersection);
                }
            }

            polygonsByZone.put(z.getId(), polyList);
        }

        //TODO check out if geometries overlay one another!!!

        return polygonsByZone;
    }


    private static void addPolygons(List<Polygon> polygonList, Geometry intersection)
    {
        if (intersection instanceof GeometryCollection) {
            GeometryCollection gc = (GeometryCollection)intersection;
            for (int i = 0; i < gc.getNumGeometries(); i++) {
                addPolygons(polygonList, gc.getGeometryN(i));
            }
        }
        else {
            polygonList.add((Polygon)intersection);
        }
    }
}
