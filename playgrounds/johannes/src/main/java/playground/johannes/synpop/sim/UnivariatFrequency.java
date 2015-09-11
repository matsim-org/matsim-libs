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

import playground.johannes.sna.math.Discretizer;
import playground.johannes.synpop.data.Attributable;
import playground.johannes.synpop.sim.data.CachedElement;
import playground.johannes.synpop.sim.data.CachedPerson;
import playground.johannes.synpop.sim.data.Converters;
import playground.johannes.synpop.sim.util.DynamicIntArray;

import java.util.Collection;
import java.util.Set;

/**
 * @author johannes
 */
public class UnivariatFrequency implements Hamiltonian, AttributeChangeListener {

    private final DynamicIntArray refFreq;

    private final DynamicIntArray simFreq;

    private final double scaleFactor;

    private final double normFactor;

    private Object dataKey;

    private final String attrKey;

    private final Discretizer discretizer;

    private double hamiltonianValue;

    public UnivariatFrequency(Set<? extends Attributable> refElements, Set<? extends Attributable> simElements,
                              String attrKey, Discretizer discretizer) {
        this.discretizer = discretizer;
        this.attrKey = attrKey;

        refFreq = initHistogram(refElements, attrKey);
        simFreq = initHistogram(simElements, attrKey);

        scaleFactor = simElements.size()/(double)refElements.size();
        normFactor = simElements.size();

        int size = Math.max(simFreq.size(), refFreq.size());
        for(int i = 0; i < size; i++) {
            double simVal = simFreq.get(i);
            double refVal = refFreq.get(i) * scaleFactor;

            hamiltonianValue += Math.abs(simVal - refVal)/ normFactor;
        }
    }

    private DynamicIntArray initHistogram(Set<? extends Attributable> elements, String key) {
        DynamicIntArray array = new DynamicIntArray(12, 0);

        for(Attributable element : elements) {
            String strVal = element.getAttribute(key);
            if(strVal != null) {
                double value = Double.parseDouble(strVal);
                int bucket = discretizer.index(value);
                array.set(bucket, array.get(bucket) + 1);
            }
        }

        return array;
    }

    @Override
    public void onChange(Object dataKey, Object oldValue, Object newValue, CachedElement person) {
        if(this.dataKey == null) this.dataKey = Converters.getObjectKey(attrKey);

        if(this.dataKey.equals(dataKey)) {
            int bucket = discretizer.index((Double)oldValue);
            double diff1 = changeBucketContent(bucket, -1);

            bucket = discretizer.index((Double)newValue);
            double diff2 = changeBucketContent(bucket, 1);

            hamiltonianValue += (diff1 + diff2)/ normFactor;
        }
    }

    private double changeBucketContent(int bucketIndex, int value) {
        double simVal = simFreq.get(bucketIndex);
        double refVal = refFreq.get(bucketIndex) * scaleFactor;
        double oldDiff = Math.abs(simVal - refVal);

        simFreq.set(bucketIndex, simFreq.get(bucketIndex) + value);

        simVal = simFreq.get(bucketIndex);
        refVal = refFreq.get(bucketIndex) * scaleFactor;
        double newDiff = Math.abs(simVal - refVal);

        return newDiff - oldDiff;
    }

    @Override
    public double evaluate(Collection<CachedPerson> population) {
        return hamiltonianValue;
    }
}
