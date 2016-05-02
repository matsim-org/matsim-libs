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

package playground.johannes.synpop.sim;

import gnu.trove.iterator.TIntDoubleIterator;
import gnu.trove.map.TIntDoubleMap;
import gnu.trove.map.hash.TIntDoubleHashMap;
import org.matsim.contrib.common.stats.Discretizer;
import playground.johannes.synpop.data.Attributable;
import playground.johannes.synpop.data.CommonKeys;
import playground.johannes.synpop.sim.data.CachedElement;
import playground.johannes.synpop.sim.data.CachedPerson;
import playground.johannes.synpop.sim.data.Converters;
import playground.johannes.synpop.sim.data.DoubleConverter;
import playground.johannes.synpop.sim.util.DynamicDoubleArray;

import java.util.Collection;
import java.util.Set;

/**
 * @author johannes
 */
public class BivariatMean implements Hamiltonian, AttributeChangeListener {

    private Object xDataKey;

    private Object yDataKey;

    private final String xAttrKey;

    private final String yAttrKey;

    private final Discretizer xDataDiscr;

    private DynamicDoubleArray referenceValues;

    private DynamicDoubleArray bucketSums;

    private DynamicDoubleArray bucketCounts;

    private double hamiltonianValue;

    private final boolean useWeights;

    private Object weightKey;

    private int binCount;

    public BivariatMean(Set<? extends Attributable> refElements, Set<? extends Attributable> simElements, String
            xAttrKey, String yAttrKey, Discretizer xDataDiscr) {
        this(refElements, simElements, xAttrKey, yAttrKey, xDataDiscr, false);

    }

    public BivariatMean(Set<? extends Attributable> refElements, Set<? extends Attributable> simElements, String
            xAttrKey, String yAttrKey, Discretizer xDataDiscr, boolean useWeights) {
        this.xAttrKey = xAttrKey;
        this.yAttrKey = yAttrKey;
        this.xDataDiscr = xDataDiscr;
        this.useWeights = useWeights;

        if(useWeights) weightKey = Converters.register(CommonKeys.PERSON_WEIGHT, DoubleConverter.getInstance());

        initReferenceValues(refElements, xAttrKey, yAttrKey, useWeights);
        initSimulationValues(simElements, xAttrKey, yAttrKey, useWeights);

        // Calculate the initial hamiltonian value.
        initHamiltonian();

    }

    public BivariatMean(TIntDoubleMap reference, Set<? extends Attributable> simElements, String xAttrKey, String
            yAttrKey, Discretizer xDataDiscr, boolean useWeights) {
        this.xAttrKey = xAttrKey;
        this.yAttrKey = yAttrKey;
        this.xDataDiscr = xDataDiscr;
        this.useWeights = useWeights;

        if(useWeights) weightKey = Converters.register(CommonKeys.PERSON_WEIGHT, DoubleConverter.getInstance());

//        initReferenceValues(refElements, xAttrKey, yAttrKey, useWeights);
        referenceValues = new DynamicDoubleArray(1, Double.NaN);
        TIntDoubleIterator it = reference.iterator();
        for(int i = 0; i < reference.size(); i++) {
            it.advance();
            referenceValues.set(it.key(), it.value());
        }

        initSimulationValues(simElements, xAttrKey, yAttrKey, useWeights);

        // Calculate the initial hamiltonian value.
        initHamiltonian();
    }

    private void initHamiltonian() {
        hamiltonianValue = 0;
        binCount = Math.max(referenceValues.size(), bucketCounts.size());
        for(int i = 0; i < binCount; i++) {
            hamiltonianValue += calculateDiff(i);
        }
    }

    private void initReferenceValues(Set<? extends Attributable> elements, String xAttrKey, String yAttrKey, boolean useWeigths) {
        referenceValues = new DynamicDoubleArray(1, Double.NaN);

        DynamicDoubleArray sums = new DynamicDoubleArray(1, Double.NaN);
        DynamicDoubleArray counts = new DynamicDoubleArray(1, -1);

        calculateBuckets(elements, sums, counts, xAttrKey, yAttrKey, useWeigths);

        for(int i = 0; i < sums.size(); i++) {
            double sum = sums.get(i);
            double cnt = counts.get(i);
            if(!Double.isNaN(sum) && cnt > 0) {
                referenceValues.set(i, sum/(double)cnt);
            }
        }
    }

    private void initSimulationValues(Set<? extends Attributable> elements, String xAttrKey, String yAttrKey, boolean useWeights) {
        bucketSums = new DynamicDoubleArray(1, Double.NaN);
        bucketCounts = new DynamicDoubleArray(1, -1);

        calculateBuckets(elements, bucketSums, bucketCounts, xAttrKey, yAttrKey, useWeights);
    }

