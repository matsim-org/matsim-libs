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

import java.util.TreeMap;

import org.matsim.core.network.NetworkChangeEvent.ChangeValue;


public interface TimeVariantAttribute
{
    interface ChangeValueGetter
    {
        ChangeValue getChangeValue(NetworkChangeEvent event);
    }


    static ChangeValueGetter FREESPEED_GETTER = new ChangeValueGetter() {
        public ChangeValue getChangeValue(NetworkChangeEvent event)
        {
            return event.getFreespeedChange();
        }
    };

    static ChangeValueGetter FLOW_CAPACITY_GETTER = new ChangeValueGetter() {
        public ChangeValue getChangeValue(NetworkChangeEvent event)
        {
            return event.getFlowCapacityChange();
        }
    };

    static ChangeValueGetter LANES_GETTER = new ChangeValueGetter() {
        public ChangeValue getChangeValue(NetworkChangeEvent event)
        {
            return event.getLanesChange();
        }
    };


    double getValue(final double time);


    boolean isRecalcRequired();


    void recalc(TreeMap<Double, NetworkChangeEvent> changeEvents, ChangeValueGetter valueGetter,
            double baseValue);


    void incChangeEvents();


    void clearEvents();
}
