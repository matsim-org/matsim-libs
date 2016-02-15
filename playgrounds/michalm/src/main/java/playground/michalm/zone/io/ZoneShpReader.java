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

package playground.michalm.zone.io;

import java.util.*;

import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.MultiPolygon;

import playground.michalm.zone.Zone;


public class ZoneShpReader
{
    private final Map<Id<Zone>, Zone> zones;


    public ZoneShpReader(Map<Id<Zone>, Zone> zones)
    {
        this.zones = zones;
    }


    public void readZones(String file)
    {
        readZones(file, ZoneShpWriter.ID_HEADER);
    }


    public void readZones(String file, String idHeader)
    {
        Collection<SimpleFeature> features = ShapeFileReader.getAllFeatures(file);
        if (features.size() != zones.size()) {
            throw new RuntimeException(
                    "Features#: " + features.size() + "; zones#: " + zones.size());
        }

        for (SimpleFeature ft : features) {
            String id = ft.getAttribute(idHeader).toString();
            Zone z = zones.get(Id.create(id, Zone.class));
            z.setMultiPolygon((MultiPolygon)ft.getDefaultGeometry());
        }
    }
}
