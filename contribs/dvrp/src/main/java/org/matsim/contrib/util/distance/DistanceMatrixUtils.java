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

package org.matsim.contrib.util.distance;

import org.matsim.api.core.v01.BasicLocation;
import org.matsim.matrices.Matrix;


public class DistanceMatrixUtils
{
    public static Matrix calculateDistanceMatrix(DistanceCalculator calculator,
            Iterable<? extends BasicLocation<?>> fromLocations,
            Iterable<? extends BasicLocation<?>> toLocations)
    {
        Matrix matrix = new Matrix("distance", "distance");

        for (BasicLocation<?> from : fromLocations) {
            for (BasicLocation<?> to : toLocations) {
                double distance = calculator.calcDistance(from.getCoord(), to.getCoord());
                matrix.createEntry(from.getId().toString(), to.getId().toString(), distance);
            }
        }

        return matrix;
    }
}
