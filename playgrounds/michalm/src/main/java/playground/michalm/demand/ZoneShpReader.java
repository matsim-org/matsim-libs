/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

import java.io.IOException;
import java.util.*;

import org.matsim.api.core.v01.*;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;


public class ZoneShpReader
{
    public static void readZones(String file, String idField, Scenario scenario, Map<Id, Zone> zones)
        throws IOException
    {
        Collection<SimpleFeature> features = ShapeFileReader.getAllFeatures(file);

        if (features.size() != zones.size()) {
            throw new RuntimeException("Features: " + features.size() + "; zones: " + zones.size());
        }

        for (SimpleFeature ft : features) {
            String id = ft.getAttribute(idField).toString();
            Zone z = zones.get(scenario.createId(id));
            z.setZonePolygon(ft);
        }
    }
}
