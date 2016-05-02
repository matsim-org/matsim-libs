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

import com.vividsolutions.jts.geom.Point;
import org.matsim.contrib.common.gis.CartesianDistanceCalculator;
import org.matsim.contrib.common.gis.DistanceCalculator;
import org.matsim.contrib.common.util.ProgressLogger;
import playground.johannes.studies.matrix2014.matrix.io.VisumOMatrixReader;
import playground.johannes.synpop.gis.Zone;
import playground.johannes.synpop.gis.ZoneCollection;
import playground.johannes.synpop.gis.ZoneGeoJsonIO;
import playground.johannes.synpop.matrix.NumericMatrix;

import java.io.IOException;
import java.util.Set;

/**
 * @author johannes
 */
public class CalcMileage {

    public static void main(String args[]) throws IOException {
        NumericMatrix m = new NumericMatrix();
        VisumOMatrixReader.read(m, "/home/johannes/gsv/miv-matrix/deploy/r33883/modena/miv.txt");
        ZoneCollection zones = ZoneGeoJsonIO.readFromGeoJSON("/home/johannes/gsv/gis/modena/geojson/zones.de.gk3.geojson",
                "NO", null);

        DistanceCalculator distCalc = CartesianDistanceCalculator.getInstance();

        double mileage = 0;
        double volSum = 0;

        Set<String> keys = m.keys();
        ProgressLogger.init(keys.size(), 2, 10);
        for(String i : keys) {
            Zone zi = zones.get(i);
            if(zi != null ) {
                Point pi = zi.getGeometry().getCentroid();
                   for (String j : keys) {

                    Zone zj = zones.get(j);
                    if (zj != null) {
                        double d = distCalc.distance(pi, zj.getGeometry().getCentroid());
                        if (d >= 50000) {
                            Double vol = m.get(i, j);
                            if (vol != null) {
                                volSum += vol;
                                mileage += (vol * d);
                            }
                        }
                    }
                }
            }
            ProgressLogger.step();
        }

        System.out.print("Mileage: " + mileage + ", persons: " + volSum);
    }
}