    private void calculateBuckets(Set<? extends Attributable> elements, DynamicDoubleArray sums, DynamicDoubleArray counts,
                                  String xAttrKey, String yAttrKey, boolean useWeights) {
        TIntDoubleHashMap sumBuckets = new TIntDoubleHashMap();
        TIntDoubleHashMap countBuckets = new TIntDoubleHashMap();

        for(Attributable element : elements) {
            String xValStr = element.getAttribute(xAttrKey);
            String yValStr = element.getAttribute(yAttrKey);

            if(xValStr != null && yValStr != null) {
                double xVal = Double.parseDouble(xValStr);
                double yVal = Double.parseDouble(yValStr);

                double weight = 1.0;
                if(useWeights) {
                    weight = Double.parseDouble(element.getAttribute(CommonKeys.PERSON_WEIGHT));
                    yVal = yVal * weight;
                }

                int bucketIdx = xDataDiscr.index(xVal);

                sumBuckets.adjustOrPutValue(bucketIdx, yVal, yVal);
                countBuckets.adjustOrPutValue(bucketIdx, weight, weight);
            }
        }

        TIntDoubleIterator it = sumBuckets.iterator();
        for(int i = 0; i < sumBuckets.size(); i++) {
            it.advance();
            int bucketIndex = it.key();
            double sum = it.value();
            double cnt = countBuckets.get(bucketIndex);

            sums.set(bucketIndex, sum);
            counts.set(bucketIndex, cnt);
        }
    }

    @Override
    public void onChange(Object dataKey, Object oldValue, Object newValue, CachedElement person) {
        if(xDataKey == null) xDataKey = Converters.getObjectKey(xAttrKey);
        if(yDataKey == null) yDataKey = Converters.getObjectKey(yAttrKey);

        if(yDataKey.equals(dataKey)) {
            onYValueChange((Double)oldValue, (Double)newValue, person);
        } else if(xDataKey.equals(dataKey)) {
            onXValueChange((Double) oldValue, (Double) newValue, person);
        }
    }

    private void onXValueChange(double oldValue, double newValue, CachedElement person) {
        Double yVal = (Double)person.getData(yDataKey);
        int oldBucketIndex = xDataDiscr.index(oldValue);

        double weight = 1.0;
        if(useWeights) weight = (Double)person.getData(weightKey);
        yVal = yVal * weight;

        double hValue1 = changeBucketContent(oldBucketIndex, yVal, -weight);

        int newBucketIndex = xDataDiscr.index(newValue);
        double hValue2 = changeBucketContent(newBucketIndex, yVal, weight);

        hamiltonianValue += (hValue1 + hValue2);
    }

    private double changeBucketContent(int bucketIndex, double value, double delta) {
        double oldDiff = calculateDiff(bucketIndex);

        double sum = bucketSums.get(bucketIndex);
        double count = bucketCounts.get(bucketIndex);

        if(delta > 0) {
            bucketSums.set(bucketIndex, sum + value);
            bucketCounts.set(bucketIndex, count + delta);
        } else {
            bucketSums.set(bucketIndex, sum - value);
            bucketCounts.set(bucketIndex, count + delta);
        }

        double newDiff = calculateDiff(bucketIndex);

        return newDiff - oldDiff;
    }

    private void onYValueChange(double oldValue, double newValue, CachedElement element) {
        Double xVal = (Double)element.getData(xDataKey);
        int bucketIndex = xDataDiscr.index(xVal);

        double oldDiff = calculateDiff(bucketIndex);

        double weight = 1.0;
        if(useWeights) weight = (Double)element.getData(weightKey);

        double delta = (newValue - oldValue) * weight;

        double sum = bucketSums.get(bucketIndex);
        bucketSums.set(bucketIndex, sum + delta);

        double newDiff = calculateDiff(bucketIndex);

        hamiltonianValue += newDiff - oldDiff;
    }

//    private long evalCount = 0;
    @Override
    public double evaluate(Collection<CachedPerson> population) {
//        evalCount++;
//        if(evalCount % 10000000 == 0) {
//            double h_before = hamiltonianValue;
//            initHamiltonian();
//            double h_after = hamiltonianValue;
//            System.out.println(String.format("Reset hamiltonian: before = %s, after = %s", h_before/(double)
//                    binCount, h_after/(double)binCount));
//        }
        return hamiltonianValue/(double)binCount;
    }

    private double calculateDiff(int bucketIndex) {
        double refValue = referenceValues.get(bucketIndex);
        if(Double.isNaN(refValue)) {
            // There is no reference value for this bucket. We cannot do any comparison.
            return 0.0;
        } else {
            double sum = bucketSums.get(bucketIndex);
            double cnt = bucketCounts.get(bucketIndex);

            if(!Double.isNaN(sum) && cnt > 0) {
                double simValue = sum/cnt;

                if(refValue == 0)
                    if(simValue == 0) return 0;
                    else return Math.abs(simValue);
                else {
                    return Math.abs(simValue - refValue)/Math.abs(refValue);
                }
            } else {
                return 0.0;
            }
        }
    }

    public double[] getBinErrors() {
        binCount = Math.max(referenceValues.size(), bucketCounts.size());
        double[] errors = new double[binCount];
        for(int i = 0; i < binCount; i++) {
            errors[i] = calculateDiff(i);
        }
        return errors;
    }
}
