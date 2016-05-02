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

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;


public class HourlyTaxiStats
{
    final int hour;

    public final DescriptiveStatistics passengerWaitTime = new DescriptiveStatistics();
    public final DescriptiveStatistics emptyDriveRatio = new DescriptiveStatistics();
    public final DescriptiveStatistics occupiedDriveTime = new DescriptiveStatistics();
    public final DescriptiveStatistics stayRatio = new DescriptiveStatistics();

    int stayLt1PctCount = 0;//== approx. "always busy"; 0pct is impossible due to 1-sec stay tasks
    int stayLt25PctCount = 0;
    int stayLt50PctCount = 0;
    int stayLt75PctCount = 0;
    int stayLt100PctCount = 0;//== approx. "at least one ride"
    int allCount = 0;//== all operating vehicles


    public HourlyTaxiStats(int hour)
    {
        this.hour = hour;
    }


    public int getStayLt1PctCount()
    {
        return stayLt1PctCount;
    }


    public int getStayLt25PctCount()
    {
        return stayLt25PctCount;
    }


    public int getStayLt50PctCount()
    {
        return stayLt50PctCount;
    }


    public int getStayLt75PctCount()
    {
        return stayLt75PctCount;
    }


    public int getStayLt100PctCount()
    {
        return stayLt100PctCount;
    }


    public int getAllCount()
    {
        return allCount;
    }
}