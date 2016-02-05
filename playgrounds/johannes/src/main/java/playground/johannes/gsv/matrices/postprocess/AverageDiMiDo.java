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
import gnu.trove.map.hash.TObjectIntHashMap;
import org.apache.log4j.Logger;

import java.io.*;

/**
 * @author johannes
 */
public class AverageDiMiDo {

    private static final Logger logger = Logger.getLogger(AverageDiMiDo.class);

    private static final String COL_SEPARATOR = ";";

    public static void main(String args[]) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(args[0]));
        BufferedWriter writer = new BufferedWriter(new FileWriter(args[1]));

        String line = reader.readLine();
        writer.write(line);
        writer.newLine();

        TObjectDoubleHashMap<String> volumes = new TObjectDoubleHashMap<>();
        TObjectIntHashMap<String> counts = new TObjectIntHashMap<>();

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

            if (day.equals("2") || day.equals("3") || day.equals("4")) {
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
                builder.append(season);

                String subkey = builder.toString();
                double volsum = volumes.adjustOrPutValue(subkey, volume.doubleValue(), volume.doubleValue());
                int cnt = counts.adjustOrPutValue(subkey, 1, 1);
                if(cnt == 3) {
                    writer.write(from);
                    writer.write(COL_SEPARATOR);
                    writer.write(to);
                    writer.write(COL_SEPARATOR);
                    writer.write(purpose);
                    writer.write(COL_SEPARATOR);
                    writer.write(year);
                    writer.write(COL_SEPARATOR);
                    writer.write(mode);
                    writer.write(COL_SEPARATOR);
                    writer.write(direction);
                    writer.write(COL_SEPARATOR);
                    writer.write("2");
                    writer.write(COL_SEPARATOR);
                    writer.write(season);
                    writer.write(COL_SEPARATOR);
                    writer.write(String.valueOf(volsum/3.0));
                    writer.newLine();

                    volumes.remove(subkey);
                    counts.remove(subkey);
                }

            } else {
                writer.write(line);
                writer.newLine();
            }
        }

        writer.close();
        logger.info("Done.");
    }
}
