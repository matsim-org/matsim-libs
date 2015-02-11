/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package org.matsim.contrib.util.random;

import java.util.*;


public class WeightedRandomSelection<T>
{
    private List<Entry<T>> entryList = new ArrayList<>();
    private double totalWeight = 0;
    private UniformRandom uniform;


    public WeightedRandomSelection()
    {
        this(RandomUtils.getGlobalUniform());
    }


    public WeightedRandomSelection(UniformRandom uniform)
    {
        this.uniform = uniform;
    }


    public void add(T obj, double weight)
    {
        totalWeight += weight;
        entryList.add(new Entry<T>(obj, totalWeight));
    }


    public T select()
    {
        if (entryList.size() == 0) {
            return null;
        }

        double rnd = uniform.nextDouble(0, totalWeight);
        int idx = Collections.binarySearch(entryList, new Entry<T>(null, rnd));

        if (idx < 0) {
            idx = -idx - 1;
        }

        return entryList.get(idx).e;
    }


    public int size()
    {
        return entryList.size();
    }


    private static class Entry<E>
        implements Comparable<Entry<E>>
    {
        final private E e;
        final private double cumulativeWeight;


        private Entry(E e, double cumulativeWeight)
        {
            this.e = e;
            this.cumulativeWeight = cumulativeWeight;
        }


        public int compareTo(Entry<E> o)
        {
            double diff = this.cumulativeWeight - o.cumulativeWeight;
            return diff == 0 ? 0 : (diff > 0 ? 1 : -1);
        }
    }
}
