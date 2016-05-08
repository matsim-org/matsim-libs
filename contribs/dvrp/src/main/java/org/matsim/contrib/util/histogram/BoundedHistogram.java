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

import java.util.Arrays;


public class BoundedHistogram
    extends AbstractHistogram<String>
{
    public static BoundedHistogram create(double[] bounds, double[] values)
    {
        BoundedHistogram histogram = new BoundedHistogram(bounds);
        histogram.addValues(values);
        return histogram;
    }


    private final double[] bounds;


    public BoundedHistogram(double[] bounds)
    {
        super(bounds.length - 1);

        for (int i = 1; i < bounds.length; i++) {
            if (bounds[i - 1] >= bounds[i]) {
                throw new IllegalArgumentException("Bounds are not sorted");
            }
        }

        this.bounds = bounds;
    }


    public void addValues(double[] values)
    {
        for (double v : values) {
            addValue(v);
        }
    }


    //fails if value is outside the bounds
    public void addValue(double value)
    {
        int idx = Arrays.binarySearch(bounds, value);
        if (idx <= 0) {
            idx = -idx - 2;
        }
        increment(idx);
    }


    @Override
    public String getBin(int idx)
    {
        return bounds[idx] + "+";
        //return "[" + bounds[idx] + "," + bounds[idx + 1] + ")";
    }
}
