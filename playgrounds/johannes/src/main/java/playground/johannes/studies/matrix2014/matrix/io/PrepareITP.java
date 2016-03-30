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
import playground.johannes.synpop.gis.Zone;
import playground.johannes.synpop.gis.ZoneCollection;
import playground.johannes.synpop.gis.ZoneGeoJsonIO;
import playground.johannes.synpop.matrix.MatrixOperations;
import playground.johannes.synpop.matrix.NumericMatrix;
import playground.johannes.synpop.matrix.NumericMatrixIO;

import java.io.IOException;
import java.util.Set;

/**
 * @author johannes
 */
public class PrepareITP {

    private static final Logger logger = Logger.getLogger(PrepareITP.class);

    public static void main(String args[]) throws IOException {
        String inFile = "/home/johannes/gsv/miv-matrix/raw/Lieferung_Intraplan/IV_Gesamt.mtx";
        String outFile = "/home/johannes/gsv/miv-matrix/raw/Lieferung_Intraplan/";
        String zonesFile = "/home/johannes/gsv/gis/zones/geojson/nuts3.psm.airports.gk3.geojson";

        logger.info("Loading visum matrix...");
        NumericMatrix m = new NumericMatrix();
        VisumOMatrixReader.read(m, inFile);

        logger.info("Loading zones...");
        ZoneCollection zones = ZoneGeoJsonIO.readFromGeoJSON(zonesFile, "NO", null);

        logger.info("Checking zones...");
        Set<String> keys = m.keys();
        for(String key : keys) {
            Zone zone = zones.get(key);
            if (zone == null) {
                logger.warn(String.format("Zone %s not found.", key));
            }
        }

        logger.info("Writing full matrix...");
        NumericMatrixIO.write(m, outFile + "itp.txt");

        logger.info("Extracting DE matrix...");
        ZoneAttributePredicate p = new ZoneAttributePredicate("NUTS0_CODE", "DE", zones);
        m = (NumericMatrix) MatrixOperations.subMatrix(p, m, new NumericMatrix());

        logger.info("Writing DE matrix...");
        NumericMatrixIO.write(m, outFile + "itp.de.txt");

        logger.info("Done.");
    }
}
