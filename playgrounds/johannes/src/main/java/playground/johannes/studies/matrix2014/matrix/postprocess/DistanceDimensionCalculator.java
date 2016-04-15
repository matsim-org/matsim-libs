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

import com.vividsolutions.jts.geom.Point;
import org.matsim.contrib.common.gis.DistanceCalculator;
import org.matsim.contrib.common.stats.Discretizer;
import playground.johannes.synpop.gis.ZoneCollection;
import playground.johannes.synpop.matrix.NumericMatrix;

import java.util.Map;

/**
 * @author johannes
 */
public class DistanceDimensionCalculator implements DimensionCalculator {

    private NumericMatrix distanceMatrix;

    private Discretizer discretizer;

    private DistanceCalculator calculator;

    private ZoneCollection zones;

    public DistanceDimensionCalculator(ZoneCollection zones, DistanceCalculator calculator, Discretizer discretizer) {
        this.zones = zones;
        this.calculator = calculator;
        this.discretizer = discretizer;
        distanceMatrix = new NumericMatrix();
    }

    @Override
    public String calculate(String origin, String destination, double volume, Map<String, String> dimensions) {
        Double d = distanceMatrix.get(origin, destination);
        if(d == null) {
            Point p_i = zones.get(origin).getGeometry().getCentroid();
            Point p_j = zones.get(destination).getGeometry().getCentroid();

            d = calculator.distance(p_i, p_j);

            distanceMatrix.add(origin, destination, d);
            distanceMatrix.add(destination, origin, d);
        }

        return String.valueOf(discretizer.discretize(d));
    }
}
