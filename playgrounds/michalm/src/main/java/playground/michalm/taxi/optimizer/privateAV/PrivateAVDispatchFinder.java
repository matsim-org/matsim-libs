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

package playground.michalm.taxi.privateAV;

import java.util.ArrayList;
import java.util.List;

import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.taxi.data.TaxiRequest;
import org.matsim.contrib.taxi.optimizer.*;
import org.matsim.contrib.taxi.scheduler.TaxiScheduleInquiry;

/**
 * @author michalm
 */
public class PrivateAVDispatchFinder extends BestDispatchFinder {


  private final BestDispatchFinder myDispatchFinder;

  public PrivateAVDispatchFinder(TaxiOptimizerContext optimContext) {
    super(optimContext);
    this.myDispatchFinder = new BestDispatchFinder(optimContext);
  }



	@Override
	public Dispatch<TaxiRequest> findBestRequestForVehicle(Vehicle veh, Iterable<TaxiRequest> unplannedRequests) {

	  // See if vehicle can serve any of the unplanned requests
	  String vehID = veh.getId().toString().substring(0,9);

	  // filter requests down to requests from own household
	  List<TaxiRequest> ownRequests = new ArrayList<TaxiRequest>();

	  for(TaxiRequest req : unplannedRequests){
	    String requester = req.getPassenger().getId().toString();
	    String hh = requester.substring(0, 9);

	    if(hh.equals(vehID)){
	      ownRequests.add(req);
	    }
	  }

	  // check if veh can serve any of the unplanned requests; if yes, then choose one
	  if(ownRequests.isEmpty()){
	    return null;
	  } else {
      Dispatch<TaxiRequest> myDispatch = myDispatchFinder.findBestRequestForVehicle(veh, ownRequests);
      return myDispatch;
	  }
	}

	@Override
	public Dispatch<TaxiRequest> findBestVehicleForRequest(TaxiRequest req, Iterable<? extends Vehicle> vehicles) {
    // check if req can be served by any of the idle vehicles; if yes, then choose one
    // filter own vehicles from all idle vehicles
    String requester = req.getPassenger().getId().toString();
    String hh = requester.substring(0, 9);

    List<Vehicle> ownVehicles = new ArrayList<Vehicle>();

    // make a list of the AV's available to the household
    for(Vehicle  veh : vehicles){
      String vehID = veh.getId().toString().substring(0,9);

      if(vehID.equals(hh)){
        ownVehicles.add(veh);
      }
    }

    //call BestDispatchFinder for the selected subset of vehicles
    if(ownVehicles.isEmpty()){
      return null;
    } else {
      Dispatch<TaxiRequest> myDispatch = myDispatchFinder.findBestVehicleForRequest(req, ownVehicles);
      return myDispatch;
    }
	}
}
