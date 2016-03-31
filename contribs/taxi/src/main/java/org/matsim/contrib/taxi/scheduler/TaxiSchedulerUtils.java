/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package org.matsim.contrib.taxi.scheduler;

import org.matsim.contrib.dvrp.data.Vehicle;

import com.google.common.base.Predicate;


public class TaxiSchedulerUtils
{
    public static Predicate<Vehicle> createIsIdle(final TaxiScheduleInquiry scheduleInquiry)
    {
        return new Predicate<Vehicle>() {
            public boolean apply(Vehicle vehicle)
            {
                return scheduleInquiry.isIdle(vehicle);
            }
        };
    }
}
