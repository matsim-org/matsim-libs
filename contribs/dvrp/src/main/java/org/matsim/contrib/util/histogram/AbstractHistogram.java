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

package org.matsim.contrib.util.histogram;

public abstract class AbstractHistogram<T>
    implements Histogram<T>
{
    protected final long[] counts;
    protected long totalCount = 0;


    public AbstractHistogram(int binCount)
    {
        counts = new long[binCount];
    }

    
    public void increment(int idx)
    {
        counts[idx]++;
        totalCount++;
    }
    

    @Override
    public int getBinCount()
    {
        return counts.length;
    }


    @Override
    public long getCount(int idx)
    {
        return counts[idx];
    }


    @Override
    public long getTotalCount()
    {
        return totalCount;
    }
}
