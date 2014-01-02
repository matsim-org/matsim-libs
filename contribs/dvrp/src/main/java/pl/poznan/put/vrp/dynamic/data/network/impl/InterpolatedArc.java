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

package pl.poznan.put.vrp.dynamic.data.network.impl;

import org.matsim.api.core.v01.network.Link;

import pl.poznan.put.vrp.dynamic.util.TimeDiscretizer;


public class InterpolatedArc
    extends AbstractArc
{
    private TimeDiscretizer timeDiscretizer;

    private int[] timesOnDeparture;
    private int[] timesOnArrival;
    private double[] costsOnDeparture;


    public InterpolatedArc(Link fromLink, Link toLink, TimeDiscretizer timeDiscretizer,
            int[] timesOnDeparture, double[] costsOnDeparture)
    {
        super(fromLink, toLink);
        if (timeDiscretizer.getIntervalCount() != timesOnDeparture.length
                || timesOnDeparture.length != costsOnDeparture.length) {
            throw new IllegalArgumentException();
        }

        this.timeDiscretizer = timeDiscretizer;
        this.timesOnDeparture = timesOnDeparture;
        this.costsOnDeparture = costsOnDeparture;

        checkFIFOProperty(timesOnDeparture, true);

        calculateATOnArival();
        checkFIFOProperty(timesOnArrival, false);
    }


    @Override
    public int getTimeOnDeparture(int departureTime)
    {
        return timeDiscretizer.interpolate(timesOnDeparture, departureTime);
    }


    @Override
    public int getTimeOnArrival(int arrivalTime)
    {
        return timeDiscretizer.interpolate(timesOnArrival, arrivalTime);
    }


    @Override
    public double getCostOnDeparture(int departureTime)
    {
        return timeDiscretizer.interpolate(costsOnDeparture, departureTime);
    }


    public int calculateAvgTime()
    {
        int sumT = 0;

        for (int t : timesOnDeparture) {
            sumT += t;
        }

        return sumT / timesOnDeparture.length;
    }


    public double calculateAvgCost()
    {
        double sumC = 0;

        for (double c : costsOnDeparture) {
            sumC += c;
        }

        return sumC / costsOnDeparture.length;
    }


    private void calculateATOnArival()
    {
        timesOnArrival = new int[timesOnDeparture.length];

        int interval = timeDiscretizer.getTimeInterval();

        int prevArrivalTime = -interval;// !!! => firstIdx == 0 (@ first iteration)
        int prevArcTime = timesOnDeparture[0];// !!! => gradient == 0 (@ first iteration)

        // regular loop
        for (int i = 0; i < timesOnDeparture.length; i++) {
            int currArcTime = timesOnDeparture[i];
            int currArrivalTime = i * interval + currArcTime;

            int deltaArcTime = currArcTime - prevArcTime;
            int deltaArrivalTime = currArrivalTime - prevArrivalTime;

            if (deltaArrivalTime < 0) {
                throw new RuntimeException("FIFO property is broken");
            }
            else if (deltaArrivalTime == 0) {
                continue;
            }

            double gradient = (double)deltaArcTime / (double)deltaArrivalTime;

            // indices between prevAT (excluding) and currAT (including), ie. (prevAT;currAT]
            int firstIdx = prevArrivalTime / interval + 1;
            int lastIdx = currArrivalTime / interval;

            if (lastIdx >= timesOnArrival.length) {
                lastIdx = timesOnArrival.length - 1;
            }

            for (int j = firstIdx; j <= lastIdx; j++) {
                timesOnArrival[j] = (int) (prevArcTime + gradient
                        * (j * interval - prevArrivalTime));
            }

            if (lastIdx == timesOnArrival.length - 1) {
                break;
            }

            prevArcTime = currArcTime;
            prevArrivalTime = currArrivalTime;
        }
    }


    private void checkFIFOProperty(int[] times, boolean onDeparture)
    {
        int prevAT = times[0];
        int interval = timeDiscretizer.getTimeInterval();

        for (int i = 1; i < times.length; i++) {
            int currAT = times[i];

            if (onDeparture) { // onDeparture
                if (prevAT > currAT + interval) {
                    times[i] = currAT = prevAT - interval + 1;
                    System.err
                            .println("Warning!!! FIFO property was broken! - times has been modified");
                    // throw new RuntimeException("FIFO property is broken! - prevAT=" + prevAT
                    // + " currAT+interval=" + (currAT + interval));
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
