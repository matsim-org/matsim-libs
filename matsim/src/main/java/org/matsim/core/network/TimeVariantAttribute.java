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

public class TimeVariantAttribute
{
    private int aFreespeedEvents = 1;
    private double[] aFreespeedValues;
    private double[] aFreespeedTimes;


    public void incChangeEvents()
    {
        aFreespeedEvents++;
    }
    
    public void clearEvents()
    {
        aFreespeedTimes = null;
        aFreespeedValues = null;
        aFreespeedEvents = 1;
    }
    
    public boolean doRequireRecalc()
    {
        return (this.aFreespeedTimes == null) || (this.aFreespeedTimes.length != this.aFreespeedEvents);
    }
    
    
    public void recalc(TreeMap<Double,NetworkChangeEvent> changeEvents, double baseValue)
    {
        this.aFreespeedTimes = new double [this.aFreespeedEvents];
        this.aFreespeedValues = new double [this.aFreespeedEvents];
        this.aFreespeedTimes[0] = Double.NEGATIVE_INFINITY;
        this.aFreespeedValues[0] = baseValue;

        int numEvent = 0;
        if (changeEvents != null) {
            for (NetworkChangeEvent event : changeEvents.values()) {
                ChangeValue value = event.getFreespeedChange();
                if (value != null) {
                    if (value.getType() == NetworkChangeEvent.ChangeType.FACTOR) {
                        double currentValue = this.aFreespeedValues[numEvent];
                        this.aFreespeedValues[++numEvent] = currentValue * value.getValue();
                        this.aFreespeedTimes[numEvent] = event.getStartTime();
                    } else {
                        this.aFreespeedValues[++numEvent] = value.getValue();
                        this.aFreespeedTimes[numEvent] = event.getStartTime();
                    }
                }
            }
        }

        if (numEvent != this.aFreespeedEvents - 1) {
            throw new RuntimeException("Expected number of change events (" + (this.aFreespeedEvents -1) + ") differs from the number of events found (" + numEvent + ")!");
        }
    }
    
    
    public double getValue(final double time)
    {
        int key = Arrays.binarySearch(this.aFreespeedTimes, time);
        key = key >= 0 ? key : -key - 2;
        return this.aFreespeedValues[key];

    }
}
