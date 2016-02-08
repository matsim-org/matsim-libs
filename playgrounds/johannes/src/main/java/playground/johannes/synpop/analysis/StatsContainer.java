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
package playground.johannes.synpop.analysis;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.math.stat.StatUtils;
import org.matsim.contrib.common.collections.CollectionUtils;
import org.matsim.contrib.common.stats.WeightedSampleMean;

import java.util.*;

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

    private Double wsum;

    private Integer nullValues;

    private Double variance;

    public StatsContainer(String name) {
        this.name = name;
    }

    public StatsContainer(String name, double[] values) {
        this(name);
        init(values);
    }

    public StatsContainer(String name, double[] values, double[] weights) {
        this(name);
        init(values, weights);
    }

    public StatsContainer(String name, List<Double> values) {
        this(name);
        init(CollectionUtils.toNativeArray(values));

        nullValues = new Integer(0);
        for(Double value : values) {
            if(value == null) nullValues++;
        }
    }

    public StatsContainer(String name, List<Double> values, List<Double> weights) {
        this(name);
        List<double[]> valuesList = playground.johannes.studies.matrix2014.analysis.CollectionUtils.toNativeArray(values, weights);
        init(valuesList.get(0), valuesList.get(1));

        nullValues = new Integer(0);
        for(Double value : values) {
            if(value == null) nullValues++;
        }
    }

    private void init(double[] values) {
        mean = StatUtils.mean(values);
//        median = StatUtils.percentile(values, 50);
        median = calculateMedian(values);
        min = StatUtils.min(values);
        max = StatUtils.max(values);
        N = values.length;
        variance = StatUtils.variance(values);
    }

    private void init(double[] values, double[] weights) {
        WeightedSampleMean wsm = new WeightedSampleMean();
        median = calculateWeightedMedian(values, weights);

        for(int i = 0; i < weights.length; i++) weights[i] = 1/weights[i];
        wsm.setPiValues(weights);
        mean = wsm.evaluate(values);


        min = StatUtils.min(values);
        max = StatUtils.max(values);
        N = values.length;
        wsum = StatUtils.sum(weights);
    }

    public String getName() {
        return name;
    }

    public void setMean(double mean) {
        this.mean = mean;
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

    public Double getWsum() {
        return wsum;
    }

    public Integer getNullValues() {
        return nullValues;
    }

    public Double getVariance() {
        return variance;
    }

    /*
    This is not a precise calculation of the median, yet should be ok for large samples sizes.
     */
    private double calculateMedian(double[] values) {
        if(values.length > 0) {
            double[] tempValues = Arrays.copyOf(values, values.length);
            Arrays.sort(tempValues);

            return tempValues[tempValues.length / 2];
        } else {
            return Double.NaN;
        }
    }

    private double calculateWeightedMedian(double[] values, double[] weights) {
        Set<Pair<Double, Double>> set = new TreeSet<>(new Comparator() {
            @Override
            public int compare(Object o1, Object o2) {
                Pair<Double, Double> p1 = (Pair)o1;
                Pair<Double, Double> p2 = (Pair)o2;

                int result = p1.getLeft().compareTo(p2.getLeft());
                if(result == 0) {
                    if(p1 == p2) result = 0;
                    else result = 1;
                }
                return result;
            }
        });

        double wsum = 0;
        for(int i = 0; i < values.length; i++) {
            set.add(new ImmutablePair<>(values[i], weights[i]));
            wsum += weights[i];
        }

        double middle = wsum/2.0;
        wsum = 0;
        for(Pair<Double, Double> pair : set) {
            wsum += pair.getRight();
            if(wsum >= middle) {
                return pair.getLeft();
            }
        }

        return Double.NaN;
    }
}
