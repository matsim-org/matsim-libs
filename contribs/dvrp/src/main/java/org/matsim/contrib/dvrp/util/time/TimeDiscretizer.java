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

package org.matsim.contrib.dvrp.util.time;

public class TimeDiscretizer
{
    public static final TimeDiscretizer DEFAULT = new TimeDiscretizer(30 * 4, 15 * 60);
    public static final TimeDiscretizer ONE_HOUR = new TimeDiscretizer(30, 60 * 60);
    public static final TimeDiscretizer ALL_DAY = new TimeDiscretizer(1, 30 * 60 * 60);
    

    private final int intervalCount;
    private final int timeInterval;


    public TimeDiscretizer(int intervalCount, int timeInterval)
    {
        this.timeInterval = timeInterval;
        this.intervalCount = intervalCount;
    }


    public int getIdx(double time)
    {
        if (time < 0) {
            throw new IllegalArgumentException();
        }

        int idx = (int)time / timeInterval;

        if (idx >= intervalCount) {
            throw new IllegalArgumentException();
        }

        return idx;
    }


    public double getTime(int idx)
    {
        if (idx >= intervalCount) {
            throw new IllegalArgumentException();
        }

        return idx * timeInterval;
    }


    public double interpolate(double[] vals, double time)
    {
        int idx0 = (int)time / timeInterval;
        int idx1 = idx0 + 1;

        if (idx1 >= intervalCount) {
            throw new IllegalArgumentException();
        }

        double weight1 = time % timeInterval;
        double weight0 = timeInterval - weight1;

        double weightedSum = weight0 * vals[idx0] + weight1 * vals[idx1];
        return weightedSum / timeInterval;
    }


    public double interpolate(int[] vals, double time)
    {
        int idx0 = (int)time / timeInterval;
        int idx1 = idx0 + 1;

        if (idx1 >= intervalCount) {
            throw new IllegalStateException();
        }

        double weight1 = time % timeInterval;
        double weight0 = timeInterval - weight1;

        double weightedSum = weight0 * vals[idx0] + weight1 * vals[idx1];// int -> double
        return weightedSum / timeInterval;

    }


    public int getTimeInterval()
    {
        return timeInterval;
    }


    public int getIntervalCount()
    {
        return intervalCount;
    }


    //=============================================================================================

    public interface Interpolator
    {
        double interpolate(double time);
    }


    public Interpolator createInterpolator(final int[] values)
    {
        if (getIntervalCount() != values.length) {
            throw new IllegalArgumentException();
        }

        return new Interpolator() {
            public double interpolate(double time)
            {
                return TimeDiscretizer.this.interpolate(values, time);
            }
        };
    }


    public Interpolator createInterpolator(final double[] values)
    {
        if (getIntervalCount() != values.length) {
            throw new IllegalArgumentException();
        }

        return new Interpolator() {
            public double interpolate(double time)
            {
                return TimeDiscretizer.this.interpolate(values, time);
            }
        };
    }
}
