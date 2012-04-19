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

package playground.michalm.demand;

import java.util.*;

import cern.jet.random.Uniform;
import cern.jet.random.engine.MersenneTwister;


public class Distribution<T>
{
    private List<Entry<T>> entryList = new ArrayList<Entry<T>>();
    private double[] cumulShares;

    private Uniform uniform = new Uniform(new MersenneTwister(new Date()));
    private double totalShare = 0;


    public void add(T obj, double share)
    {
        entryList.add(new Entry<T>(obj, share));
        totalShare += share;
        cumulShares = null;// invalidate shares array after modification
    }


    public T draw()
    {
        if (cumulShares == null) {
            int size = entryList.size();
            cumulShares = new double[size];

            int shareSum = 0;
            for (int i = 0; i < size; i++) {
                shareSum += entryList.get(i).share;
                cumulShares[i] = shareSum;
            }
        }

        double rnd = uniform.nextDoubleFromTo(0, totalShare);
        int idx = Arrays.binarySearch(cumulShares, rnd);

        if (idx < 0) {
            idx = -idx - 1;
        }

        return entryList.get(idx).e;
    }


    private class Entry<E>
    {
        E e;
        double share;


        public Entry(E e, double share)
        {
            this.e = e;
            this.share = share;
        }
    }
}
