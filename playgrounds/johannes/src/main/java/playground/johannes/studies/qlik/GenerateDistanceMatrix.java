/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.johannes.studies.qlik;

import org.matsim.contrib.common.gis.CartesianDistanceCalculator;
import org.matsim.contrib.common.gis.DistanceCalculator;
import org.matsim.contrib.common.util.ProgressLogger;
import playground.johannes.synpop.gis.Zone;
import playground.johannes.synpop.gis.ZoneCollection;
import playground.johannes.synpop.gis.ZoneGeoJsonIO;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author johannes
 */
public class GenerateDistanceMatrix {

    public static void main(String args[]) throws IOException {
        String zoneFile = "/Users/johannes/gsv/gis/zones/geojson/plz5.gk3.geojson";
        String zoneId = "plz";
        String outFile = "/Users/johannes/gsv/fpd/telefonica/032016/analysis/distances-plz5.txt";

        ZoneCollection zones = ZoneGeoJsonIO.readFromGeoJSON(zoneFile, zoneId, null);
        List<Zone> zoneList = new ArrayList<>(zones.getZones());

//        DistanceCalculator distanceCalculator = WGS84DistanceCalculator.getInstance();
        DistanceCalculator distanceCalculator = CartesianDistanceCalculator.getInstance();

        BufferedWriter writer = new BufferedWriter(new FileWriter(outFile));
        writer.write("from;to;distance");
        writer.newLine();

        ProgressLogger.init(zoneList.size(), 2, 10);

        for(int i = 0; i < zoneList.size(); i++) {
            for(int j = i; j < zoneList.size(); j++) {
                Zone z_i = zoneList.get(i);
                Zone z_j = zoneList.get(j);

                double d = distanceCalculator.distance(z_i.getGeometry().getCentroid(), z_j.getGeometry().getCentroid());

                writer.write(z_i.getAttribute(zoneId));
                writer.write(";");
                writer.write(z_j.getAttribute(zoneId));
                writer.write(";");
                writer.write(String.valueOf(d));
                writer.newLine();

                if(i != j) {
                    writer.write(z_j.getAttribute(zoneId));
                    writer.write(";");
                    writer.write(z_i.getAttribute(zoneId));
                    writer.write(";");
                    writer.write(String.valueOf(d));
                    writer.newLine();
                }
            }

            ProgressLogger.step();
        }

        ProgressLogger.terminate();

        writer.close();
    }
}
