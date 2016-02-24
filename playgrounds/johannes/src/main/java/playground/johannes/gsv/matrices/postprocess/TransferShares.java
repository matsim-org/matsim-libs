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
import playground.johannes.synpop.gis.Zone;
import playground.johannes.synpop.gis.ZoneCollection;
import playground.johannes.synpop.gis.ZoneGeoJsonIO;
import playground.johannes.synpop.matrix.NumericMatrix;
import playground.johannes.synpop.matrix.ODMatrixOperations;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author johannes
 */
public class TransferShares {

    private static final Logger logger = Logger.getLogger(TransferShares.class);

    private static final String COL_SEPARATOR = ";";

    private static Map<String, NumericMatrix> refMatricesModena;

    private static Map<String, NumericMatrix> refMatricesNuts3;

    private static NumericMatrix targetMatrixModena;

    private static NumericMatrix targetMatrixNuts3;

    private static Map<String, String> zoneIdMapping;

    private static Set<String> zoneKeys = new HashSet<>();

    public static void main(String args[]) throws IOException {
        ZoneCollection zones = ZoneGeoJsonIO.readFromGeoJSON(args[3], "NO");
        loadZoneIdMapping(zones, "NUTS3_CODE");

        refMatricesModena = loadRefMatrix(args[0]);
        refMatricesNuts3 = aggregate(refMatricesModena, zones, "NUTS3_CODE");

        targetMatrixModena = loadTargetMatrix(args[1]);
        targetMatrixNuts3 = ODMatrixOperations.aggregate(targetMatrixModena, zones, "NUTS3_CODE");

        BufferedWriter writer = IOUtils.getBufferedWriter(args[2]);
        writer.write("von;nach;zweck;jahr;Verkehrsmittel;Richtung;Tagestyp;Saison;fahrtenj");
        writer.newLine();

        logger.info("Writing new matrix...");
        ProgressLogger.init(zoneKeys.size(), 2, 10);

        int cntNoChange = 0;
        for (String i : zoneKeys) {
            for (String j : zoneKeys) {
                double factor = calcFactor(i, j);

                for (Map.Entry<String, NumericMatrix> entry : refMatricesModena.entrySet()) {
                    String dimension = entry.getKey();
                    NumericMatrix refMatrix = entry.getValue();

                    Double vol = refMatrix.get(i, j);
                    if (vol != null) {
                        if(factor == 1) cntNoChange++;
                        if(factor == 0) throw new RuntimeException("Factor = 0 must not occur.");

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
            ProgressLogger.step();
        }
        ProgressLogger.terminate();
        writer.close();

        if(cntNoChange > 0) logger.warn(String.format("No changes made for %s relations.", cntNoChange));
        logger.info("Done.");
    }

    private static Map<String, NumericMatrix> loadRefMatrix(String file) throws IOException {
        BufferedReader reader = IOUtils.getBufferedReader(file);


        Map<String, NumericMatrix> refMatrices = new HashMap<>();

        logger.info("Loading reference matrix...");
        String line = reader.readLine();

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
            if (m == null) {
                m = new NumericMatrix();
                refMatrices.put(dimension, m);
            }

            m.add(from, to, volume);

            zoneKeys.add(from);
            zoneKeys.add(to);
        }

        return refMatrices;
    }

    private static NumericMatrix loadTargetMatrix(String file) throws IOException {
        NumericMatrix targetMatrix = new NumericMatrix();

        logger.info("Loading target matrix...");
        BufferedReader reader = IOUtils.getBufferedReader(file);
        String line = reader.readLine();
        while ((line = reader.readLine()) != null) {
            String tokens[] = line.split(COL_SEPARATOR);

            String from = tokens[0];
            String to = tokens[1];
            Double volume = new Double(tokens[8]);

            targetMatrix.add(from, to, volume);
        }

        return targetMatrix;
    }

    private static Map<String, NumericMatrix> aggregate(Map<String, NumericMatrix> matrices, ZoneCollection zones,
                                                        String key) {
        Map<String, NumericMatrix> aggregates = new HashMap<>();
        for (Map.Entry<String, NumericMatrix> entry : matrices.entrySet()) {
            NumericMatrix aggr = ODMatrixOperations.aggregate(entry.getValue(), zones, key);
            aggregates.put(entry.getKey(), aggr);
        }

        return aggregates;
    }

    private static double calcFactor(String i, String j) {
        double factor = calcFactor(i, j, targetMatrixModena, refMatricesModena);

        if(!Double.isNaN(factor)) return factor;
        else {
            String i_nuts3 = zoneIdMapping.get(i);
            String j_nuts3 = zoneIdMapping.get(j);

            factor = calcFactor(i_nuts3, j_nuts3, targetMatrixNuts3, refMatricesNuts3);
            if(!Double.isNaN(factor)) return factor;
            else return 1.0;
        }
    }

    private static double calcFactor(String i, String j, NumericMatrix target, Map<String, NumericMatrix> references) {
        double refSum = 0;
        for (NumericMatrix refMatrix : references.values()) {
            Double vol = refMatrix.get(i, j);
            if (vol != null) refSum += vol;
        }
        /*
        if there is no volume in the reference matrix, it can be ignored
         */
        if(refSum == 0) return 0;

        Double targetSum = target.get(i, j);
        if (targetSum == null) return Double.NaN;
        else return targetSum / refSum;
    }

    private static void loadZoneIdMapping(ZoneCollection zones, String key) {
        zoneIdMapping = new HashMap<>();

        for(Zone zone : zones.getZones()) {
            String modenaKey = zone.getAttribute(zones.getPrimaryKey());
            String nuts3Key = zone.getAttribute(key);

            zoneIdMapping.put(modenaKey, nuts3Key);
        }
    }
}
