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

package playground.johannes.gsv.matrices.analysis;

import gnu.trove.map.hash.TObjectDoubleHashMap;
import org.apache.log4j.Logger;
import org.matsim.contrib.common.stats.Discretizer;
import org.matsim.contrib.common.stats.FixedSampleSizeDiscretizer;
import org.matsim.contrib.common.util.ProgressLogger;
import playground.johannes.synpop.gis.Zone;
import playground.johannes.synpop.gis.ZoneCollection;
import playground.johannes.synpop.gis.ZoneGeoJsonIO;
import playground.johannes.synpop.matrix.NumericMatrix;
import playground.johannes.synpop.matrix.NumericMatrixTxtIO;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Set;

/**
 * @author johannes
 */
public class ModalSplitPopDensity {

    private static final Logger logger = Logger.getLogger(ModalSplitPopDensity.class);

    public static final void main(String args[]) throws IOException {
        String outdir = "/home/johannes/gsv/miv-matrix/qs2013/";
        String matrixFile = "/home/johannes/gsv/miv-matrix/qs2013/matrix.txt";
        String zoneFile = "/home/johannes/gsv/gis/zones/geojson/nuts3.psm.gk3.geojson";
        String inhabFile = "/home/johannes/gsv/miv-matrix/qs2013/inhabitants.csv";

        final NumericMatrix carVol = new NumericMatrix();
        final NumericMatrix railVol = new NumericMatrix();
        final NumericMatrix airVol = new NumericMatrix();

        logger.info("Loading zones...");
        ZoneCollection zones = ZoneGeoJsonIO.readFromGeoJSON(zoneFile, "NO", null);
        TObjectDoubleHashMap<String> zoneRho = calcDensity(inhabFile, zones);

        logger.info("Reading matrix...");
        MatrixReader mReader = new MatrixReader();
        mReader.read(matrixFile, new MatrixReader.RowHandler() {
            @Override
            public void handleRow(String from, String to, String purpose, String year, String mode, String direction, String day, String season, double volume) {
                if (mode.equalsIgnoreCase("M")) {
                    carVol.add(from, to, volume);
                } else if (mode.equalsIgnoreCase("B")) {
                    railVol.add(from, to, volume);
                } else if (mode.equalsIgnoreCase("F")) {
                    airVol.add(from, to, volume);
                }
            }
        });

        logger.info("Calculating shares per od-pair...");
        NumericMatrix carShare = new NumericMatrix();
        NumericMatrix railShare = new NumericMatrix();
        NumericMatrix airShare = new NumericMatrix();
        NumericMatrix odCounts = new NumericMatrix();

        Discretizer discr = FixedSampleSizeDiscretizer.create(zoneRho.values(), 1, 20);
//        Discretizer discr = new DummyDiscretizer();

        Set<String> keys = carVol.keys();
        keys.addAll(railVol.keys());
        keys.addAll(airVol.keys());

        ProgressLogger.init(keys.size(), 2, 10);

        for (String from : keys) {
            for (String to : keys) {
                Double car = carVol.get(from, to);
                if (car == null) car = 0.0;
                Double rail = railVol.get(from, to);
                if (rail == null) rail = 0.0;
                Double air = airVol.get(from, to);
                if (air == null) air = 0.0;

                if (car > 0 && rail > 0 && air > 0) {
                    double total = car + rail + air;

                    double fromRho = zoneRho.get(from);
                    double toRho = zoneRho.get(to);

                    fromRho = discr.discretize(fromRho);
                    toRho = discr.discretize(toRho);

                    String fromRhoStr = String.valueOf(fromRho);
                    String toRhoStr = String.valueOf(toRho);
                    odCounts.add(fromRhoStr, toRhoStr, 1);
//                    odCounts.add(fromRhoStr, toRhoStr, total);

                    carShare.add(fromRhoStr, toRhoStr, car / total);
                    railShare.add(fromRhoStr, toRhoStr, rail / total);
                    airShare.add(fromRhoStr, toRhoStr, air / total);
                }
            }
            ProgressLogger.step();
        }
        ProgressLogger.terminate();

        logger.info("Calculating averages...");
        keys = odCounts.keys();
        for (String from : keys) {
            for (String to : keys) {
                Double count = odCounts.get(from, to);
                if(count != null) {
                    carShare.multiply(from, to, 1/count);
                    railShare.multiply(from, to, 1/count);
                    airShare.multiply(from, to, 1/count);
                }
            }
        }

        logger.info("Writing matrices...");
        NumericMatrixTxtIO.write(carShare, String.format("%s/carShare.txt", outdir));
        NumericMatrixTxtIO.write(railShare, String.format("%s/railShare.txt", outdir));
        NumericMatrixTxtIO.write(airShare, String.format("%s/airShare.txt", outdir));
        logger.info("Done.");
    }

    private static TObjectDoubleHashMap<String> calcDensity(String file, ZoneCollection zones) throws IOException {
        TObjectDoubleHashMap<String> inhabs = new TObjectDoubleHashMap<>();
        BufferedReader reader = new BufferedReader(new FileReader(file));
        String line = reader.readLine();
        while((line = reader.readLine()) != null) {
            String tokens[] = line.split("\t");
            String id = tokens[0];
            double pop = Double.parseDouble(tokens[1]);
            Zone zone = zones.get(id);
            if(zone != null) {
                double rho = pop/zone.getGeometry().getArea() * 1000 * 1000;
                inhabs.put(id, rho);
            }
        }
        reader.close();
        return inhabs;
    }
}
