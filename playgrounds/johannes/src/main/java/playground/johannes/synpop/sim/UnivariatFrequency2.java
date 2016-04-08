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

import gnu.trove.map.TDoubleDoubleMap;
import org.apache.log4j.Logger;
import org.matsim.contrib.common.stats.Discretizer;
import playground.johannes.synpop.analysis.Predicate;
import playground.johannes.synpop.data.CommonKeys;
import playground.johannes.synpop.data.Person;
import playground.johannes.synpop.data.Segment;
import playground.johannes.synpop.sim.data.CachedElement;
import playground.johannes.synpop.sim.data.CachedPerson;
import playground.johannes.synpop.sim.data.Converters;
import playground.johannes.synpop.sim.data.DoubleConverter;
import playground.johannes.synpop.sim.util.DynamicArrayBuilder;
import playground.johannes.synpop.sim.util.DynamicDoubleArray;

import java.util.Collection;

/**
 * @author johannes
 */
public class UnivariatFrequency2 implements Hamiltonian, AttributeChangeListener {

    private final static Logger logger = Logger.getLogger(UnivariatFrequency2.class);

    private final DynamicDoubleArray refFreq;

    private final HistogramBuilder histBuilder;

    private DynamicDoubleArray simFreq;

    private double scaleFactor;

//    private double refSum;

//    private double simSum;

    private double binCount;

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

    private final double noRefValError = 1e6;

    private long iterations = 0;

    private final long resetInterval = (long)1e7;

    private final boolean debugMode = false;

    public UnivariatFrequency2(TDoubleDoubleMap refHist, HistogramBuilder histBuilder,
                               String attrKey, Discretizer discretizer, boolean useWeights, boolean absoluteMode) {
        this.histBuilder = histBuilder;
        this.discretizer = discretizer;
        this.attrKey = attrKey;
        this.absoluteMode = absoluteMode;
        this.useWeights = useWeights;

        if(useWeights) weightKey = Converters.register(CommonKeys.PERSON_WEIGHT, DoubleConverter.getInstance());

        refFreq = DynamicArrayBuilder.build(refHist, discretizer);
    }

    public void setPredicate(Predicate<Segment> predicate) {
        this.predicate = predicate;
    }

    public void setErrorExponent(double exponent) {
        this.errorExponent = exponent;
    }

    private void initHistogram(Collection<? extends Person> simPersons) {
        TDoubleDoubleMap simHist = histBuilder.build(simPersons);
        DynamicDoubleArray tempFreq = DynamicArrayBuilder.build(simHist, discretizer);

        if(simFreq != null) {
            for (int i = 0; i < tempFreq.size(); i++) {
                if (tempFreq.get(i) != simFreq.get(i))
                    logger.warn(String.format("Histogram out of sync: idx = %s, old = %s, new = %s",
                            i,
                            simFreq.get(i),
                            tempFreq.get(i)));
            }
        }

        simFreq = tempFreq;

        double refSum = 0;
        double simSum = 0;

        binCount = Math.max(simFreq.size(), refFreq.size());
        for (int i = 0; i < binCount; i++) {
            simSum += simFreq.get(i);
            refSum += refFreq.get(i);
        }

        scaleFactor = simSum/refSum;
    }

    private void initHamiltonian() {
        hamiltonianValue = 0;
//        double scaleFactor = simSum/refSum;

        for (int i = 0; i < binCount; i++) {
            double simVal = simFreq.get(i);
            double refVal = refFreq.get(i) * scaleFactor;


            hamiltonianValue += calculateError(simVal, refVal);
        }
    }


    @Override
    public void onChange(Object dataKey, Object oldValue, Object newValue, CachedElement element) {
        if(simFreq != null) {
            if (this.dataKey == null) this.dataKey = Converters.getObjectKey(attrKey);

            if (this.dataKey.equals(dataKey) && evaluatePredicate(element)) {
                double delta = 1.0;
                if (useWeights) delta = (Double) element.getData(weightKey);

                double diff1 = 0;
                if (oldValue != null) {
                    int bucket = discretizer.index((Double) oldValue);
                    diff1 = changeBucketContent(bucket, -delta);
                }

                double diff2 = 0;
                if (newValue != null) {
                    int bucket = discretizer.index((Double) newValue);
                    diff2 = changeBucketContent(bucket, delta);
                }

                hamiltonianValue += (diff1 + diff2);
            }
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
//        double scaleFactor = simSum/refSum;

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
        if(simFreq == null) {
            initHistogram(population);
            initHamiltonian();
        }

        iterations++;
        if(iterations % resetInterval == 0) {
            double h_old = hamiltonianValue;
            if(debugMode) initHistogram(population);
            initHamiltonian();
            if(h_old != hamiltonianValue)
                logger.trace(String.format("Reset hamiltonian: %s -> %s", h_old, hamiltonianValue));
        }

        return hamiltonianValue/binCount;
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
                else return noRefValError;
//                else return simVal/scaleFactor; //TODO: this should be invariant from the sample size of sim values.
                // Not sure if scaleFactor is the appropriate normalization...
            }
        }
    }

}
