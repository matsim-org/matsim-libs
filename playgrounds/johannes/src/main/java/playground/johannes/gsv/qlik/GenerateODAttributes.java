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

import com.vividsolutions.jts.algorithm.MinimumDiameter;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import org.apache.log4j.Logger;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.matsim.contrib.common.gis.DistanceCalculator;
import org.matsim.contrib.common.gis.WGS84DistanceCalculator;
import playground.johannes.gsv.zones.KeyMatrix;
import playground.johannes.synpop.gis.Zone;
import playground.johannes.synpop.gis.ZoneCollection;
import playground.johannes.synpop.gis.ZoneEsriShapeIO;

import java.io.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author johannes
 */
public class GenerateODAttributes {

    private static final Logger logger = Logger.getLogger(GenerateODAttributes.class);

    public static final void main(String args[]) throws IOException {
        String matrixFile1 = "/mnt/cifs/B-drive/C_Vertrieb/2014_03_01_Nachfragematrizen_PV/07_Qlik/MIV_Flug_Kreis_2013.csv";
        String matrixFile2 = "/mnt/cifs/B-drive/C_Vertrieb/2014_03_01_Nachfragematrizen_PV/07_Qlik/Bahn_Kreis_2013.csv";
        String zonesFile = "/home/johannes/gsv/gis/nuts/world/psmobility.shp";
        String outFile = "/mnt/cifs/B-drive/C_Vertrieb/2014_03_01_Nachfragematrizen_PV/07_Qlik/odAttributes-kreis.csv";

//        ZoneCollection zones = ZoneGeoJsonIO.readFromGeoJSON(args[1], "NO");
        logger.info("Loading zones...");
        ZoneCollection zones = ZoneEsriShapeIO.read(zonesFile);
        zones.setPrimaryKey("NO");

        BufferedWriter writer = new BufferedWriter(new FileWriter(outFile));
        writer.write("from;to;id;distance");
        writer.newLine();

        KeyMatrix idMatrix = new KeyMatrix();
        KeyMatrix distMatrix = new KeyMatrix();
        AtomicInteger idCounter = new AtomicInteger(0);
        DistanceCalculator dCalc = WGS84DistanceCalculator.getInstance();

        logger.info(String.format("Loading matrix %s...", matrixFile1));
        BufferedReader reader = new BufferedReader(new FileReader(matrixFile1));
        read(reader, writer, idCounter, idMatrix, distMatrix, zones, dCalc);
        reader.close();

        logger.info(String.format("Loading matrix %s...", matrixFile2));
        reader = new BufferedReader(new FileReader(matrixFile2));
        read(reader, writer, idCounter, idMatrix, distMatrix, zones, dCalc);
        reader.close();

        writer.close();
        logger.info("Done.");
    }

    private static void read(BufferedReader reader, BufferedWriter writer, AtomicInteger idCounter, KeyMatrix
            idMatrix, KeyMatrix distMatrix, ZoneCollection zones, DistanceCalculator dCalc) throws IOException {

        GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory();
        String line = reader.readLine();
        int count = 0;

        while ((line = reader.readLine()) != null) {
            String tokens[] = line.split(";");
            String i = tokens[0];
            String j = tokens[1];

            Double id = idMatrix.get(i, j);
            Double d;

            if (id == null) {
                id = new Double(idCounter.get());
                idMatrix.set(i, j, id);
                idCounter.incrementAndGet();

                writer.write(i);
                writer.write(";");
                writer.write(j);
                writer.write(";");
                writer.write(String.valueOf(id.intValue()));
                writer.write(";");

                Zone zi = zones.get(i);
                Zone zj = zones.get(j);

                if (zi != null && zj != null) {
                    if (zi != zj) {
                        d = dCalc.distance(zi.getGeometry().getCentroid(), zj.getGeometry().getCentroid());
                    } else {
                        Geometry geo = zi.getGeometry();
                        Coordinate c = new MinimumDiameter(geo).getWidthCoordinate();
                        d = dCalc.distance(geo.getCentroid(), geometryFactory.createPoint(c));
                    }
                    distMatrix.set(i, j, d);
                    writer.write(String.valueOf(d));
                } else {
                    writer.write("NA");
                }

                writer.newLine();

                count++;
                if (count % 100000 == 0) System.out.println(String.format("Parsed %s lines...", count));
            }
        }
    }
}
