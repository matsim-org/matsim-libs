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

public class IntegerInterpolator
{
    private TimeDiscretizer timeDiscretizer;
    private int[] values;


    public IntegerInterpolator(TimeDiscretizer timeDiscretizer, int[] values)
    {
        this.timeDiscretizer = timeDiscretizer;
        this.values = values;

        if (timeDiscretizer.getIntervalCount() != values.length) {
            throw new IllegalArgumentException();
        }
    }


    public int interpolate(int time)
    {
        return timeDiscretizer.interpolate(values, time);
    }
}
