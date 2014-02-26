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

package playground.michalm.taxi.optimizer.fifo;

import org.matsim.contrib.dvrp.data.Vehicle;

import playground.michalm.taxi.schedule.TaxiSchedules;


public class RESTaxiOptimizer
    extends OTSTaxiOptimizer
{
    public RESTaxiOptimizer(TaxiOptimizerConfiguration optimConfig)
    {
        super(optimConfig);
    }


    @Override
    protected void scheduleUnplannedRequests()
    {
        for (Vehicle veh : optimConfig.context.getVrpData().getVehicles()) {
            optimConfig.scheduler.removePlannedRequests(TaxiSchedules.getSchedule(veh),
                    unplannedRequests);
        }

        super.scheduleUnplannedRequests();
    }
}
