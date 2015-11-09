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
package playground.johannes.gsv.popsim.analysis;

import org.apache.commons.math.stat.StatUtils;

import java.util.List;

/**
 * @author jillenberger
 */
public class StatsContainer {

    private final String name;

    private Double mean;

    private Double median;

    private Double min;

    private Double max;

    private Integer N;

    private Double variance;

    public StatsContainer(String name) {
        this.name = name;
    }

    public StatsContainer(String name, double[] values) {
        this(name);
        mean = StatUtils.mean(values);
        median = StatUtils.percentile(values, 50);
        min = StatUtils.min(values);
        max = StatUtils.max(values);
        N = values.length;
        variance = StatUtils.variance(values);
    }

    public String getName() {
        return name;
    }

    public Double getMean() {
        return mean;
    }

    public Double getMedian() {
        return median;
    }

    public Double getMin() {
        return min;
    }

    public Double getMax() {
        return max;
    }

    public Integer getN() {
        return N;
    }

    public Double getVariance() {
        return variance;
    }
}
