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

package org.matsim.core.network;

import java.util.*;

import org.matsim.core.network.NetworkChangeEvent.ChangeValue;


public class VariableIntervalTimeVariantAttribute
    implements TimeVariantAttribute
{
    private int aEvents = 1;
    private double[] aValues;
    private double[] aTimes;


    @Override
    public boolean isRecalcRequired()
    {
        return (this.aTimes == null) || (this.aTimes.length != this.aEvents);
    }


    @Override
    public void recalc(TreeMap<Double, NetworkChangeEvent> changeEvents,
            ChangeValueGetter valueGetter, double baseValue)
    {
        this.aTimes = new double[this.aEvents];
        this.aValues = new double[this.aEvents];
        this.aTimes[0] = Double.NEGATIVE_INFINITY;
        this.aValues[0] = baseValue;

        int numEvent = 0;
        if (changeEvents != null) {
            for (NetworkChangeEvent event : changeEvents.values()) {
                ChangeValue value = valueGetter.getChangeValue(event);
                if (value != null) {
                    if (value.getType() == NetworkChangeEvent.ChangeType.FACTOR) {
                        double currentValue = this.aValues[numEvent];
                        this.aValues[++numEvent] = currentValue * value.getValue();
                        this.aTimes[numEvent] = event.getStartTime();
                    }
                    else {
                        this.aValues[++numEvent] = value.getValue();
                        this.aTimes[numEvent] = event.getStartTime();
                    }
                }
            }
        }

        if (numEvent != this.aEvents - 1) {
            throw new RuntimeException("Expected number of change events (" + (this.aEvents - 1)
                    + ") differs from the number of events found (" + numEvent + ")!");
        }
    }


    @Override
    public double getValue(final double time)
    {
        int key = Arrays.binarySearch(this.aTimes, time);
        key = key >= 0 ? key : -key - 2;
        return this.aValues[key];
    }


    @Override
    public void incChangeEvents()
    {
        aEvents++;
    }


    @Override
    public void clearEvents()
    {
        aTimes = null;
        aValues = null;
        aEvents = 1;
    }
}
