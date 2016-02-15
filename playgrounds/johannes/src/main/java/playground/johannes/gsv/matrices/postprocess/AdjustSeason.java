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

import gnu.trove.map.hash.TObjectDoubleHashMap;
import org.apache.log4j.Logger;
import org.matsim.contrib.common.util.ProgressLogger;
import playground.johannes.studies.matrix2014.stats.Histogram;

import java.io.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author johannes
 */
public class AdjustSeason {

    private static final Logger logger = Logger.getLogger(AdjustSeason.class);

    private static final String COL_SEPARATOR = ";";

    private static final String DOT = ".";

    private static final String WINTER = "W";

    private static final String SUMMER = "S";

    public static void main(String args[]) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(args[0]));
        BufferedWriter writer = new BufferedWriter(new FileWriter(args[1]));

        Set<String> subkeys = new HashSet<>();
        Map<String, Double> volumes = new HashMap<>();

        String line = reader.readLine();
        writer.write(line);
        writer.newLine();

        Map<String, TObjectDoubleHashMap<String>> seasons = new HashMap<>();

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

            String subkey = buildSubKey(from, to, purpose, year, mode, direction, day);
            subkeys.add(subkey);

            TObjectDoubleHashMap<String> hist = seasons.get(purpose);
            if(hist == null) {
                hist = new TObjectDoubleHashMap<>();
                seasons.put(purpose, hist);
            }

            hist.adjustOrPutValue(season, volume, volume);
        }

        for(Map.Entry<String, TObjectDoubleHashMap<String>> entry : seasons.entrySet()) {
            TObjectDoubleHashMap<String> hist = entry.getValue();
            Histogram.normalize(hist);
            logger.info(String.format("%s: S=%.2f, W=%.2f", entry.getKey(), hist.get(SUMMER), hist.get(WINTER)));
        }

        logger.info("Adjusting...");

        ProgressLogger.init(subkeys.size(), 2, 10);

        for(String subkey : subkeys) {
            StringBuilder builder = new StringBuilder(200);
            builder.append(subkey);
            builder.append(COL_SEPARATOR);
            builder.append(SUMMER);
            String sumKey = builder.toString();

            Double sumVol = volumes.get(sumKey);
            if(sumVol == null) sumVol = 0.0;

            builder = new StringBuilder(200);
            builder.append(subkey);
            builder.append(COL_SEPARATOR);
            builder.append(WINTER);
            String winKey = builder.toString();

            Double winVol = volumes.get(winKey);
            if(winVol == null) winVol = 0.0;

            String purpose = subkey.split(COL_SEPARATOR)[2];

            TObjectDoubleHashMap<String> hist = seasons.get(purpose);
            double sumFactor = hist.get(SUMMER);
            double winFactor = hist.get(WINTER);

            double total = sumVol + winVol;

            sumVol = total * sumFactor;
            winVol = total * winFactor;

            writer.write(sumKey);
            writer.write(COL_SEPARATOR);
            writer.write(String.valueOf(sumVol));
            writer.newLine();

            writer.write(winKey);
            writer.write(COL_SEPARATOR);
            writer.write(String.valueOf(winVol));
            writer.newLine();

            ProgressLogger.step();
        }
        ProgressLogger.terminate();
        writer.close();

        logger.info("Done.");
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
            direction, String day) {
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

        return builder.toString();
    }
}
