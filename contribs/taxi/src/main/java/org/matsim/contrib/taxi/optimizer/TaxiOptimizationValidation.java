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

package org.matsim.contrib.taxi.optimizer;

import org.matsim.contrib.taxi.data.*;
import org.matsim.contrib.taxi.data.TaxiRequest.TaxiRequestStatus;
import org.matsim.contrib.taxi.scheduler.TaxiSchedulerUtils;

import com.google.common.collect.Iterables;


public class TaxiOptimizationValidation
{
    public static void assertNoUnplannedRequestsWhenIdleVehicles(
            TaxiOptimizerContext optimContext)
    {
        int vehCount = Iterables.size(Iterables.filter(optimContext.taxiData.getVehicles().values(),
                TaxiSchedulerUtils.createIsIdle(optimContext.scheduler)));

        if (vehCount == 0) {
            return;//OK
        }

        if (TaxiRequests.countRequestsWithStatus(optimContext.taxiData.getTaxiRequests().values(),
                TaxiRequestStatus.UNPLANNED) == 0) {
            return; //OK
        }

        //idle vehicles and unplanned requests
        throw new IllegalStateException();
    }
}
