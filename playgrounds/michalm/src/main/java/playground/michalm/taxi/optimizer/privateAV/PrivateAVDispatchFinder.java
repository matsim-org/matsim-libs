/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

package playground.michalm.taxi.optimizer.privateAV;

import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.taxi.data.TaxiRequest;
import org.matsim.contrib.taxi.optimizer.*;

/**
 * @author michalm
 */
public class PrivateAVDispatchFinder extends BestDispatchFinder {

	public PrivateAVDispatchFinder(TaxiOptimizerContext optimContext) {
		super(optimContext);
	}

	@Override
	public Dispatch<TaxiRequest> findBestRequestForVehicle(Vehicle veh, Iterable<TaxiRequest> unplannedRequests) {
		// check if veh can serve any of the unplanned requests; if yes, then choose one
		//
		// return new Dispatch<>(veh, selectedRequest, vrpPathFromVehicleLocationToSelectedRequest);
		return null;
	}

	@Override
	public Dispatch<TaxiRequest> findBestVehicleForRequest(TaxiRequest req, Iterable<? extends Vehicle> vehicles) {
		// check if req can be served by any of the idle vehicles; if yes, then choose one
		//
		// return new Dispatch<>(selectedVehicle, req, vrpPathFromVehicleLocationToSelectedRequest);
		return null;
	}
}
