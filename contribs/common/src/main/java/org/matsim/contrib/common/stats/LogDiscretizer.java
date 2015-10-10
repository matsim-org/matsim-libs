/* *********************************************************************** *
 * project: org.matsim.*
 * PowerDiscretizer.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

/**
 * A discretizer with logarithmically scaled bins.
 *
 * @author illenberger
 */
public class LogDiscretizer implements Discretizer {

    private double base;

    private double upperBound;

    private double lowerBound;

    /**
     * Creates a new discretizer. Values less than 1 are rounded up to 1.
     *
     * @param base the base of the logarithm
     */
    public LogDiscretizer(double base) {
        this(base, Double.POSITIVE_INFINITY, 1.0);
    }

    /**
     * Creates a new discretizer where values greater than <tt>upperBound</tt> are rounded down to <tt>upperBound</tt>.
     * Values less than 1 are rounded up to 1.
     *
     * @param base       the base of the logarithm
     * @param upperBound the upper bound
     */
    public LogDiscretizer(double base, double upperBound) {
        this(base, upperBound, 1.0);
    }

    /**
     * Creates a new discretizer where values greater than <tt>upperBound</tt> are rounded down to <tt>upperBound</tt>
     * and values less than <tt>lowerBound</tt> are rounded up to <tt>lowerBound</tt>.
     *
     * @param base       the base of the logarithm
     * @param upperBound the upper bound
     * @param lowerBound the lower bound
     */
    public LogDiscretizer(double base, double upperBound, double lowerBound) {
        this.base = base;
        this.upperBound = upperBound;
        this.lowerBound = lowerBound;
    }

    /**
     * @param value a value
     * @returns rounds <tt>value</tt> up to the upper bin border.
     */
    @Override
    public double discretize(double value) {
        value = checkValue(value);
        double bin = index(value);
        return Math.pow(base, bin);
    }

    /**
     * @param value a value
     * @return the width of the bin of <tt>value</tt>. The width of the 0-th bin is 1.
     */
    @Override
    public double binWidth(double value) {
        double bin = index(value);
        if (bin == 0)
            return Math.pow(base, bin);
        else
            return Math.pow(base, bin) - Math.pow(base, bin - 1);
    }

    /**
     * @see {@link Discretizer#index(double)}
     */
    @Override
    public int index(double value) {
        value = checkValue(value);
        return (int) Math.ceil(Math.log(value) / Math.log(base));
    }

    private double checkValue(double value) {
        value = Math.max(lowerBound, value);
        value = Math.min(upperBound, value);
        return value;

    }
}
