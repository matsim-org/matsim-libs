/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.johannes.gsv.gis;

import com.vividsolutions.jts.geom.Point;
import playground.johannes.gsv.zones.Zone;
import playground.johannes.gsv.zones.ZoneCollection;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

/**
 * @author johannes
 */
public class Zones2Centroids {

    public static void main(String args[]) throws IOException {
        ZoneCollection zones = ZoneCollection.readFromGeoJSON("/home/johannes/gsv/gis/modena/geojson/zones.geojson", "NO");

        BufferedWriter writer = new BufferedWriter(new FileWriter("/mnt/cifs/B-drive/U_Benutzer/JohannesIllenberger/qlik/centroids.csv"));
        writer.write("id,name,lng,lat");
        writer.newLine();
        for(Zone zone : zones.zoneSet()) {
            Point c = zone.getGeometry().getCentroid();
            String id = zone.getAttribute("NO");
            String name = zone.getAttribute("NAME");

            writer.write(id);
            writer.write(",");
            writer.write(name);
            writer.write(",");
            writer.write(String.valueOf(c.getX()));
            writer.write(",");
            writer.write(String.valueOf(c.getY()));
            writer.newLine();
        }

        writer.close();
    }
}
