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

import java.util.*;

import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.util.LinkTimePair;

import playground.michalm.taxi.util.TaxicabUtils;


public class VehicleData
{
    public final List<Vehicle> vehicles = new ArrayList<Vehicle>();
    public final List<LinkTimePair> departures = new ArrayList<LinkTimePair>();
    public final int idleVehCount;
    public final int dimension;


    public VehicleData(TaxiOptimizerConfiguration optimConfig)
    {
        int idleVehs = 0;
        for (Vehicle v : optimConfig.context.getVrpData().getVehicles()) {
            LinkTimePair departure = optimConfig.scheduler.getEarliestIdleness(v);
            //LinkTimePair diversion = scheduler.getClosestDiversion(v);

            if (departure != null) {
                vehicles.add(v);
                departures.add(departure);

                if (TaxicabUtils.isIdle(v)) {
                    idleVehs++;
                }
            }
        }

        idleVehCount = idleVehs;
        dimension = vehicles.size();
    }
}