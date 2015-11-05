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

package playground.johannes.gsv.matrices.postprocess;

import gnu.trove.TObjectDoubleHashMap;
import gnu.trove.TObjectDoubleIterator;
import org.apache.log4j.Logger;
import org.matsim.contrib.common.gis.DistanceCalculator;
import org.matsim.contrib.common.gis.WGS84DistanceCalculator;
import org.matsim.contrib.common.stats.Discretizer;
import org.matsim.contrib.common.stats.FixedBordersDiscretizer;
import playground.johannes.gsv.zones.KeyMatrix;
import playground.johannes.synpop.gis.Zone;
import playground.johannes.synpop.gis.ZoneCollection;
import playground.johannes.synpop.gis.ZoneEsriShapeIO;

import java.io.*;
import java.util.*;

/**
 * @author johannes
 */
public class AdjustDays {

    private static final Logger logger = Logger.getLogger(AdjustDays.class);

    private static final String COL_SEPARATOR = ";";

    public static void main(String args[]) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(args[0]));
        BufferedWriter writer = new BufferedWriter(new FileWriter(args[1]));
//        ZoneCollection zones = ZoneEsriShapeIO.read("/home/johannes/gsv/gis/modena/shp/zones.SHP");
        ZoneCollection zones = ZoneEsriShapeIO.read("/home/johannes/gsv/gis/modena/shp/zones.SHP");
        zones.setPrimaryKey("NO");

        DistanceCalculator distCalc = WGS84DistanceCalculator.getInstance();
        KeyMatrix distances = new KeyMatrix();
        Set<String> subkeys = new HashSet<>();
        Map<String, Double> volumes = new HashMap<>();

        String line = reader.readLine();
        writer.write(line);
        writer.newLine();

        Map<String, TObjectDoubleHashMap<String>> days = new HashMap<>();

        logger.info("Loading file...");
        while ((line = reader.readLine()) != null) {
            String tokens[] = line.split(COL_SEPARATOR);

            String from = tokens[0];
            String to = tokens[1];
            String purpose = tokens[2];
            String year = tokens[3];
            String mode = tokens[4];
            String direction = tokens[5];
            String day = tokens[6];
            String season = tokens[7];
            Double volume = new Double(tokens[8]);

            String key = buildKey(from, to, purpose, year, mode, direction, day, season);
            volumes.put(key, volume);

            Zone fromZone = zones.get(from);
            Zone toZone = zones.get(to);
            Double d = distances.get(from, to);
            if (d == null) {
                d = distances.get(to, from);
                if (d == null) {
                    d = distCalc.distance(fromZone.getGeometry().getCentroid(), toZone.getGeometry().getCentroid());
                    distances.set(from, to, d);
                    distances.set(to, from, d);
                }
            }
            Discretizer discretizer = new FixedBordersDiscretizer(new double[]{100000, 10000000});
            d = discretizer.discretize(d);

            String histKey = String.format("%s;%d;%s", purpose, (int)(d/1000), season);
            TObjectDoubleHashMap<String> hist = days.get(histKey);
            if (hist == null) {
                hist = new TObjectDoubleHashMap<>();
                days.put(histKey, hist);
            }

            hist.adjustOrPutValue(day, volume, volume);
        }

        for (Map.Entry<String, TObjectDoubleHashMap<String>> entry : days.entrySet()) {
            TObjectDoubleHashMap<String> hist = entry.getValue();
//            Histogram.normalize(hist);

            StringBuilder builder = new StringBuilder();
            TObjectDoubleIterator<String> it = hist.iterator();
            for (int i = 0; i < hist.size(); i++) {
                it.advance();
                builder.append(it.key());
                builder.append("=");
                builder.append(String.format(Locale.US, "%.1f", it.value()));
                builder.append(", ");
            }
//            logger.info(String.format("%s: %s", entry.getKey(), builder.toString()));
            System.out.println(String.format("%s: %s", entry.getKey(), builder.toString()));
        }
    }

    private static String buildKey(String from, String to, String purpose, String year, String mode, String
            direction, String day, String season) {
        StringBuilder builder = new StringBuilder(200);

        builder.append(from);
        builder.append(COL_SEPARATOR);
        builder.append(to);
        builder.append(COL_SEPARATOR);
        builder.append(purpose);
        builder.append(COL_SEPARATOR);
        builder.append(year);
        builder.append(COL_SEPARATOR);
        builder.append(mode);
        builder.append(COL_SEPARATOR);
        builder.append(direction);
        builder.append(COL_SEPARATOR);
        builder.append(day);
        builder.append(COL_SEPARATOR);
        builder.append(season);

        return builder.toString();
    }
}
