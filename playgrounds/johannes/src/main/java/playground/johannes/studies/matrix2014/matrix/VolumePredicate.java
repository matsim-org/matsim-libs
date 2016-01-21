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

import playground.johannes.synpop.matrix.Matrix;

/**
 * @author jillenberger
 */
public class VolumePredicate implements ODPredicate<String, Double> {

    private final double threshold;

    public VolumePredicate(double threshold) {
        this.threshold = threshold;
    }

    @Override
    public boolean test(String row, String col, Matrix<String, Double> matrix) {
        Double vol = matrix.get(row, col);
        if(vol == null)
            return false;
        else
            return (vol >= threshold);
    }
}
