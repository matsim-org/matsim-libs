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

package playground.michalm.taxi.optimizer;

import org.matsim.contrib.dvrp.data.Vehicles;

import playground.michalm.taxi.data.*;
import playground.michalm.taxi.data.TaxiRequest.TaxiRequestStatus;
import playground.michalm.taxi.scheduler.TaxiSchedulerUtils;


public class TaxiOptimizationValidation
{
    public static void assertNoUnplannedRequestsWhenIdleVehicles(
            TaxiOptimizerConfiguration optimConfig)
    {
        ETaxiData taxiData = (ETaxiData)optimConfig.context.getVrpData();

        if (Vehicles.countVehicles(taxiData.getVehicles(),
                TaxiSchedulerUtils.createIsIdle(optimConfig.scheduler)) == 0) {
            return;//OK
        }

        if (TaxiRequests.countRequestsWithStatus(taxiData.getTaxiRequests(),
                TaxiRequestStatus.UNPLANNED) == 0) {
            return; //OK
        }

        //idle vehicles and unplanned requests
        throw new IllegalStateException();
    }
}
