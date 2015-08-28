/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.jbischoff.taxibus.scheduler;

import java.util.Set;

import org.matsim.contrib.dvrp.router.VrpPathWithTravelData;
import org.matsim.contrib.dvrp.schedule.DriveTaskImpl;

import playground.jbischoff.taxibus.passenger.TaxibusRequest;


/**
 * @author  jbischoff
 *
 */
public class TaxibusDriveWithPassengerTask extends DriveTaskImpl implements TaxibusTaskWithRequests {
	
	private Set<TaxibusRequest> requests;
	
	public TaxibusDriveWithPassengerTask(Set<TaxibusRequest> requests, VrpPathWithTravelData path) {
		super(path);
		this.requests = requests;
		for (TaxibusRequest req: this.requests){
			req.addDriveWithPassengerTask(this);
		}
		}

	

	@Override
	public TaxibusTaskType getTaxibusTaskType() {
		
		return TaxibusTaskType.DRIVE_WITH_PASSENGER;
	}
	
	

	@Override
	public Set<TaxibusRequest> getRequests() {
		return requests;
	}
	

	

	@Override
	public void removeFromRequest(TaxibusRequest request) {
		
		request.addDriveWithPassengerTask(null);
		this.requests.remove(request);

	}

	@Override
	public void removeFromAllRequests() {
		for (TaxibusRequest request : this.requests){
			request.addDriveWithPassengerTask(null);
			
		}
		this.requests.clear();
	}


	

}
