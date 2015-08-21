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
import playground.johannes.gsv.matrices.episodes2matrix.DirectionPredicate;
import playground.johannes.gsv.matrices.episodes2matrix.Episodes2Matrix;
import playground.johannes.gsv.matrices.episodes2matrix.InfereWeCommuter;
import playground.johannes.gsv.synPop.ActivityType;
import playground.johannes.gsv.zones.KeyMatrix;
import playground.johannes.gsv.zones.io.KeyMatrixTxtIO;
import playground.johannes.synpop.data.CommonValues;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author johannes
 */
public class MergeDimensions {

    private static final String SEPARATOR = ";";

    private static final Logger logger = Logger.getLogger(MergeDimensions.class);

    public static void main(String args[]) throws IOException {
        String rootDir = args[0];
        String out = args[1];

        Map<String, String> modes = new HashMap<>();
        modes.put("car", "M");

        Map<String, String> purposes = new HashMap<>();
        purposes.put(ActivityType.EDUCATION, "A");
        purposes.put(ActivityType.WORK, "B");
        purposes.put(ActivityType.BUSINESS, "G");
        purposes.put(ActivityType.SHOP, "E");
        purposes.put(ActivityType.LEISURE, "F1");
        purposes.put(ActivityType.VACATIONS_SHORT, "F2");
        purposes.put(ActivityType.VACATIONS_LONG, "U");
        purposes.put(InfereWeCommuter.WECOMMUTER, "WE");

        Map<String, String> days = new HashMap<>();
        days.put(CommonValues.MONDAY, "1");
        days.put(Episodes2Matrix.DIMIDO, "2");
        days.put(CommonValues.FRIDAY, "5");
        days.put(CommonValues.SATURDAY, "6");
        days.put(CommonValues.SUNDAY, "7");

        Map<String, String> seasons = new HashMap<>();
        seasons.put(Episodes2Matrix.SUMMER, "S");
        seasons.put(Episodes2Matrix.WINTER, "W");

        Map<String, String> directions = new HashMap<>();
        directions.put(DirectionPredicate.OUTWARD, "H");
        directions.put(DirectionPredicate.RETURN, "Z");
        directions.put(DirectionPredicate.INTERMEDIATE, "I");

        BufferedWriter writer = new BufferedWriter(new FileWriter(out));
        writer.write("\"vonmod\";\"nachmod\";\"zweck\";\"jahr\";\"vm\";\"Richtung\";\"Tagestyp\";\"Saison\";" +
                "\"Fahrtenj\"");
        writer.newLine();

        for (Map.Entry<String, String> mode : modes.entrySet()) {
            for (Map.Entry<String, String> purpose : purposes.entrySet()) {
                for (Map.Entry<String, String> day : days.entrySet()) {
                    for (Map.Entry<String, String> season : seasons.entrySet()) {
                        for (Map.Entry<String, String> direction : directions.entrySet()) {
                            String matrixName = String.format("%s.%s.%s.%s.%s", mode.getKey(), purpose.getKey(),
                                    day.getKey(), season.getKey(), direction.getKey());

                            String filename = String.format("%s/%s.txt.gz", rootDir, matrixName);
                            if(new File(filename).exists()) {
                                logger.info(String.format("Loading matrix %s...", matrixName));
                                KeyMatrix m = new KeyMatrix();
                                KeyMatrixTxtIO.read(m, filename);

                                Set<String> keys = m.keys();
                                for (String i : keys) {
                                    for (String j : keys) {
                                        Double vol = m.get(i, j);
                                        if (vol != null) {
                                            writer.write(i);
                                            writer.write(SEPARATOR);
                                            writer.write(j);
                                            writer.write(SEPARATOR);
                                            writer.write(purpose.getValue());
                                            writer.write(SEPARATOR);
                                            writer.write("2013");
                                            writer.write(SEPARATOR);
                                            writer.write(mode.getValue());
                                            writer.write(SEPARATOR);
                                            writer.write(direction.getValue());
                                            writer.write(SEPARATOR);
                                            writer.write(day.getValue());
                                            writer.write(SEPARATOR);
                                            writer.write(season.getValue());
                                            writer.write(SEPARATOR);
                                            writer.write(String.valueOf(vol));
                                            writer.newLine();
                                        }
                                    }
                                }
                            } else {
                                logger.warn(String.format("Matrix %s not found.", matrixName));
                            }
                        }
                    }
                }
            }
        }

        writer.close();
    }
}
