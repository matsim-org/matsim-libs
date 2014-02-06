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

import org.matsim.contrib.dvrp.MatsimVrpContext;
import org.matsim.contrib.dvrp.data.Vehicle;

import playground.michalm.taxi.TaxiData;
import playground.michalm.taxi.model.*;
import playground.michalm.taxi.model.TaxiRequest.TaxiRequestStatus;


public class TaxiScheduleValidator
{
    public static void assertNotIdleVehiclesAndUnplannedRequests(MatsimVrpContext context)
    {
        int unplannedRequests = 0;
        for (TaxiRequest req : ((TaxiData)context.getVrpData()).getTaxiRequests()) {
            if (req.getStatus() == TaxiRequestStatus.UNPLANNED) {
                unplannedRequests++;
            }
        }

        int idleTaxis = 0;
        for (Vehicle veh : context.getVrpData().getVehicles()) {
            if (TaxiUtils.isIdle(veh)) {
                idleTaxis++;
            }
        }

        if (idleTaxis > 0 && unplannedRequests > 0) {
            throw new IllegalStateException();
        }
    }
}
