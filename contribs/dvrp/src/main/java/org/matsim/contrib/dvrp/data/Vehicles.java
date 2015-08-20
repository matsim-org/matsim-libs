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

package org.matsim.contrib.dvrp.data;

import java.util.Comparator;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;


public class Vehicles
{
    public static final Comparator<Vehicle> T0_COMPARATOR = new Comparator<Vehicle>() {
        public int compare(Vehicle v1, Vehicle v2)
        {
            return Double.compare(v1.getT0(), v2.getT0());
        }
    };

    public static final Comparator<Vehicle> T1_COMPARATOR = new Comparator<Vehicle>() {
        public int compare(Vehicle v1, Vehicle v2)
        {
            return Double.compare(v1.getT1(), v2.getT1());
        }
    };
    
    public static int countVehicles(Iterable<? extends Vehicle> vehicles,
            Predicate<Vehicle> predicate)
    {
        return Iterables.size(Iterables.filter(vehicles, predicate));
    }
}
