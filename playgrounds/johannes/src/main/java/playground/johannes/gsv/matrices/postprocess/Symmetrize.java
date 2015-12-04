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

import org.apache.log4j.Logger;
import org.matsim.contrib.common.util.ProgressLogger;
import playground.johannes.gsv.zones.ObjectKeyMatrix;

import java.io.*;
import java.util.*;

/**
 * @author johannes
 */
public class Symmetrize {

    private static final Logger logger = Logger.getLogger(Symmetrize.class);

    private static final String DOT = ".";

    private static final String COL_SEPARATOR = ";";

    private static final String OUTWARD = "H";

    private static final String RETURN = "Z";

    private static final String INTERMEDIATE = "I";

    public static void main(String args[]) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(args[0]));
        BufferedWriter writer = new BufferedWriter(new FileWriter(args[1]));

        Set<String> ids = new HashSet<>();
        ObjectKeyMatrix<Boolean> matrix = new ObjectKeyMatrix<>();
        Set<String> subkeys = new HashSet<>();

        Map<String, Double> volumes = new HashMap<>();

        List<String> untouched = new LinkedList<>();

        String line = reader.readLine();
        writer.write(line);
        writer.newLine();

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

            if (direction.equals(INTERMEDIATE) && (from.equals(to))) {
                untouched.add(line);
            } else {
                matrix.set(from, to, false);

                ids.add(from);
                ids.add(to);

                String subkey = buildSubKey(purpose, year, mode, day, season);
                subkeys.add(subkey);

                String key = buildKey(from, to, purpose, year, mode, direction, day, season);
                volumes.put(key, volume);
            }
        }

        logger.info("Symmetrizing...");
        ProgressLogger.init(ids.size(), 2, 10);

        List<String> idList = new ArrayList<>(ids);
        for (int i = 0; i < idList.size(); i++) {
            for (int j = 0; j < idList.size(); j++) {
                String from = idList.get(i);
                String to = idList.get(j);

                Boolean flag = matrix.get(from, to);
                if (flag != null && !flag) {

                    for (String subkey : subkeys) {
                        symmetrize(from, to, subkey, OUTWARD, RETURN, volumes, writer);
                        if (i != j) {
                            symmetrize(to, from, subkey, OUTWARD, RETURN, volumes, writer);
                            symmetrize(from, to, subkey, INTERMEDIATE, INTERMEDIATE, volumes, writer);
                        }
                    }
                    matrix.set(from, to, true);
                    matrix.set(to, from, true);
                }
            }

            ProgressLogger.step();
        }
        ProgressLogger.terminate();

        logger.info("Writing intermediate trips...");
        for (String outline : untouched) {
            writer.write(outline);
            writer.newLine();
        }

        writer.close();

        logger.info("Done.");
    }

    private static void symmetrize(String from, String to, String subkey, String direction1, String direction2,
                                   Map<String, Double> volumes, BufferedWriter writer) throws IOException {
        String toKey = buildFromSubkey(from, to, subkey, direction1);
        Double vol_i = volumes.get(toKey);
        if (vol_i == null) vol_i = 0.0;

        String fromKey = buildFromSubkey(to, from, subkey, direction2);
        Double vol_j = volumes.get(fromKey);
        if (vol_j == null) vol_j = 0.0;

        double avr = (vol_i + vol_j) / 2.0;
        if (avr > 0.0) {
            write(from, to, subkey, direction1, avr, writer);
            write(to, from, subkey, direction2, avr, writer);
        }
    }

    private static String buildSubKey(String purpose, String year, String mode, String day, String season) {
        StringBuilder builder = new StringBuilder(200);
        builder.append(purpose);
        builder.append(DOT);
        builder.append(year);
        builder.append(DOT);
        builder.append(mode);
        builder.append(DOT);
        builder.append(day);
        builder.append(DOT);
        builder.append(season);

        return builder.toString();
    }

    private static String buildKey(String from, String to, String purpose, String year, String mode, String
            direction, String day, String season) {
        StringBuilder builder = new StringBuilder(200);

        builder.append(from);
        builder.append(DOT);
        builder.append(to);
        builder.append(DOT);
        builder.append(purpose);
        builder.append(DOT);
        builder.append(year);
        builder.append(DOT);
        builder.append(mode);
        builder.append(DOT);
        builder.append(day);
        builder.append(DOT);
        builder.append(season);
        builder.append(DOT);
        builder.append(direction);


        return builder.toString();
    }

    private static String buildFromSubkey(String from, String to, String subkey, String direction) {
        StringBuilder builder = new StringBuilder(200);

        builder.append(from);
        builder.append(DOT);
        builder.append(to);
        builder.append(DOT);
        builder.append(subkey);
        builder.append(DOT);
        builder.append(direction);

        return builder.toString();
    }

    private static void write(String from, String to, String subkey, String direction, double volume, BufferedWriter
            writer) throws IOException {
        String tokens[] = subkey.split("\\.");

        writer.write(from);
        writer.write(COL_SEPARATOR);
        writer.write(to);
        writer.write(COL_SEPARATOR);
        writer.write(tokens[0]);
        writer.write(COL_SEPARATOR);
        writer.write(tokens[1]);
        writer.write(COL_SEPARATOR);
        writer.write(tokens[2]);
        writer.write(COL_SEPARATOR);
        writer.write(direction);
        writer.write(COL_SEPARATOR);
        writer.write(tokens[3]);
        writer.write(COL_SEPARATOR);
        writer.write(tokens[4]);
        writer.write(COL_SEPARATOR);
        writer.write(String.valueOf(volume));
        writer.newLine();
    }

}
