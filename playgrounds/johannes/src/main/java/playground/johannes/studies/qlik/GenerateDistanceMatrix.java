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

import com.vividsolutions.jts.geom.Point;
import org.matsim.contrib.common.collections.CollectionUtils;
import org.matsim.contrib.common.gis.CartesianDistanceCalculator;
import org.matsim.contrib.common.gis.DistanceCalculator;
import org.matsim.contrib.common.util.ProgressLogger;
import org.matsim.core.utils.io.IOUtils;
import playground.johannes.synpop.gis.Zone;
import playground.johannes.synpop.gis.ZoneCollection;
import playground.johannes.synpop.gis.ZoneGeoJsonIO;
import playground.johannes.synpop.util.Executor;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author johannes
 */
public class GenerateDistanceMatrix {

    private static final String SEPARATOR = ";";

    public static void main(String args[]) throws IOException {
//        String zoneFile = "/Users/johannes/gsv/gis/zones/geojson/modena.geojson";
        String zoneFile = "/Users/johannes/gsv/gis/zones/geojson/nuts3.psm.airports.gk3.geojson";
        String zoneId = "NO";
//        String outFile = "/Volumes/GSV-2/C_Vertrieb/2016_04_25-0984-Nachfragematrix_FPD_Telefonica/QS/MIV/in/modenaDistances.txt";
        String outFile = "/Volumes/GSV-2/C_Vertrieb/2016_04_25-0984-Nachfragematrix_FPD_Telefonica/QS/MIV/in/kreisDistances.txt";

        ZoneCollection zones = ZoneGeoJsonIO.readFromGeoJSON(zoneFile, zoneId, null);
        List<Zone> zoneList = new ArrayList<>(zones.getZones());

//        DistanceCalculator distanceCalculator = WGS84DistanceCalculator.getInstance();
        DistanceCalculator distanceCalculator = CartesianDistanceCalculator.getInstance();

//        BufferedWriter writer = new BufferedWriter(new FileWriter(outFile));
        BufferedWriter writer = IOUtils.getBufferedWriter(outFile);
        writer.write("from;to;distance");
        writer.newLine();

        int numThreads = 1;//Executor.getFreePoolSize();
        List<Runnable> threads = new ArrayList<>(numThreads);
        List<Zone>[] segments = CollectionUtils.split(zoneList, numThreads);

        for(List<Zone> segment : segments) {
            threads.add(new RunThread(zoneList, segment, distanceCalculator, zoneId, writer));
        }
        ProgressLogger.init(zoneList.size(), 2, 10);
        Executor.submitAndWait(threads);



//        for(int i = 0; i < zoneList.size(); i++) {
//            for(int j = i; j < zoneList.size(); j++) {
//                Zone z_i = zoneList.get(i);
//                Zone z_j = zoneList.get(j);
//
//                double d = distanceCalculator.distance(z_i.getGeometry().getCentroid(), z_j.getGeometry().getCentroid());
//
//                writer.write(z_i.getAttribute(zoneId));
//                writer.write(";");
//                writer.write(z_j.getAttribute(zoneId));
//                writer.write(";");
//                writer.write(String.valueOf(d));
//                writer.newLine();
//
//                if(i != j) {
//                    writer.write(z_j.getAttribute(zoneId));
//                    writer.write(";");
//                    writer.write(z_i.getAttribute(zoneId));
//                    writer.write(";");
//                    writer.write(String.valueOf(d));
//                    writer.newLine();
//                }
//            }
//
//            ProgressLogger.step();
//        }

        ProgressLogger.terminate();

        writer.close();
    }

    private static class RunThread implements Runnable {

        private List<Zone> allZones;

        private List<Zone> pendingZones;

        private DistanceCalculator distanceCalculator;

        private String zoneId;

        private BufferedWriter writer;

        public RunThread(List<Zone> allZones, List<Zone> pendingZones, DistanceCalculator distanceCalculator, String zoneId, BufferedWriter writer) {
            this.allZones = allZones;
            this.pendingZones = pendingZones;
            this.distanceCalculator = distanceCalculator;
            this.zoneId = zoneId;
            this.writer = writer;
        }

        @Override
        public void run() {
            try {
                for (int i = 0; i < pendingZones.size(); i++) {
                    Zone z_i = pendingZones.get(i);
                    Point c_i = z_i.getGeometry().getCentroid();
                    String id_i = z_i.getAttribute(zoneId);

                    for (int j = 0; j < allZones.size(); j++) {
                        Zone z_j = allZones.get(j);
                        String id_j = z_j.getAttribute(zoneId);

                        double d = distanceCalculator.distance(c_i, z_j.getGeometry().getCentroid());
                        String dStr = String.valueOf(d);

                        writer.write(id_i);
                        writer.write(SEPARATOR);
                        writer.write(id_j);
                        writer.write(SEPARATOR);
                        writer.write(dStr);
                        writer.newLine();

                        if (i != j) {
                            writer.write(id_j);
                            writer.write(SEPARATOR);
                            writer.write(id_i);
                            writer.write(SEPARATOR);
                            writer.write(dStr);
                            writer.newLine();
                        }
                    }

                    ProgressLogger.step();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
