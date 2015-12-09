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

package org.matsim.contrib.common.stats;

import gnu.trove.list.array.TDoubleArrayList;

import java.util.Arrays;

/**
 * An <tt>InterpolatingDiscretizer</tt> is a discretizer with fixed borders so that a border lies in the middle between
 * two neighbouring values. For instance, for a set of samples <tt>2, 2, 2, 4, 6, 6</tt> the borders would be <tt>3</tt>
 * and <tt>5</tt> and the bin values would be <tt>2, 4</tt> and <tt>6</tt>. Thas is, {@code
 * InterpolatingDiscretizer#discretize(4.2)} returns <tt>4.0</tt> and {@code InterpolatingDiscretizer#discretize(10)}
 * returns <tt>6</tt>.
 *
 * @author johannes
 */
public class InterpolatingDiscretizer implements Discretizer {

    private double[] binValues;

    private Discretizer borders;

    /**
     * Creates a new discretizer with borders and bin values set according to <tt>values</tt>.
     *
     * @param values a list of samples
     */
    public InterpolatingDiscretizer(double[] values) {
        Arrays.sort(values);
        TDoubleArrayList tmpBorders = new TDoubleArrayList();
        TDoubleArrayList tmpValues = new TDoubleArrayList();
        double low = values[0];
        double high;
        for (int i = 1; i < values.length; i++) {
            high = values[i];
            if (low < high) {
                tmpBorders.add(low + (high - low) / 2.0);
                tmpValues.add(low);
            }
            low = high;
        }
        tmpValues.add(values[values.length - 1]);

        borders = new FixedBordersDiscretizer(tmpBorders.toArray());
        binValues = tmpValues.toArray();
    }

    /**
     * Discretizes <tt>value</tt> and returns the middle of the corresponding bin.
     *
     * @param value the value to discretize
     * @return the discretized value
     */
    @Override
    public double discretize(double value) {
        int idx = (int) index(value);
        return binValues[idx];
    }

    /**
     * Returns the bin's index in which <tt>value</tt> is associated to.
     *
     * @param value the value to discretize
     * @return the bin's index
     */
    @Override
    public int index(double value) {
        return borders.index(value);
    }

    /**
     * Returns the bin's width in which <tt>value</tt> is associated to.
     *
     * @param value the value to discretize
     * @return the bin's width
     * @see FixedBordersDiscretizer#binWidth(double)
     */
    @Override
    public double binWidth(double value) {
        return borders.binWidth(value);
    }

}
