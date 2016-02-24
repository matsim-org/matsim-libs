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

package playground.michalm.poznan.zone;

import java.util.*;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.zone.*;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.MultiPolygon;


public class PoznanZones
{
    public static Map<Id<Zone>, Zone> readVisumZones()
    {
        String zonesXmlFile = "d:/eTaxi/Poznan_MATSim/zones.xml";
        String zonesShpFile = "d:/eTaxi/Poznan_MATSim/GIS/zones.shp";
        return Zones.readZones(zonesXmlFile, zonesShpFile);
    }


    public static Map<Id<Zone>, Zone> readTaxiZones()
    {
        String zonesXmlFile = "d:/PP-rad/taxi/poznan-supply/dane/rejony/taxi_zones.xml";
        String zonesShpFile = "d:/PP-rad/taxi/poznan-supply/dane/rejony/taxi_zones.shp";
        return Zones.readZones(zonesXmlFile, zonesShpFile);
    }


    public static MultiPolygon readAgglomerationArea()
    {
        String agglomerationShpFile = "d:/eTaxi/Poznan_MATSim/GIS/agglomeration.shp";

        Collection<SimpleFeature> features = ShapeFileReader.getAllFeatures(agglomerationShpFile);
        if (features.size() != 1) {
            throw new RuntimeException();
        }

        return (MultiPolygon)features.iterator().next().getDefaultGeometry();
    }
}
