/* *********************************************************************** *
 * project, org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       , (C) 2015 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           , info at matsim dot org                                *
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

import java.util.*;


public class HourlyHistograms
{
    public enum Type
    {
        passengerWaitTime, emptyDriveTime, occupiedDriveTime, emptyDriveRatio, stayRatio;
    }


    public final int hour;

    public final Histogram passengerWaitTime = new Histogram(2.5 * 60, 25);

    public final Histogram emptyDriveTime = new Histogram(60, 21);
    public final Histogram occupiedDriveTime = new Histogram(120, 21);

    //histograms from different hours cannot be aggregated for these two:
    public final Histogram emptyDriveRatio = new Histogram(0.05, 20);
    public final Histogram stayRatio = new Histogram(0.05, 20);

    public final Map<Type, Histogram> histogramMap;


    public HourlyHistograms(int hour)
    {
        this.hour = hour;

        EnumMap<Type, Histogram> histogramMap = new EnumMap<>(Type.class);
        histogramMap.put(Type.passengerWaitTime, passengerWaitTime);
        histogramMap.put(Type.emptyDriveTime, emptyDriveTime);
        histogramMap.put(Type.occupiedDriveTime, occupiedDriveTime);
        histogramMap.put(Type.emptyDriveRatio, emptyDriveRatio);
        histogramMap.put(Type.stayRatio, stayRatio);
        this.histogramMap = Collections.unmodifiableMap(histogramMap);
    }
}
