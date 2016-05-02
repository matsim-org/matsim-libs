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

import org.matsim.contrib.common.stats.Discretizer;
import playground.johannes.synpop.analysis.Predicate;
import playground.johannes.synpop.data.Attributable;
import playground.johannes.synpop.data.CommonKeys;
import playground.johannes.synpop.data.Segment;
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
public class UnivariatFrequency implements Hamiltonian, AttributeChangeListener {

    private final DynamicDoubleArray refFreq;

    private final DynamicDoubleArray simFreq;

    private final double scaleFactor;

    private final double binCount;

    private Object dataKey;

    private final String attrKey;

    private final Discretizer discretizer;

    private double hamiltonianValue;

    private final boolean absoluteMode;

    private boolean useWeights;

    private Object weightKey;

    private Predicate<Segment> predicate;

    private final Object PREDICATE_RESULT_KEY = new Object();

    private double errorExponent = 1.0;

    public UnivariatFrequency(Set<? extends Attributable> refElements, Set<? extends Attributable> simElements,
                              String attrKey, Discretizer discretizer) {
        this(refElements, simElements, attrKey, discretizer, false, false);
    }

    public UnivariatFrequency(Set<? extends Attributable> refElements, Set<? extends Attributable> simElements,
                              String attrKey, Discretizer discretizer, boolean useWeights) {
        this(refElements, simElements, attrKey, discretizer, useWeights, false);
    }

    public UnivariatFrequency(Set<? extends Attributable> refElements, Set<? extends Attributable> simElements,
                              String attrKey, Discretizer discretizer, boolean useWeights, boolean absoluteMode) {
        this.discretizer = discretizer;
        this.attrKey = attrKey;
        this.absoluteMode = absoluteMode;
        this.useWeights = useWeights;

        if(useWeights) weightKey = Converters.register(CommonKeys.PERSON_WEIGHT, DoubleConverter.getInstance());

        refFreq = initHistogram(refElements, attrKey, useWeights);
        simFreq = initHistogram(simElements, attrKey, useWeights);

        double refSum = 0;
        double simSum = 0;

        binCount = Math.max(simFreq.size(), refFreq.size());

        for (int i = 0; i < binCount; i++) {
            simSum += simFreq.get(i);
            refSum += refFreq.get(i);
        }

        scaleFactor = simSum/refSum;


        for (int i = 0; i < binCount; i++) {
            double simVal = simFreq.get(i);
            double refVal = refFreq.get(i) * scaleFactor;

            hamiltonianValue += calculateError(simVal, refVal) / binCount;
        }
    }

    public void setPredicate(Predicate<Segment> predicate) {
        this.predicate = predicate;
    }

    public void setErrorExponent(double exponent) {
        this.errorExponent = exponent;
    }

    private DynamicDoubleArray initHistogram(Set<? extends Attributable> elements, String key, boolean useWeights) {
        DynamicDoubleArray array = new DynamicDoubleArray(1, 0);

        for (Attributable element : elements) {
            String strVal = element.getAttribute(key);

            if (strVal != null) {
                double value = Double.parseDouble(strVal);
                int bucket = discretizer.index(value);
                double weight = 1.0;

                if(useWeights) {
                    String strWeight = element.getAttribute(CommonKeys.PERSON_WEIGHT);
                    weight = Double.parseDouble(strWeight);
                }

                array.set(bucket, array.get(bucket) + weight);
            }
        }

        return array;
    }

    @Override
    public void onChange(Object dataKey, Object oldValue, Object newValue, CachedElement element) {
        if (this.dataKey == null) this.dataKey = Converters.getObjectKey(attrKey);

        if (this.dataKey.equals(dataKey) && evaluatePredicate(element)) {
            double delta = 1.0;
            if(useWeights) delta = (Double)element.getData(weightKey);

            int bucket = discretizer.index((Double) oldValue);
            double diff1 = changeBucketContent(bucket, -delta);

            bucket = discretizer.index((Double) newValue);
            double diff2 = changeBucketContent(bucket, delta);

            hamiltonianValue += (diff1 + diff2) / binCount;
        }
    }

    private boolean evaluatePredicate(CachedElement element) {
        if(predicate == null) return true;
        else {
            Boolean result = (Boolean) element.getData(PREDICATE_RESULT_KEY);
            if(result != null) return result;
            else {
                result = predicate.test((Segment) element);
                element.setData(PREDICATE_RESULT_KEY, result);
                return result;
            }
        }
    }

    private double changeBucketContent(int bucketIndex, double value) {
        double simVal = simFreq.get(bucketIndex);
        double refVal = refFreq.get(bucketIndex) * scaleFactor;
        double oldDiff = calculateError(simVal, refVal);

        simFreq.set(bucketIndex, simFreq.get(bucketIndex) + value);

        simVal = simFreq.get(bucketIndex);
        refVal = refFreq.get(bucketIndex) * scaleFactor;
        double newDiff = calculateError(simVal, refVal);

        return newDiff - oldDiff;
    }

    @Override
    public double evaluate(Collection<CachedPerson> population) {
        return hamiltonianValue;
    }

    private double calculateError(double simVal, double refVal) {
        if (absoluteMode) {
            return Math.abs(simVal - refVal);
        } else {
            if (refVal > 0) {
//                return Math.abs(simVal - refVal) / refVal;
                double err = Math.pow(Math.abs(simVal - refVal) / refVal, errorExponent);
                return err;
            } else {
                if (simVal == 0) return 0;
                else return simVal/scaleFactor; //TODO: this should be invariant from the sample size of sim values.
                // Not sure if scaleFactor is the appropriate normalization...
            }
        }
    }

}
