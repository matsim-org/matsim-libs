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

package playground.johannes.gsv.popsim;

import gnu.trove.*;
import playground.johannes.sna.math.Discretizer;
import playground.johannes.synpop.data.Person;
import playground.johannes.synpop.data.PlainPerson;
import playground.johannes.synpop.sim.Hamiltonian;

import java.util.Arrays;
import java.util.Set;

/**
 * @author johannes
 */
public class HistogramSync2D implements Hamiltonian, HistogramSync {

    private Object xAttKey;

    private Object yAttKey;

    private Discretizer xDiscretizer;

    private double[] refAvrs;

    private double[] sums;

    private int[] counts;

    private double hValue;

    public HistogramSync2D(Set<PlainPerson> refPop, Set<PlainPerson> simPop, Object xAttKey, Object yAttKey, Discretizer xDiscretizer) {
        this.xAttKey = xAttKey;
        this.yAttKey = yAttKey;
        this.xDiscretizer = xDiscretizer;

        TDoubleArrayList sumValues = new TDoubleArrayList();
        TIntArrayList countValues = new TIntArrayList();
        calcMeans(refPop, sumValues, countValues);
        refAvrs = new double[sumValues.size()];
        for(int i = 0; i < sumValues.size(); i++) {
            double cnt = countValues.get(i);
            if(cnt == 0) refAvrs[i] = 0; // better would be null
            else refAvrs[i] = sumValues.get(i)/cnt;
        }

        sumValues = new TDoubleArrayList();
        countValues = new TIntArrayList();
        calcMeans(simPop, sumValues, countValues);
        sums = sumValues.toNativeArray();
        counts = countValues.toNativeArray();

        hValue = calcFullDiff();
    }

    private void calcMeans(Set<PlainPerson> persons, TDoubleArrayList sumValues, TIntArrayList countValues) {
        TIntDoubleHashMap sumsMap = new TIntDoubleHashMap();
        TIntIntHashMap countsMap = new TIntIntHashMap();

        int maxIdx = 0;
        for(PlainPerson person : persons) {
            Double key = (Double)person.getUserData(xAttKey);
            Double value = (Double)person.getUserData(yAttKey);
            if(key != null && value != null) {
                int idx = (int) xDiscretizer.index(key);
                sumsMap.adjustOrPutValue(idx, value, value);
                countsMap.adjustOrPutValue(idx, 1, 1);

                maxIdx = Math.max(maxIdx, idx);
            }
        }

//        sumValues = new double[sumsMap.size()];
//        countValues = new int[sumsMap.size()];
        TIntDoubleIterator it = sumsMap.iterator();
        for (int i = 0; i < maxIdx; i++) {
            sumValues.add(sumsMap.get(i));
            countValues.add(countsMap.get(i));
        }
    }

    private double getIdx(int idx, double[] values) {
        if(values.length <= idx + 1) {
            return 0;
        } else {
            return values[idx];
        }
    }

    private double[] setIdx(int idx, double value, double[] values) {
        double[] newValues = values;
        if(values.length <= idx + 1) {
            newValues = Arrays.copyOf(values, (int)(values.length * 1.5));
        }

        newValues[idx] = value;

        return newValues;
    }

    private int getIdx(int idx, int[] values) {
        if(values.length <= idx + 1) {
            return 0;
        } else {
            return values[idx];
        }
    }

    private int[] setIdx(int idx, int value, int[] values) {
        int[] newValues = values;
        if(values.length <= idx + 1) {
            newValues = Arrays.copyOf(values, (int)(values.length * 1.5));
        }

        newValues[idx] = value;

        return newValues;
    }

    public void notifyChange(Object attKey, double oldValue, double newValue, PlainPerson person) {
        if(attKey.equals(yAttKey)) {
            Double xVal = (Double) person.getUserData(xAttKey);
            int idx = (int)xDiscretizer.index(xVal);

            double oldDiff = calcBinDiff(idx);

            double delta = newValue - oldValue;
//            sums[idx] += delta;
            sums = setIdx(idx, getIdx(idx, sums) + delta, sums);

            double newDiff = calcBinDiff(idx);

            hValue += newDiff - oldDiff;
        } else if(attKey.equals(xAttKey)) {
            Double yVal = (Double)person.getUserData(yAttKey);

            int idxOld = (int)xDiscretizer.index(oldValue);
            double oldDiff1 = calcBinDiff(idxOld);
//            sums[idxOld] -= yVal;
            sums = setIdx(idxOld, getIdx(idxOld, sums) - yVal, sums);
//            counts[idxOld]--;
            int cnt = getIdx(idxOld, counts) - 1;
            counts = setIdx(idxOld, cnt, counts);
            double newDiff1 = calcBinDiff(idxOld);
            double diff1 = newDiff1 - oldDiff1;

            int idxNew = (int)xDiscretizer.index(newValue);
            double oldDiff2 = calcBinDiff(idxNew);
//            sums[idxNew]+= yVal;
            sums = setIdx(idxNew, getIdx(idxNew, sums) + yVal, sums);
//            counts[idxNew]++;
            cnt = getIdx(idxNew, counts) + 1;
            counts = setIdx(idxNew, cnt, counts);
            double newDiff2 = calcBinDiff(idxNew);
            double diff2 = newDiff2 - oldDiff2;

            hValue += diff1 + diff2;
        }
    }

    private double calcBinDiff(int idx) {
//        return refAvrs[idx] - (sums[idx] / (double)counts[idx]);
        double refAvr = getIdx(idx, refAvrs);
        double sum = getIdx(idx, sums);
        double cnt = (double)getIdx(idx, counts);
        double simAvr = sum/cnt;

        if(cnt == 0) simAvr = 0;
        return Math.abs(refAvr - simAvr);
    }


    @Override
    public double evaluate(Person person) {
        return hValue;
//        return calcFullDiff();
    }

    private double calcFullDiff() {
        double sum = 0;
        for(int i = 0; i < refAvrs.length; i++) {
            sum += calcBinDiff(i);
        }
        return sum;
    }
}
