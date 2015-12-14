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

import java.io.PrintWriter;


public class HourlyHistograms
{
    public final int hour;

    public final Histogram passengerWaitTime = new Histogram(5 * 60, 25);

    public final Histogram emptyDriveTime = new Histogram(60, 21);
    public final Histogram occupiedDriveTime = new Histogram(60, 21);

    //histograms from different hours cannot be aggregated for these two:
    public final Histogram emptyDriveRatio = new Histogram(0.05, 20);
    public final Histogram stayRatio = new Histogram(0.05, 20);


    public HourlyHistograms(int hour)
    {
        this.hour = hour;
    }


    public static final String MAIN_HEADER = "hour\t" + // 
            "Passenger_Wait_Time" + tabs(25) + //
    //
    "Empty_Drive_Time" + tabs(21) + //
            "Occupied_Drive_Time" + tabs(21) + //
    //
    "Empty_Drive_Ratio" + tabs(20) + //
            "Vehicle_Wait_Ratio" + tabs(20); //


    public static String tabs(int binCount)
    {
        String tabs = "";
        for (int i = 0; i < binCount; i++) {
            tabs += '\t';
        }

        return tabs;
    }


    public void printSubHeaders(PrintWriter pw)
    {
        pw.print("hour\t");
        //
        pw.print(passengerWaitTime.binsToString());
        //
        pw.print(emptyDriveTime.binsToString());
        pw.print(occupiedDriveTime.binsToString());
        //
        pw.print(emptyDriveRatio.binsToString());
        pw.print(stayRatio.binsToString());
        pw.println();
    }


    public void printStats(PrintWriter pw)
    {
        pw.print(hour + "\t");
        //
        pw.print(passengerWaitTime.countsToString());
        //
        pw.print(emptyDriveTime.countsToString());
        pw.print(occupiedDriveTime.countsToString());
        //
        pw.print(emptyDriveRatio.countsToString());
        pw.print(stayRatio.countsToString());
        pw.println();
    }
}
