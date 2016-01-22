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
import org.matsim.core.utils.misc.Time;


public class FixedIntervalTimeVariantAttribute
    implements TimeVariantAttribute
{
    private final int interval;
    private final int intervalCount;

    private double baseValue;
    private double[] values;

    private int eventsCount = 0;
    private int eventsCountWhenLastRecalc = -1;


    public FixedIntervalTimeVariantAttribute(int interval, int intervalCount)
    {
        this.interval = interval;
        this.intervalCount = intervalCount;
    }


    @Override
    public boolean isRecalcRequired()
    {
        return eventsCountWhenLastRecalc != eventsCount;
    }


    @Override
    public void recalc(TreeMap<Double, NetworkChangeEvent> changeEvents,
            ChangeValueGetter valueGetter, double baseValue)
    {
        this.baseValue = baseValue;

        if (eventsCount == 0) {
            return;
        }

        if (values == null) {
            values = new double[intervalCount];
        }

        int numEvent = 0;
        int fromBin = 0;//inclusive
        double currentValue = baseValue;
        if (changeEvents != null) {
            for (NetworkChangeEvent event : changeEvents.values()) {
                ChangeValue value = valueGetter.getChangeValue(event);
                if (value != null) {
                    numEvent++;

                    int toBin = (int) (event.getStartTime() / interval);//exclusive
                    Arrays.fill(values, fromBin, toBin, currentValue);

                    if (value.getType() == NetworkChangeEvent.ChangeType.FACTOR) {
                        currentValue *= value.getValue();
                    }
                    else {
                        currentValue = value.getValue();
                    }
                    fromBin = toBin;
                }
            }
        }
        Arrays.fill(values, fromBin, values.length, currentValue);
        eventsCountWhenLastRecalc = eventsCount;

        if (numEvent != this.eventsCount) {
            throw new RuntimeException("Expected number of change events (" + (this.eventsCount)
                    + ") differs from the number of events found (" + numEvent + ")!");
        }
    }


    @Override
    public double getValue(final double time)
    {
        if (time == Time.UNDEFINED_TIME || eventsCount == 0) {
            return baseValue;
        }

        int bin = (int) (time / interval);
        return values[bin];
    }


    @Override
    public void incChangeEvents()
    {
        eventsCount++;
    }


    @Override
    public void clearEvents()
    {
        eventsCount = 0;
        eventsCountWhenLastRecalc = -1;
        values = null;
    }
}
