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

package playground.michalm.taxi.util;

import org.matsim.contrib.dvrp.data.Vehicle;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;


public class TaxicabUtils
{
    public static int countVehicles(Iterable<? extends Vehicle> vehicles,
            Predicate<Vehicle> predicate)
    {
        return Iterables.size(Iterables.filter(vehicles, predicate));
    }
}
