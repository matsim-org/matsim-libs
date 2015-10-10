/* *********************************************************************** *
 * project: org.matsim.*
 * UnivariatePiStatistic.java
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

import org.apache.commons.math.stat.descriptive.UnivariateStatistic;

/**
 * An extension of {@link UnivariateStatistic} that allows to associate pi-values to the samples.
 *
 * @author illenberger
 */
public interface UnivariatePiStatistic extends UnivariateStatistic {

    /**
     * Sets the pi-values associated to the samples. The pi-value array must have same length and order as the array of
     * samples.
     *
     * @param piValues the pi-values
     */
    void setPiValues(double[] piValues);

}
