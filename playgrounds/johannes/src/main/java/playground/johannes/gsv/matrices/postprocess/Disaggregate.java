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

import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import org.apache.log4j.Logger;
import org.matsim.contrib.common.util.XORShiftRandom;
import org.matsim.core.utils.io.IOUtils;
import playground.johannes.studies.matrix2014.stats.Histogram;
import playground.johannes.synpop.matrix.MatrixOperations;
import playground.johannes.synpop.matrix.NumericMatrix;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * @author johannes
 */
public class Disaggregate {

    private static final Logger logger = Logger.getLogger(Disaggregate.class);

    private static final String COL_SEPARATOR = ";";

    private static final TObjectDoubleMap<String> population = new TObjectDoubleHashMap<>();;

    private static final Map<String, List<String>> zoneMapping = new HashMap<>();

    private static final Random random = new XORShiftRandom(1);

    public static void main(String args[]) throws IOException {
//        String inFile = "/Users/johannes/gsv/miv-matrix/postprocess/MIV_Flug_Kreis_2013.csv";
//        String mappingFile = "/Users/johannes/gsv/miv-matrix/postprocess/modena2nuts3.csv";
//        String outFile = "/Users/johannes/gsv/miv-matrix/postprocess/miv2013.17032016.csv";
        String inFile = args[0];
        String mappingFile = args[1];
        double threshold = Double.parseDouble(args[2]);
        String modeFilter = args[3];
        String outFile = args[4];
        /*
         * load zone mappings and population
         */
        logger.info("Load zone mappings...");
        BufferedReader reader = new BufferedReader(new FileReader(mappingFile));
        String line = reader.readLine();
        while((line = reader.readLine()) != null) {

            String tokens[] = line.split(COL_SEPARATOR);
            String modenaId = tokens[0];
            String nuts3Id = tokens[1];
            double inhabs = Double.parseDouble(tokens[2]);

            population.put(modenaId, inhabs);
            List<String> zones = zoneMapping.get(nuts3Id);
            if(zones == null) {
                zones = new ArrayList<>();
                zoneMapping.put(nuts3Id, zones);
            }
            zones.add(modenaId);
        }
        /*
         * read and nuts3 matrix
         */
        logger.info("Load nuts3 matrix...");
        reader = new BufferedReader(new FileReader(inFile));
        BufferedWriter writer = IOUtils.getBufferedWriter(outFile);

        line = reader.readLine();
        writer.write(line);
        writer.newLine();

        int count = 0;
        while((line = reader.readLine()) != null) {
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

            if(mode.equalsIgnoreCase(modeFilter)) {
                if (volume == 0) {
                    writer.write(line);
                    writer.newLine();
                } else {
                    NumericMatrix m = disaggregate(from, to, volume, threshold);

                    Set<String> keys = m.keys();
                    for (String i : keys) {
                        for (String j : keys) {
                            Double vol = m.get(i, j);
                            if(vol != null) writeLine(writer, i, j, purpose, year, mode, direction, day, season, vol);
                        }
                    }
                }

                count++;
                if (count % 10000 == 0) logger.info(String.format("Processed %s lines...", count));
            }
        }
        writer.close();
        logger.info("Done.");

    }

    private static void writeLine(BufferedWriter writer, String i, String j, String purpose, String year, String mode,
                                  String direction, String day, String season, double vol) throws IOException {
        writer.write(i);
        writer.write(COL_SEPARATOR);
        writer.write(j);
        writer.write(COL_SEPARATOR);
        writer.write(purpose);
        writer.write(COL_SEPARATOR);
        writer.write(year);
        writer.write(COL_SEPARATOR);
        writer.write(mode);
        writer.write(COL_SEPARATOR);
        writer.write(direction);
        writer.write(COL_SEPARATOR);
        writer.write(day);
        writer.write(COL_SEPARATOR);
        writer.write(season);
        writer.write(COL_SEPARATOR);
        writer.write(String.valueOf(vol));
        writer.newLine();
    }

    private static NumericMatrix disaggregate(String origin, String destination, double volume, double threshold) {
        List<String> origins = zoneMapping.get(origin);
        List<String> destinations = zoneMapping.get(destination);

        if(origins == null || destinations == null) throw new RuntimeException("No modena zones found!");

        TObjectDoubleMap<String> originShares = calcShares(origins);
        TObjectDoubleMap<String> destinationShares = calcShares(destinations);

        NumericMatrix m = new NumericMatrix();
        /*
         * split volume according to row share and uniform in columns
         */
        for(String row : origins) {
            double originVol = volume * originShares.get(row);
            double cellVol = originVol / (double)destinations.size();
            for(String col : destinations) {
                m.set(row, col, cellVol);
            }
        }
        /*
        adjust cells according to column shares
         */
        for(String col : destinations) {
            double colSum = 0;
            for(String row : origins) {
                colSum += m.get(row, col);
            }

            double destVol = volume * destinationShares.get(col);
            double f = destVol / colSum;
            for(String row : origins) {
                m.multiply(row, col, f);
            }
        }

        discretize(m, threshold, origins, destinations);
        /*
        check
         */
        double sum = MatrixOperations.sum(m);
        sum = (double)Math.round(sum);
        volume = (double)Math.round(volume);

        if(volume != sum) logger.warn(String.format("Target volume: %s, matrix sum: %s", volume, sum));

        return m;
    }

    private static TObjectDoubleMap<String> calcShares(Collection<String> zones) {
        TObjectDoubleMap<String> shares = new TObjectDoubleHashMap<>();
        double sum = 0;
        for(String z : zones) {
            double pop = population.get(z);
            shares.put(z, pop);
            sum += pop;
        }

        if(sum == 0) {
            /*
            if the sum is zero, treat as uniform distribution
             */
            for(String z : zones) {
                shares.put(z, 1.0);
            }
        }

        Histogram.normalize(shares);

        return shares;
    }

    private static void discretize(NumericMatrix m, double threshold, List<String> origins, List<String> destinations) {
        double reminder = 0;
        int nonZeroCells = 0;

        for(String row : origins) {
            for(String col : destinations) {
                double vol = m.get(row, col);
                if(vol < threshold) {
                    reminder += vol;
                    m.set(row, col, null);
                } else {
                    nonZeroCells++;
                }
            }
        }

        if(reminder > 0) {
            if(nonZeroCells == 0) {
                String row = origins.get(random.nextInt(origins.size()));
                String col = destinations.get(random.nextInt(destinations.size()));
                m.add(row, col, reminder);
            } else {
                double rvol = reminder / (double) nonZeroCells;
                for (String row : origins) {
                    for (String col : destinations) {
                        Double vol = m.get(row, col);
                        if (vol != null) {
                            m.add(row, col, rvol);
                        }
                    }
                }
            }
        }
    }
}
