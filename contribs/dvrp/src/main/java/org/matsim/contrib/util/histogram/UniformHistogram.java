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

public class UniformHistogram
    extends AbstractHistogram<Double>
{
    public static UniformHistogram create(double binSize, int binCount, double[] values)
    {
        UniformHistogram histogram = new UniformHistogram(binSize, binCount);
        histogram.addValues(values);
        return histogram;
    }


    private final double binSize;


    public UniformHistogram(double binSize, int binCount)
    {
        super(binCount);
        this.binSize = binSize;
    }


    public void addValues(double[] values)
    {
        for (double v : values) {
            addValue(v);
        }
    }


    public void addValue(double value)
    {
        increment(Math.min((int) (value / binSize), counts.length - 1));
    }


    @Override
    public Double getBin(int idx)
    {
        return idx * binSize;
    }
}
