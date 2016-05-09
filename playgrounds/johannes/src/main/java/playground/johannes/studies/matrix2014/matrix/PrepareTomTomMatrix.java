/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,       *
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
package playground.johannes.studies.matrix2014.matrix;

import org.matsim.contrib.common.gis.CartesianDistanceCalculator;
import org.matsim.contrib.common.gis.DistanceCalculator;
import playground.johannes.synpop.gis.Zone;
import playground.johannes.synpop.gis.ZoneCollection;
import playground.johannes.synpop.gis.ZoneGeoJsonIO;
import playground.johannes.synpop.matrix.NumericMatrix;
import playground.johannes.synpop.matrix.NumericMatrixIO;

import java.io.IOException;
import java.util.Set;

/**
 * @author jillenberger
 */
public class PrepareTomTomMatrix {

    public static void main(String args[]) throws IOException {
        String in = "/Users/jillenberger/work/matrix2014/data/matrices/tomtom.de.txt";
        String out = "/Users/jillenberger/work/matrix2014/data/matrices/tomtom.de.100KM.txt";
        String zoneFile = "/Users/jillenberger/work/matrix2014/data/zones/nuts3.psm.gk3.geojson";
        String idKey = "NO";

        NumericMatrix m = NumericMatrixIO.read(in);
        ZoneCollection zones = ZoneGeoJsonIO.readFromGeoJSON(zoneFile, idKey, null);
        DistanceCalculator dCalc = CartesianDistanceCalculator.getInstance();
        double threshold = 100000;

        Set<String> keys = m.keys();
        for(String i : keys) {
            Zone z_i = zones.get(i);
            for(String j : keys) {
                Zone z_j = zones.get(j);

                double d = dCalc.distance(z_i.getGeometry().getCentroid(), z_j.getGeometry().getCentroid());
                if(d < threshold) {
                    m.set(i, j, null);
                }
            }
        }

        NumericMatrixIO.write(m, out);
    }
}
