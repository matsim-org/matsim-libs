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

package playground.jbischoff.taxibus.optimizer.filter;

import java.util.Collection;

import org.matsim.contrib.dvrp.data.Vehicle;

import playground.jbischoff.taxibus.passenger.TaxibusRequest;
import playground.jbischoff.taxibus.vehreqpath.TaxibusVehicleRequestPath;
import playground.michalm.taxi.data.TaxiRequest;


public interface TaxibusRequestFilter
{
    TaxibusRequestFilter NO_FILTER = new TaxibusRequestFilter() {

		@Override
		public Iterable<TaxibusRequest> filterRequestsForVehicle(Iterable<TaxibusRequest> requests, Vehicle vehicle) {
			return requests;
		}

		@Override
		public Iterable<TaxibusRequest> filterRequestsForBestRequest(Iterable<TaxibusRequest> unplannedRequests,
				TaxibusVehicleRequestPath best) {
			return unplannedRequests;
		}
    };


    Iterable<TaxibusRequest> filterRequestsForVehicle(Iterable<TaxibusRequest> requests, Vehicle vehicle);


	Iterable<TaxibusRequest> filterRequestsForBestRequest(Iterable<TaxibusRequest> unplannedRequests, TaxibusVehicleRequestPath best);
}
