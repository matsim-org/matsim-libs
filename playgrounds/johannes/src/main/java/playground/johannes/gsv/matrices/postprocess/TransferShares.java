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

package playground.johannes.gsv.matrices.postprocess;

import org.apache.log4j.Logger;
import org.matsim.contrib.common.util.ProgressLogger;
import org.matsim.core.utils.io.IOUtils;
import playground.johannes.synpop.matrix.NumericMatrix;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author johannes
 */
public class TransferShares {

    private static final Logger logger = Logger.getLogger(TransferShares.class);

    private static final String COL_SEPARATOR = ";";

    public static void main(String args[]) throws IOException {
        BufferedReader reader = IOUtils.getBufferedReader(args[0]);
        BufferedWriter writer = IOUtils.getBufferedWriter(args[2]);

        Map<String, NumericMatrix> refMatrices = new HashMap<>();

        logger.info("Loading reference matrix...");
        String line = reader.readLine();
        writer.write(line);
        writer.newLine();

        while ((line = reader.readLine()) != null) {
            String tokens[] = line.split(COL_SEPARATOR);

            String from = tokens[0];
            String to = tokens[1];
            Double volume = new Double(tokens[8]);

            StringBuilder builder = new StringBuilder();
            for (int i = 2; i < 8; i++) {
                builder.append(tokens[i]);
                builder.append(COL_SEPARATOR);
            }

            String dimension = builder.toString();
            NumericMatrix m = refMatrices.get(dimension);
            if(m == null) {
                m = new NumericMatrix();
                refMatrices.put(dimension, m);
            }

            m.add(from, to, volume);
        }

        NumericMatrix targetMatrix = new NumericMatrix();

        logger.info("Loading target matrix...");
        reader = IOUtils.getBufferedReader(args[1]);
        line = reader.readLine();
        while ((line = reader.readLine()) != null) {
            String tokens[] = line.split(COL_SEPARATOR);

            String from = tokens[0];
            String to = tokens[1];
            Double volume = new Double(tokens[8]);

            targetMatrix.add(from, to, volume);
        }



        logger.info("Writing new matrix...");
        Set<String> keys = targetMatrix.keys();
        ProgressLogger.init(keys.size(), 2, 10);

        for(String i : keys) {
            for(String j : keys) {
                Double targetSum = targetMatrix.get(i, j);
                if(targetSum != null) {
                    double refSum = 0;
                    for(NumericMatrix refMatrix : refMatrices.values()) {
                        Double vol = refMatrix.get(i, j);
                        if(vol != null) refSum += vol;
                    }

                    double factor = targetSum/refSum;

                    for(Map.Entry<String, NumericMatrix> entry : refMatrices.entrySet()) {
                        String dimension = entry.getKey();
                        NumericMatrix refMatrix = entry.getValue();

                        Double vol = refMatrix.get(i, j);
                        if(vol != null) {
                            vol *= factor;
                            writer.write(i);
                            writer.write(COL_SEPARATOR);
                            writer.write(j);
                            writer.write(COL_SEPARATOR);
                            writer.write(dimension);
                            writer.write(String.valueOf(vol));
                            writer.newLine();
                        }
                    }
                }
            }
            ProgressLogger.step();
        }
        ProgressLogger.terminate();
        writer.close();

        logger.info("Done.");
    }
}
