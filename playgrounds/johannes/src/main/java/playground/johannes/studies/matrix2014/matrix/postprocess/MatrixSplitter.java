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

package playground.johannes.studies.matrix2014.matrix.postprocess;

import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.Logger;
import org.matsim.core.utils.io.IOUtils;
import playground.johannes.studies.matrix2014.matrix.io.GSVMatrixWriter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * @author johannes
 */
public class MatrixSplitter {

    private static final Logger logger = Logger.getLogger(MatrixSplitter.class);

    private static final String SEPARATOR = GSVMatrixWriter.SEPARATOR;

    private List<String> groupDimensions;

    private Collection<Pair<Map<String, String>, TObjectDoubleMap<String>>> shareTable;

    private Map<String, DimensionCalculator> dimensionCalculators;

    private List<String> values;

    private String newDimension;

    private TObjectDoubleMap<String> fallbackShares;

    private int fallbackCount;

    public MatrixSplitter(String name, List<Pair<Map<String, String>, TObjectDoubleMap<String>>> table) {
        this.newDimension = name;
        values = new ArrayList<>(table.get(0).getRight().keySet());
        groupDimensions = new ArrayList<>(table.get(0).getLeft().keySet());
        shareTable = table;
        dimensionCalculators = new HashMap<>();

    }

    public void addDimensionCalculator(String name, DimensionCalculator calculator) {
        dimensionCalculators.put(name, calculator);
    }

    public void process(String sourceFile, String targetFile) throws IOException {
        fallbackCount = 0;

        long totalBytes = new File(sourceFile).length();
        long bytesRead = 0;
        long threshold = 0;
        long step = Math.max(1000000, totalBytes/100);

        BufferedReader reader = IOUtils.getBufferedReader(sourceFile);
        BufferedWriter writer = IOUtils.getBufferedWriter(targetFile);

        String line = reader.readLine();
        bytesRead += line.length();

        String[] keys = line.split(SEPARATOR);
        /*
        write original header
         */
        writer.write(line);
        /*
        write additional column name
         */
        writer.write(SEPARATOR);
        writer.write(newDimension);
        writer.newLine();
        /*
        process matrix file
         */
        while((line = reader.readLine()) != null) {
            bytesRead += line.length();
            String[] tokens = line.split(SEPARATOR);

            String origin = tokens[0];
            String destination = tokens[1];
            double volume = Double.parseDouble(tokens[2]);
            /*
            create dimension map
             */
            Map<String, String> fields = new LinkedHashMap<>();
            if(tokens.length > 3) {
                for(int i = 3; i < tokens.length; i++) {
                    fields.put(keys[i], tokens[i]);
                }
            }
            /*
            split line
             */
            processLine(origin, destination, volume, fields, writer);

            if(bytesRead > threshold) {
                logger.info(String.format("%s MB read (%s %%).", (int)(bytesRead/1000000.0), (int)(bytesRead/(double)totalBytes * 100)));
                threshold += step;
            }
        }

        writer.close();

        if(fallbackCount > 0) {
            logger.warn(String.format("Fallback share used %s times.", fallbackCount));
        }
    }

    private void processLine(String origin, String destination, double volume, Map<String, String> fields, BufferedWriter writer) throws IOException {
        Map<String, String> groupFields = new HashMap();
        for(String key : groupDimensions) {
            String value = fields.get(key);
            if(value == null) {
                value = dimensionCalculators.get(key).calculate(origin, destination, volume, fields);
            }
            groupFields.put(key, value);
        }
        TObjectDoubleMap<String> shares = getShares(groupFields);


        for(String dim : values) {
            double share = shares.get(dim);

            writer.write(origin);
            writer.write(SEPARATOR);
            writer.write(destination);
            writer.write(SEPARATOR);

            double newVolume = volume * share;

            writer.write(String.valueOf(newVolume));
            writer.write(SEPARATOR);

            Set<String> keys = fields.keySet();
            for(String key : keys) {
                writer.write(fields.get(key));
                writer.write(SEPARATOR);
            }

            writer.write(dim);
            writer.newLine();
        }
    }

    private TObjectDoubleMap<String> getShares(Map<String, String> dimensions) {
        TObjectDoubleMap<String> shares = null;

        for(Pair<Map<String, String>, TObjectDoubleMap<String>> entry : shareTable) {
            if(dimensions.equals(entry.getLeft())) {
                shares = entry.getRight();
                break;
            }
        }

        if(shares == null) {
            if(fallbackShares == null) {
                fallbackShares = new TObjectDoubleHashMap<>();
                double p = 1/(double)values.size();
                for(String value : values) {
                    fallbackShares.put(value, p);
                }
            }

            shares = fallbackShares;

            fallbackCount++;
        }

        return shares;
    }
}
