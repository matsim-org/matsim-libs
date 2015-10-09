/* *********************************************************************** *
 * project: org.matsim.*
 * Descretizer.java
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
 * A <tt>Discretizer</tt> "rounds" values to values of defined bins (buckets/categories). For instance, use this class
 * to generate histograms.
 *
 * @author illenberger
 */
public interface Discretizer {

    /**
     * Returns the bin's value of which <tt>value</tt> is associated to.
     *
     * @param value the value to discretized
     * @return the bin's value
     */
    double discretize(double value);

    /**
     * Returns the bin's index of which <tt>values</tt> is associated to.
     *
     * @param value the value to discretize
     * @return the index of the bin
     */
    int index(double value);

    /**
     * Returns the bin's width of which <tt>value</tt> is associated to.
     *
     * @param value the value to discretize
     * @return returns the width of the bin.
     */
    double binWidth(double value);

}
