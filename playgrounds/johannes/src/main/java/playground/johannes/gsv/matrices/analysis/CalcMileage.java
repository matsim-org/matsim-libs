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
import playground.johannes.gsv.zones.KeyMatrix;
import playground.johannes.gsv.zones.Zone;
import playground.johannes.gsv.zones.ZoneCollection;
import playground.johannes.gsv.zones.io.VisumOMatrixReader;
import playground.johannes.sna.util.ProgressLogger;
import playground.johannes.socialnetworks.gis.CartesianDistanceCalculator;
import playground.johannes.socialnetworks.gis.DistanceCalculator;

import java.io.IOException;
import java.security.Key;
import java.util.Set;

/**
 * @author johannes
 */
public class CalcMileage {

    public static void main(String args[]) throws IOException {
        KeyMatrix m = new KeyMatrix();
        VisumOMatrixReader.read(m, "/home/johannes/gsv/miv-matrix/deploy/r33883/modena/miv.txt");
        ZoneCollection zones = ZoneCollection.readFromGeoJSON("/home/johannes/gsv/gis/modena/geojson/zones.de.gk3.geojson", "NO");

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
