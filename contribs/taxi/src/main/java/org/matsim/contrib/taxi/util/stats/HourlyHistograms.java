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

import java.io.*;


public class HourlyHistograms
{
    public final int hour;

    public final Histogram passengerWaitTime = new Histogram(2.5 * 60, 25);

    public final Histogram emptyDriveTime = new Histogram(60, 21);
    public final Histogram occupiedDriveTime = new Histogram(120, 21);

    //histograms from different hours cannot be aggregated for these two:
    public final Histogram emptyDriveRatio = new Histogram(0.05, 20);
    public final Histogram stayRatio = new Histogram(0.05, 20);


    public HourlyHistograms(int hour)
    {
        this.hour = hour;
    }


    public static final String MAIN_HEADER = //
    "\tPassenger_Wait_Time [min]" + tabs(25) + //
    //
    "\tEmpty_Drive_Time [min]" + tabs(21) + //
            "\tOccupied_Drive_Time [min]" + tabs(21) + //
    //
    "\tEmpty_Drive_Ratio [%]" + tabs(20) + //
            "\tVehicle_Wait_Ratio [%]" + tabs(20); //


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
        pw.print(passengerWaitTime.binsToString(1. / 60));
        //
        pw.print("hour\t");
        pw.print(emptyDriveTime.binsToString(1. / 60));
        pw.print("hour\t");
        pw.print(occupiedDriveTime.binsToString(1. / 60));
        //
        pw.print("hour\t");
        pw.print(emptyDriveRatio.binsToString(100));
        pw.print("hour\t");
        pw.print(stayRatio.binsToString(100));
        pw.println();
    }


    public void printHistograms(PrintWriter pw)
    {
        pw.print(hour + "\t");
        pw.print(passengerWaitTime.countsToString());
        //
        pw.print(hour + "\t");
        pw.print(emptyDriveTime.countsToString());
        pw.print(hour + "\t");
        pw.print(occupiedDriveTime.countsToString());
        //
        pw.print(hour + "\t");
        pw.print(emptyDriveRatio.countsToString());
        pw.print(hour + "\t");
        pw.print(stayRatio.countsToString());
        pw.println();
    }


    public static void printAllHistograms(HourlyHistograms[] hourlyHistograms, String file)
    {
        try (PrintWriter hourlyHistogramsWriter = new PrintWriter(file)) {
            hourlyHistogramsWriter.println(HourlyHistograms.MAIN_HEADER);
            hourlyHistograms[0].printSubHeaders(hourlyHistogramsWriter);

            for (HourlyHistograms hh : hourlyHistograms) {
                hh.printHistograms(hourlyHistogramsWriter);
            }
        }
        catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
