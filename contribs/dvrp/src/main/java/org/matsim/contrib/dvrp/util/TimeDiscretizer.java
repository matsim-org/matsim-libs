/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package org.matsim.contrib.dvrp.util;

public class TimeDiscretizer
{
    public static final TimeDiscretizer ACYCLIC_1_SEC = new TimeDiscretizer(30, 1, false);//just no discretization
    public static final TimeDiscretizer ACYCLIC_15_MIN = new TimeDiscretizer(30, 900, false);
    public static final TimeDiscretizer ACYCLIC_1_HOUR = new TimeDiscretizer(30, 3600, false);
    public static final TimeDiscretizer ACYCLIC_30_HOURS = new TimeDiscretizer(30, 30 * 3600,
            false);

    //useful for routing when running over-night scenarios, such as a 5am-5am taxi simulation
    public static final TimeDiscretizer CYCLIC_1_SEC = new TimeDiscretizer(24, 1, true);//just no discretization
    public static final TimeDiscretizer CYCLIC_15_MIN = new TimeDiscretizer(24, 900, true);
    public static final TimeDiscretizer CYCLIC_1_HOUR = new TimeDiscretizer(24, 3600, true);
    public static final TimeDiscretizer CYCLIC_24_HOURS = new TimeDiscretizer(24, 24 * 3600, true);

    private final int intervalCount;
    private final int timeInterval;
    private final boolean cyclic;


    public TimeDiscretizer(int hours, int timeInterval, boolean cyclic)
    {
        this.timeInterval = timeInterval;
        this.cyclic = cyclic;

        if (hours * 3600 % timeInterval != 0) {
            throw new IllegalArgumentException();
        }

        intervalCount = hours * 3600 / timeInterval;
    }


    public int getIdx(double time)
    {
        if (time < 0) {
            throw new IllegalArgumentException();
        }

        int idx = (int)time / timeInterval;

        if (idx >= intervalCount) {
            if (cyclic) {
                idx %= intervalCount;
            }
            else {
                throw new IllegalArgumentException();
            }
        }

        return idx;
    }


    public int discretize(double time)
    {
        int idx = getIdx(time);
        return idx * timeInterval;
    }


    public int getTimeInterval()
    {
        return timeInterval;
    }


    public int getIntervalCount()
    {
        return intervalCount;
    }
}
