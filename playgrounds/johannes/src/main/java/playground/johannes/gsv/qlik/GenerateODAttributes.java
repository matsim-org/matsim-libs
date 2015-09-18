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

import playground.johannes.gsv.zones.KeyMatrix;
import playground.johannes.gsv.zones.Zone;
import playground.johannes.gsv.zones.ZoneCollection;
import playground.johannes.socialnetworks.gis.DistanceCalculator;
import playground.johannes.socialnetworks.gis.WGS84DistanceCalculator;

import java.io.*;

/**
 * @author johannes
 */
public class GenerateODAttributes {

    public static final void main(String args[]) throws IOException {
        ZoneCollection zones = ZoneCollection.readFromGeoJSON(args[1], "NO");
        DistanceCalculator dCalc = WGS84DistanceCalculator.getInstance();

        KeyMatrix m = new KeyMatrix();
        int idCounter = 0;

        BufferedWriter writer = new BufferedWriter(new FileWriter(args[2]));
        writer.write("from;to;id;distance");
        writer.newLine();

        BufferedWriter writer2 = new BufferedWriter(new FileWriter(args[3]));

        BufferedReader reader = new BufferedReader(new FileReader(args[0]));
        String line = reader.readLine();
        writer2.write(line);
        writer2.write(";\"odId\";\"distance\"");
        writer2.newLine();

        double d = 0.0;

        while((line = reader.readLine()) != null) {
            String tokens[] = line.split(";");
            String i = tokens[0];
            String j = tokens[1];

            Double id = m.get(i, j);
            if(id == null) {
                id = new Double(idCounter);
                m.set(i, j, id);
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
                    d = dCalc.distance(zi.getGeometry().getCentroid(), zj.getGeometry().getCentroid());
                    writer.write(String.valueOf(d));
                } else {
                    d = 0.0;
                }

                writer.newLine();
            }

            writer2.write(line);
            writer2.write(String.format(";%s;%s", id.intValue(), d));
            writer2.newLine();
        }
    }
}
