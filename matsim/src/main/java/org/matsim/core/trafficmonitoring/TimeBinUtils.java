/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package org.matsim.core.trafficmonitoring;

public class TimeBinUtils
{
    public static int getTimeBinIndex(double time, int travelTimeBinSize, int travelTimeBinCount)
    {
        return Math.min((int)time / travelTimeBinSize, travelTimeBinCount - 1);
    }


    public static int getTimeBinCount(int maxTime, int travelTimeBinSize)
    {
        return maxTime / travelTimeBinSize + 1;
    }
}
