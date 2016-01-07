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

package playground.johannes.studies.matrix2014.matrix.io;

import org.apache.log4j.Logger;
import playground.johannes.studies.matrix2014.matrix.ODPredicate;
import playground.johannes.synpop.gis.Zone;
import playground.johannes.synpop.gis.ZoneCollection;
import playground.johannes.synpop.gis.ZoneGeoJsonIO;
import playground.johannes.synpop.matrix.Matrix;
import playground.johannes.synpop.matrix.MatrixOperations;
import playground.johannes.synpop.matrix.NumericMatrix;
import playground.johannes.synpop.matrix.NumericMatrixIO;

import java.io.IOException;

/**
 * @author johannes
 */
public class PrepareTomTom {

    private static final Logger logger = Logger.getLogger(PrepareTomTom.class);

    public static void main(String args[]) throws IOException {
        String inFile = "/home/johannes/gsv/miv-matrix/raw/TomTom/TTgrob_gesamt_aus_zeitunabhängig.txt";
        String outFile = "/home/johannes/gsv/matrix2014/sim/data/matrices/tomtom.de.txt";
        String zonesFile = "/home/johannes/gsv/gis/zones/geojson/tomtom.gk3.geojson";
        String primaryKey = "NO";

        logger.info("Loading matrix...");
        NumericMatrix m = new NumericMatrix();
        VisumOMatrixReader.read(m, inFile);

        logger.info("Loading zones...");
        ZoneCollection zones = ZoneGeoJsonIO.readFromGeoJSON(zonesFile, primaryKey);

        logger.info("Extracting DE matrix...");
        Predicate p = new Predicate("NUTS0_CODE", "DE", zones);
        m = (NumericMatrix) MatrixOperations.subMatrix(p, m, new NumericMatrix());

        logger.info("Writing matrix...");
        NumericMatrixIO.write(m, outFile);
        logger.info("Done.");
    }

    private static class Predicate implements ODPredicate<String, Double> {

        private final ZoneCollection zones;

        private final String key;

        private final String value;

        public Predicate(String key, String value, ZoneCollection zones) {
            this.key = key;
            this.value = value;
            this.zones = zones;
        }

        @Override
        public boolean test(String row, String col, Matrix<String, Double> matrix) {
            Zone zone_i = zones.get(row);
            Zone zone_j = zones.get(col);

            if(zone_i != null && zone_j != null) {
                return (value.equals(zone_i.getAttribute(key)) && value.equals(zone_j.getAttribute(key)));
            } else {
                if(zone_i == null)
                    logger.warn(String.format("Zone not found: %s", row));

                if(zone_j == null)
                    logger.warn(String.format("Zone not found: %s", col));

                return false;
            }
        }
    }
}
