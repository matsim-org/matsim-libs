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

package org.matsim.contrib.taxi.util.stats;

import java.text.DecimalFormat;


public class Histogram
{
    private final int[] counts;
    private final double binSize;


    public Histogram(double binSize, int binCount)
    {
        this.binSize = binSize;
        counts = new int[binCount];
    }


    public void addValue(double value)
    {
        int bin = Math.min((int) (value / binSize), counts.length - 1);
        counts[bin]++;
    }


    public double getBinSize()
    {
        return binSize;
    }


    public int[] getCounts()
    {
        return counts;
    }


    public String binsToString()
    {
        return binsToString(1.0);
    }


    public String binsToString(double scaleFactor)
    {
        DecimalFormat df = new DecimalFormat("#.##");
        String str = "";
        for (int i = 0; i < counts.length; i++) {
            str += df.format(i * binSize * scaleFactor) + "+\t";
        }
        return str;
    }


    public String countsToString()
    {
        String str = "";
        for (int i = 0; i < counts.length; i++) {
            str += String.format("%d\t", counts[i]);
        }
        return str;
    }
}
