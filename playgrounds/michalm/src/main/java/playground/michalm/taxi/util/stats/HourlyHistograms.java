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

package playground.michalm.taxi.util.stats;

public class HourlyHistograms
{
    public final int hour;

    public final Histogram emptyDriveTime = new Histogram(60, 21);
    public final Histogram occupiedDriveTime = new Histogram(60, 21);

    //histograms from different hours cannot be aggregated for these two:
    public final Histogram emptyDriveRatio = new Histogram(0.05, 20);
    public final Histogram stayRatio = new Histogram(0.05, 20);

    public final Histogram passengerWaitTime = new Histogram(5 * 60, 25);


    public HourlyHistograms(int hour)
    {
        this.hour = hour;
    }
}
