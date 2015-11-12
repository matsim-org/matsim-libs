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
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.matsim.contrib.common.gis.DistanceCalculator;
import org.matsim.contrib.common.gis.WGS84DistanceCalculator;
import playground.johannes.gsv.zones.KeyMatrix;
import playground.johannes.synpop.gis.Zone;
import playground.johannes.synpop.gis.ZoneCollection;
import playground.johannes.synpop.gis.ZoneEsriShapeIO;

import java.io.*;

/**
 * @author johannes
 */
public class GenerateODAttributes {

    public static final void main(String args[]) throws IOException {
//        ZoneCollection zones = ZoneGeoJsonIO.readFromGeoJSON(args[1], "NO");
        DistanceCalculator dCalc = WGS84DistanceCalculator.getInstance();
        ZoneCollection zones = ZoneEsriShapeIO.read
                ("/mnt/cifs/B-drive/C_Vertrieb/2014_03_01_Nachfragematrizen_PV/07_Qlik/nuts3.SHP");
        zones.setPrimaryKey("NO");

        KeyMatrix idMatrix = new KeyMatrix();
        KeyMatrix distMatrix = new KeyMatrix();

        int idCounter = 0;

        BufferedWriter writer = new BufferedWriter(new FileWriter
                ("/mnt/cifs/B-drive/C_Vertrieb/2014_03_01_Nachfragematrizen_PV/07_Qlik/odAttributes-kreis.csv"));
        writer.write("from;to;id;distance");
        writer.newLine();

//        BufferedWriter writer2 = new BufferedWriter(new FileWriter(args[3]));

        BufferedReader reader = new BufferedReader(new FileReader
                ("/mnt/cifs/B-drive/C_Vertrieb/2014_03_01_Nachfragematrizen_PV/07_Qlik/MIV_Flug_Kreis_2013.csv"));
        String line = reader.readLine();
//        writer2.write(line);
//        writer2.write(";\"odId\";\"distance\"");
//        writer2.newLine();

//        double d = 0.0;

        GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory();

        int count = 0;
        while((line = reader.readLine()) != null) {
            String tokens[] = line.split(";");
            String i = tokens[0];
            String j = tokens[1];

            Double id = idMatrix.get(i, j);
            Double d = distMatrix.get(i, j);

            if(id == null) {
                id = new Double(idCounter);
                idMatrix.set(i, j, id);
                idCounter++;

                writer.write(i);
                writer.write(";");
                writer.write(j);
                writer.write(";");
                writer.write(String.valueOf(id.intValue()));
                writer.write(";");

                Zone zi = zones.get(i);
                Zone zj = zones.get(j);

                if(zi != null && zj != null) {
                    if(zi != zj) {
                        d = dCalc.distance(zi.getGeometry().getCentroid(), zj.getGeometry().getCentroid());
                    } else {
                        Geometry geo = zi.getGeometry();
                        Coordinate c = new MinimumDiameter(geo).getWidthCoordinate();
                        d = dCalc.distance(geo.getCentroid(), geometryFactory.createPoint(c));
//                        d = new MinimumDiameter(geo).getLength()/2.0;
                    }
                    distMatrix.set(i, j, d);
                    writer.write(String.valueOf(d));
                } else {
                    d = 0.0;
                }

                writer.newLine();

                count++;
                if(count % 10000 == 0) System.out.println(String.format("Parsed %s lines...", count));
            }

//            writer2.write(line);
//            writer2.write(String.format(";%s;%s", id.intValue(), d.intValue()));
//            writer2.newLine();
        }

        reader = new BufferedReader(new FileReader
                ("/mnt/cifs/B-drive/C_Vertrieb/2014_03_01_Nachfragematrizen_PV/07_Qlik/Bahn_Kreis_2013.csv"));
        line = reader.readLine();

        while((line = reader.readLine()) != null) {
            String tokens[] = line.split(";");
            String i = tokens[0];
            String j = tokens[1];

            Double id = idMatrix.get(i, j);
            Double d = distMatrix.get(i, j);

            if(id == null) {
                id = new Double(idCounter);
                idMatrix.set(i, j, id);
                idCounter++;

                writer.write(i);
                writer.write(";");
                writer.write(j);
                writer.write(";");
                writer.write(String.valueOf(id.intValue()));
                writer.write(";");

                Zone zi = zones.get(i);
                Zone zj = zones.get(j);

                if(zi != null && zj != null) {
                    if(zi != zj) {
                        d = dCalc.distance(zi.getGeometry().getCentroid(), zj.getGeometry().getCentroid());
                    } else {
                        Geometry geo = zi.getGeometry();
                        Coordinate c = new MinimumDiameter(geo).getWidthCoordinate();
                        d = dCalc.distance(geo.getCentroid(), geometryFactory.createPoint(c));
                    }
                    distMatrix.set(i, j, d);
                    writer.write(String.valueOf(d));
                } else {
                    d = 0.0;
                }

                writer.newLine();

                count++;
                if(count % 10000 == 0) System.out.println(String.format("Parsed %s lines...", count));
            }
        }
    }
}
