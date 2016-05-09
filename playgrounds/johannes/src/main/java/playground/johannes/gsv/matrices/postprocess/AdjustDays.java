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

import gnu.trove.iterator.TObjectDoubleIterator;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import org.apache.log4j.Logger;
import org.matsim.contrib.common.gis.DistanceCalculator;
import org.matsim.contrib.common.gis.WGS84DistanceCalculator;
import org.matsim.contrib.common.stats.Discretizer;
import org.matsim.contrib.common.stats.FixedBordersDiscretizer;
import org.matsim.contrib.common.util.ProgressLogger;
import playground.johannes.studies.matrix2014.stats.Histogram;
import playground.johannes.synpop.gis.Zone;
import playground.johannes.synpop.gis.ZoneCollection;
import playground.johannes.synpop.gis.ZoneGeoJsonIO;
import playground.johannes.synpop.matrix.NumericMatrix;

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
        ZoneCollection zones = ZoneGeoJsonIO.readFromGeoJSON(args[2], "NO", null);

        DistanceCalculator distCalc = WGS84DistanceCalculator.getInstance();
        NumericMatrix distances = new NumericMatrix();
        Set<String> subkeys = new HashSet<>();
        Map<String, Double> volumes = new HashMap<>();

        Discretizer discretizer = new FixedBordersDiscretizer(new double[]{20000, 50000, 100000, 10000000});

        String line = reader.readLine();
        writer.write(line);
        writer.newLine();

        Map<String, TObjectDoubleHashMap<String>> histograms = new HashMap<>();

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

            String subkey = buildSubKey(from, to, purpose, year, mode, direction, season);
            subkeys.add(subkey);

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

            d = discretizer.discretize(d);

            String histKey = buildHistKey(purpose, d);
            TObjectDoubleHashMap<String> hist = histograms.get(histKey);
            if (hist == null) {
                hist = new TObjectDoubleHashMap<>();
                histograms.put(histKey, hist);
            }

            hist.adjustOrPutValue(day, volume, volume);

            if(purpose.equals("F2")) {
                histKey = buildHistKey("U", d);
                hist = histograms.get(histKey);
                if (hist == null) {
                    hist = new TObjectDoubleHashMap<>();
                    histograms.put(histKey, hist);
                }

                hist.adjustOrPutValue(day, volume, volume);
            }
        }

        for (Map.Entry<String, TObjectDoubleHashMap<String>> entry : histograms.entrySet()) {
            TObjectDoubleHashMap<String> hist = entry.getValue();
            Histogram.normalize(hist);

            StringBuilder builder = new StringBuilder();
            TObjectDoubleIterator<String> it = hist.iterator();
            for (int i = 0; i < hist.size(); i++) {
                it.advance();
                builder.append(it.key());
                builder.append("=");
                builder.append(String.format(Locale.US, "%.2f", it.value()));
                builder.append(", ");
            }
            logger.info(String.format("%s: %s", entry.getKey(), builder.toString()));
//            System.out.println(String.format("%s: %s", entry.getKey(), builder.toString()));
        }

        List<String> dayCodes = new ArrayList<>(5);
        dayCodes.add("1");
        dayCodes.add("2");
        dayCodes.add("5");
        dayCodes.add("6");
        dayCodes.add("7");

        logger.info("Adjusting days...");
        ProgressLogger.init(subkeys.size(), 2, 10);
        for(String subkey : subkeys) {
            String tokens[] = subkey.split(COL_SEPARATOR);

            double totalVol = 0.0;

            List<String> keys = new ArrayList<>(5);
            for(String dayCode : dayCodes) {
                String key = rebuildKey(tokens, dayCode);
                keys.add(key);
                Double vol = volumes.get(key);
                if(vol != null) {
                    totalVol += vol;
                }
            }

            String from = tokens[0];
            String to = tokens[1];

            Double d = distances.get(from, to);
            d = discretizer.discretize(d);

            String histKey = buildHistKey(tokens[2], d);
            TObjectDoubleHashMap<String> hist = histograms.get(histKey);

            for(int i = 0; i < dayCodes.size(); i++) {
                String dayCode = dayCodes.get(i);
                double factor = hist.get(dayCode);
                double vol = totalVol * factor;

                String key = keys.get(i);
                writer.write(key);
                writer.write(COL_SEPARATOR);
                writer.write(String.valueOf(vol));
                writer.newLine();
            }

            ProgressLogger.step();
        }
        ProgressLogger.terminate();
        writer.close();
        logger.info("Done.");

    }

    private static String buildHistKey(String purpose, double d) {
        return String.format("%s;%d", purpose, (int)(d/1000));
    }

    private static String rebuildKey(String tokens[], String day) {
        StringBuilder builder = new StringBuilder(200);

        for(int i = 0; i < 6; i++) {
            builder.append(tokens[i]);
            builder.append(COL_SEPARATOR);
        }
        builder.append(day);
        builder.append(COL_SEPARATOR);
        builder.append(tokens[6]);


        return builder.toString();
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

    private static String buildSubKey(String from, String to, String purpose, String year, String mode, String
            direction, String season) {
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
//        builder.append(COL_SEPARATOR);
//        builder.append(day);
        builder.append(COL_SEPARATOR);
        builder.append(season);

        return builder.toString();
    }
}
