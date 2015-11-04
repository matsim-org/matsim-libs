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

package playground.johannes.gsv.popsim;

import gnu.trove.TDoubleFunction;
import gnu.trove.TObjectDoubleHashMap;

/**
 * @author johannes
 */
public class Histogram {

    public static TObjectDoubleHashMap<?> normalize(TObjectDoubleHashMap<?> histogram) {
        double sum = 0;
        double[] values = histogram.getValues();

        for (int i = 0; i < values.length; i++) {
            sum += values[i];
        }

        return normalize(histogram, sum);
    }

    public static TObjectDoubleHashMap<?> normalize(TObjectDoubleHashMap<?> histogram, double sum) {
        final double norm = 1 / sum;

        TDoubleFunction fct = new TDoubleFunction() {
            public double execute(double value) {
                return value * norm;
            }

        };

        histogram.transformValues(fct);

        return histogram;
    }
}
