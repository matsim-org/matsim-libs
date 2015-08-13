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

import gnu.trove.TDoubleArrayList;
import gnu.trove.TIntArrayList;
import gnu.trove.TIntDoubleHashMap;
import gnu.trove.TIntIntHashMap;
import playground.johannes.gsv.synPop.sim3.Hamiltonian;
import playground.johannes.sna.math.Discretizer;
import playground.johannes.synpop.data.Person;
import playground.johannes.synpop.data.PlainPerson;
import playground.johannes.synpop.sim.data.CachedPerson;
import playground.johannes.synpop.sim.util.DynamicDoubleArray;
import playground.johannes.synpop.sim.util.DynamicIntArray;

import java.util.Set;

/**
 * @author johannes
 */
public class MultivariatMean implements Hamiltonian, AttributeChangeListener {

    private Object xDataKey;

    private Object yDataKey;

    private Discretizer xDataDiscr;

    private void init() {

    }
    private void calculateBuckets(Set<? extends Person> persons, DynamicDoubleArray sums, DynamicIntArray counts,
                                  String xAttrKey, String yAttrKey) {
        TIntDoubleHashMap sumBuckets = new TIntDoubleHashMap();
        TIntIntHashMap countBuckets = new TIntIntHashMap();

        for(Person person : persons) {
            String xValStr = person.getAttribute(xAttrKey);
            String yValStr = person.getAttribute(yAttrKey);

            if(xValStr != null && yValStr != null) {
                double xVal = Double.parseDouble(xValStr);
                double yVal = Double.parseDouble(yValStr);

                int bucketIdx = (int)xDataDiscr.index(xVal);
            }
        }
    }

    @Override
    public void onChange(Object dataKey, double oldValue, double newValue, CachedPerson person) {

    }

    @Override
    public double evaluate(PlainPerson person) {
        return 0;
    }
}
