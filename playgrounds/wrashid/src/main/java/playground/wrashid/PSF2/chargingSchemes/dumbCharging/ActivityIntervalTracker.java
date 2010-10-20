/* *********************************************************************** *
 * project: org.matsim.*
 * ActivityIntervalTracker.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.wrashid.PSF2.chargingSchemes.dumbCharging;

import java.util.HashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.ActivityEndEvent;
import org.matsim.core.api.experimental.events.ActivityStartEvent;
import org.matsim.core.api.experimental.events.handler.ActivityEndEventHandler;
import org.matsim.core.api.experimental.events.handler.ActivityStartEventHandler;

import playground.wrashid.PSF.ParametersPSF;
import playground.wrashid.PSF.energy.charging.ChargingTimes;
import playground.wrashid.PSF2.ParametersPSF2;
import playground.wrashid.PSF2.vehicle.vehicleFleet.Vehicle;
import playground.wrashid.lib.GeneralLib;
import playground.wrashid.lib.obj.TwoHashMapsConcatenated;

public class ActivityIntervalTracker implements ActivityStartEventHandler, ActivityEndEventHandler{

	
	// personId,linkId,activityStartTime
	TwoHashMapsConcatenated<Id,Id,Double> activityStartTime=new TwoHashMapsConcatenated<Id, Id, Double>();
	
	@Override
	public void reset(int iteration) {
		activityStartTime=new TwoHashMapsConcatenated<Id, Id, Double>();
		
		ParametersPSF2.chargingTimes=new HashMap<Id, ChargingTimes>();
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		Link link=ParametersPSF2.controler.getNetwork().getLinks().get(event.getLinkId());
		Vehicle vehicle=ParametersPSF2.vehicles.get(event.getPersonId()).get(0);
		
		Double actStartTime=activityStartTime.get(event.getPersonId(), event.getLinkId());
		if (actStartTime!=null){
			ParametersPSF2.energyStateMaintainer.chargeVehicle(vehicle, actStartTime, ParametersPSF.getFacilityChargingPowerMapper().getChargingPower(event.getFacilityId()), event);
		}
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		activityStartTime.put(event.getPersonId(), event.getLinkId(), event.getTime());
	}

}
