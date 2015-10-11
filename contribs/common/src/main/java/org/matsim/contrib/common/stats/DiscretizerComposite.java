/* *********************************************************************** *
 * project: org.matsim.*
 * DiscretizerComposite.java
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
 * A composition of two discretizers. Values are first discretized with the first discretizer, the bin indicies are the
 * discretized with the second discretizer, e.g., meters to kilometers and then kilometers in log2 bins.
 *
 * @author illenberger
 */
public class DiscretizerComposite implements Discretizer {

    private Discretizer first;

    private Discretizer second;

    /**
     * Creates a new discretizer composite.
     *
     * @param first  the first discretizer.
     * @param second the second discretizer.
     */
    public DiscretizerComposite(Discretizer first, Discretizer second) {
        this.first = first;
        this.second = second;
    }

    /**
     * @see {@link Discretizer#binWidth(double)}
     */
    @Override
    public double binWidth(double value) {
        double bin = first.index(value);
        return second.binWidth(bin) * first.binWidth(value);
    }

    /**
     * @see {@link Discretizer#discretize(double)}
     */
    @Override
    public double discretize(double value) {
        double bin = first.index(value);
        bin = second.discretize(bin);
        return bin * first.binWidth(value);
    }

    /**
     * @see {@link Discretizer#index(double)}
     */
    @Override
    public int index(double value) {
        double bin = first.index(value);
        return second.index(bin);
    }

}
