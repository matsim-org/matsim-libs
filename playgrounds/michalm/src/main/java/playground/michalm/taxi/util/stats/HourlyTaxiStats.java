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

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;


public class HourlyTaxiStats
{
    public final int hour;

    public final DescriptiveStatistics passengerWaitTimes = new DescriptiveStatistics();
    public final DescriptiveStatistics emptyDriveRatio = new DescriptiveStatistics();
    public final DescriptiveStatistics stayRatio = new DescriptiveStatistics();

    public int stayLt1PctCount = 0;//== approx. "always busy"; 0pct is impossible due to 1-sec stay tasks
    public int stayLt25PctCount = 0;
    public int stayLt50PctCount = 0;
    public int stayLt75PctCount = 0;
    public int stayLt100PctCount = 0;//== approx. "at least one ride"
    public int allCount = 0;//== all operating vehicles


    public HourlyTaxiStats(int hour)
    {
        this.hour = hour;
    }


    public static final String MAIN_HEADER = "hour\t" + // 
            "Passenger_Wait_Time\t\t\t\t\t\t\t\t\t\t" + //
            "Empty_Drive_Ratio\t\t\t\t\t\t\t\t\t\t" + //
            "Vehicle_Wait_Ratio\t\t\t\t\t\t\t\t\t\t" + //
            "Num_Vehicle_Wait_Ratio\t\t\t\t\t";

    public static final String SUB_HEADER = "\t" + //
            "mean\tmin\tpc_2\tpc_5\tpc_25\tpc_50\tpc_75\tpc_95\tpc_98\tmax\t" + //
            "mean\tmin\tpc_2\tpc_5\tpc_25\tpc_50\tpc_75\tpc_95\tpc_98\tmax\t" + //
            "mean\tmin\tpc_2\tpc_5\tpc_25\tpc_50\tpc_75\tpc_95\tpc_98\tmax\t" + //
            "<1\t<25\t<50\t<75\t<100\tall\t";


    public void printStats(PrintWriter pw)
    {
        pw.printf(
                "%d\t"//hour
                        + "%.0f\t%.0f\t%.0f\t%.0f\t%.0f\t%.0f\t%.0f\t%.0f\t%.0f\t%.0f\t"//
                        + "%.2f\t%.2f\t%.2f\t%.2f\t%.2f\t%.2f\t%.2f\t%.2f\t%.2f\t%.2f\t"//
                        + "%.2f\t%.2f\t%.2f\t%.2f\t%.2f\t%.2f\t%.2f\t%.2f\t%.2f\t%.2f\t"//
                        + "%d\t%d\t%d\t%d\t%d\t%d\t\n", //
                hour, //
                //
                passengerWaitTimes.getMean(), //
                passengerWaitTimes.getMin(), //
                passengerWaitTimes.getPercentile(2), //
                passengerWaitTimes.getPercentile(5), //
                passengerWaitTimes.getPercentile(25), //
                passengerWaitTimes.getPercentile(50), //
                passengerWaitTimes.getPercentile(75), //
                passengerWaitTimes.getPercentile(95), //
                passengerWaitTimes.getPercentile(98), //
                passengerWaitTimes.getMax(), //
                //
                emptyDriveRatio.getMean(), //
                emptyDriveRatio.getMin(), //
                emptyDriveRatio.getPercentile(2), //
                emptyDriveRatio.getPercentile(5), //
                emptyDriveRatio.getPercentile(25), //
                emptyDriveRatio.getPercentile(50), //
                emptyDriveRatio.getPercentile(75), //
                emptyDriveRatio.getPercentile(95), //
                emptyDriveRatio.getPercentile(98), //
                emptyDriveRatio.getMax(), //
                //
                stayRatio.getMean(), //
                stayRatio.getMin(), //
                stayRatio.getPercentile(2), //
                stayRatio.getPercentile(5), //
                stayRatio.getPercentile(25), //
                stayRatio.getPercentile(50), //
                stayRatio.getPercentile(75), //
                stayRatio.getPercentile(95), //
                stayRatio.getPercentile(98), //
                stayRatio.getMax(), //
                //
                stayLt1PctCount, //
                stayLt25PctCount, //
                stayLt50PctCount, //
                stayLt75PctCount, //
                stayLt100PctCount, //
                allCount);
    }
}