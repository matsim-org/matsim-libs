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

package pl.poznan.put.vrp.dynamic.util;

public class DoubleInterpolator
{
    private TimeDiscretizer timeDiscretizer;
    private double[] values;


    public DoubleInterpolator(TimeDiscretizer timeDiscretizer, double[] values)
    {
        this.timeDiscretizer = timeDiscretizer;
        this.values = values;

        if (timeDiscretizer.getIntervalCount() != values.length) {
            throw new IllegalArgumentException();
        }
    }


    public double interpolate(int time)
    {
        return timeDiscretizer.interpolate(values, time);
    }
}
