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

package playground.jbischoff.taxibus.optimizer.defaultOptimizer.filter;

import org.matsim.contrib.dvrp.data.Vehicle;

import playground.jbischoff.taxibus.passenger.TaxibusRequest;
import playground.michalm.taxi.data.TaxiRequest;


public interface TaxibusVehicleFilter
{
    TaxibusVehicleFilter NO_FILTER = new TaxibusVehicleFilter() {
        public Iterable<Vehicle> filterVehiclesForRequest(Iterable<Vehicle> vehicles,
                TaxibusRequest request)
        {
            return vehicles;
        }
    };


    Iterable<Vehicle> filterVehiclesForRequest(Iterable<Vehicle> vehicles, TaxibusRequest request);
}
