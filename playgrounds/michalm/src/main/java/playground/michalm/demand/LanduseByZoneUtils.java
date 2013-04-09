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

package playground.michalm.demand;

import java.util.*;

import org.matsim.api.core.v01.Id;
import org.opengis.feature.simple.SimpleFeature;

import pl.poznan.put.util.random.WeightedRandomSelection;

import com.vividsolutions.jts.geom.*;


public class LanduseByZoneUtils
{
    public static Map<Id, List<Geometry>> buildMap(Map<Id, Zone> zones,
            Collection<SimpleFeature> features)
    {
        Map<Id, List<Geometry>> geometriesByZone = new LinkedHashMap<Id, List<Geometry>>();

        for (Zone z : zones.values()) {
            Geometry zg = (Geometry)z.getZonePolygon().getDefaultGeometry();
            List<Geometry> gList = new ArrayList<Geometry>();

            for (SimpleFeature f : features) {
                Geometry fg = (Geometry)f.getDefaultGeometry();

                Geometry intersection = null;
                try {
                    intersection = zg.intersection(fg);
                }
                catch (TopologyException e) {}

                if (intersection != null) {
                    addGeometries(gList, intersection);
                }
            }

            geometriesByZone.put(z.getId(), gList);
        }

        //TODO check out if geometries overlay one another!!!
        
        return geometriesByZone;
    }


    private static void addGeometries(List<Geometry> gList, Geometry g)
    {
        if (g instanceof GeometryCollection) {
            GeometryCollection gc = (GeometryCollection)g;
            for (int i = 0; i < gc.getNumGeometries(); i++) {
                addGeometries(gList, gc.getGeometryN(i));
            }
        }
        else {
            gList.add(g);
        }
    }


    public static WeightedRandomSelection<Geometry> buildRandomSelectionByArea(
            List<Geometry> geometries)
    {
        WeightedRandomSelection<Geometry> randomSelection = new WeightedRandomSelection<Geometry>();

        for (Geometry g : geometries) {
            randomSelection.add(g, g.getArea());
        }

        return randomSelection;
    }
}
