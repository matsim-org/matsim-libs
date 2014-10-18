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

public class TravelTimeUtils
{
    public static double[] calculateATOnArival(double[] timesOnDeparture, int interval)
    {
        double[] timesOnArrival = new double[timesOnDeparture.length];

        double prevArrivalTime = -interval;// !!! => firstIdx == 0 (@ first iteration)
        double prevArcTime = timesOnDeparture[0];// !!! => gradient == 0 (@ first iteration)

        // regular loop
        for (int i = 0; i < timesOnDeparture.length; i++) {
            double currArcTime = timesOnDeparture[i];
            double currArrivalTime = i * interval + currArcTime;

            double deltaArcTime = currArcTime - prevArcTime;
            double deltaArrivalTime = currArrivalTime - prevArrivalTime;

            if (deltaArrivalTime < 0) {
                throw new RuntimeException("FIFO property is broken");
            }
            else if (deltaArrivalTime == 0) {
                continue;
            }

            double gradient = (double)deltaArcTime / (double)deltaArrivalTime;

            // indices between prevAT (excluding) and currAT (including), ie. (prevAT;currAT]
            int firstIdx = (int)prevArrivalTime / interval + 1;
            int lastIdx = (int)currArrivalTime / interval;

            if (lastIdx >= timesOnArrival.length) {
                lastIdx = timesOnArrival.length - 1;
            }

            for (int j = firstIdx; j <= lastIdx; j++) {
                timesOnArrival[j] = prevArcTime + gradient * (j * interval - prevArrivalTime);
            }

            if (lastIdx == timesOnArrival.length - 1) {
                break;
            }

            prevArcTime = currArcTime;
            prevArrivalTime = currArrivalTime;
        }

        return timesOnArrival;
    }


    public static void checkFIFOProperty(int[] times, boolean onDeparture, int interval)
    {
        int prevAT = times[0];

        for (int i = 1; i < times.length; i++) {
            int currAT = times[i];

            if (onDeparture) { // onDeparture
                if (prevAT > currAT + interval) {
                    times[i] = currAT = prevAT - interval + 1;
                    throw new RuntimeException("FIFO property is broken! - prevAT=" + prevAT
                            + " currAT+interval=" + (currAT + interval));
                }
            }
            else { // onArrival
                if (currAT > prevAT + interval) {
                    throw new RuntimeException("FIFO property is broken!");
                }
            }

            prevAT = currAT;
        }
    }
}
