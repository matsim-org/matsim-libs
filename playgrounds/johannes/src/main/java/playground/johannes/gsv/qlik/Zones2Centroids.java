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

package playground.johannes.gsv.qlik;

import com.vividsolutions.jts.geom.Point;
import playground.johannes.synpop.gis.Zone;
import playground.johannes.synpop.gis.ZoneCollection;
import playground.johannes.synpop.gis.ZoneGeoJsonIO;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

/**
 * @author johannes
 */
public class Zones2Centroids {

    private final static String SEPARATOR = ";";

    public static void main(String args[]) throws IOException {
        ZoneCollection zones = ZoneGeoJsonIO.readFromGeoJSON("/home/johannes/gsv/gis/nuts/world/nuts3-psmobility.geojson", "NO");

        BufferedWriter writer = new BufferedWriter(new FileWriter
                ("/mnt/cifs/B-drive/C_Vertrieb/2014_03_01_Nachfragematrizen_PV/07_Qlik/nuts3.csv"));
        writer.write("id;lng;lat;nuts3_name;nuts3_code;nuts2_name;nuts2_code;nuts1_name;nuts1_code;nuts0_name;" +
                        "nuts0_code");
        writer.newLine();

        for(Zone zone : zones.getZones()) {
            Point c = zone.getGeometry().getCentroid();
            String id = zone.getAttribute("NO");

            writer.write(id);
            writer.write(SEPARATOR);
            writer.write(String.valueOf(c.getX()));
            writer.write(SEPARATOR);
            writer.write(String.valueOf(c.getY()));

            writer.write(SEPARATOR);
            writer.write(zone.getAttribute("NUTS3_NAME"));
            writer.write(SEPARATOR);
            writer.write(zone.getAttribute("NUTS3_CODE"));
            writer.write(SEPARATOR);
            writer.write(zone.getAttribute("NUTS2_NAME"));
            writer.write(SEPARATOR);
            writer.write(zone.getAttribute("NUTS2_CODE"));
            writer.write(SEPARATOR);
            writer.write(zone.getAttribute("NUTS1_NAME"));
            writer.write(SEPARATOR);
            writer.write(zone.getAttribute("NUTS1_CODE"));
            writer.write(SEPARATOR);
            writer.write(zone.getAttribute("NUTS0_NAME"));
            writer.write(SEPARATOR);
            writer.write(zone.getAttribute("NUTS0_CODE"));

            writer.newLine();
        }

        writer.close();
    }
}
