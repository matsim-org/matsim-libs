/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

import org.apache.commons.math3.random.RandomGenerator;


public class UniformRandom
{
    private final RandomGenerator rg;


    public UniformRandom(RandomGenerator rg)
    {
        this.rg = rg;
    }


    public double nextDouble(double from, double to)
    {
        return from == to ? from : from + (to - from) * rg.nextDouble();
    }


    public int nextInt(int from, int to)
    {
        if (from == to) {
            return from;
        }

        long delta = (long) ( (1L + (long)to - (long)from) * rg.nextDouble());
        return (int) (from + delta);
    }


    public double floorOrCeil(double value)
    {
        double floor = Math.floor(value);
        boolean selectCeil = trueOrFalse(value - floor);
        return selectCeil ? floor + 1 : floor;
    }


    public boolean trueOrFalse(double trueProbability)
    {
        return rg.nextDouble() < trueProbability;
    }
}
