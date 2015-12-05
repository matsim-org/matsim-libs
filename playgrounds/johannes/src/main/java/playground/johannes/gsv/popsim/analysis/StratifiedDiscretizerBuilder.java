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

package playground.johannes.gsv.popsim.analysis;

import org.matsim.contrib.common.stats.Discretizer;
import org.matsim.contrib.common.stats.FixedSampleSizeDiscretizer;

/**
 * @author johannes
 */
public class StratifiedDiscretizerBuilder implements DiscretizerBuilder {

    public static final String DEFAULT_NAME = "stratified";

    private final int numBins;

    private final int minSize;

    private final String name;

    public StratifiedDiscretizerBuilder(int numBins, int minSize) {
        this(numBins, minSize, DEFAULT_NAME);
    }

    public StratifiedDiscretizerBuilder(int numBins, int minSize, String name) {
        this.numBins = numBins;
        this.minSize = minSize;
        this.name = name;
    }

    @Override
    public Discretizer build(double[] values) {
        return FixedSampleSizeDiscretizer.create(values, minSize, numBins);
    }

    @Override
    public String getName() {
        return name;
    }
}
