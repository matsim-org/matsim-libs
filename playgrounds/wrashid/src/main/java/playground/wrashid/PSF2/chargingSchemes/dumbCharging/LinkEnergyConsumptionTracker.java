/* *********************************************************************** *
 * project: org.matsim.*
 * LinkEnergyConsumptionTracker.java
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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.AgentWait2LinkEvent;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.AgentWait2LinkEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;

import playground.wrashid.PSF2.ParametersPSF2;
import playground.wrashid.PSF2.vehicle.vehicleFleet.Vehicle;
import playground.wrashid.lib.GeneralLib;
import playground.wrashid.lib.obj.TwoHashMapsConcatenated;

public class LinkEnergyConsumptionTracker implements LinkEnterEventHandler, LinkLeaveEventHandler, AgentWait2LinkEventHandler {

	// personId,linkId,timeOfLinkEnterance
	TwoHashMapsConcatenated<Id,Id,Double> linkEntranceTime=new TwoHashMapsConcatenated<Id, Id, Double>(); 
	
	@Override
	public void reset(int iteration) {
		linkEntranceTime=new TwoHashMapsConcatenated<Id, Id, Double>();
	}

	@Override
	public void handleEvent(AgentWait2LinkEvent event) {
		// TODO Auto-generated method stub
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		Link link=ParametersPSF2.controler.getNetwork().getLinks().get(event.getLinkId());
		Vehicle vehicle=ParametersPSF2.vehicles.getValue(event.getPersonId());
		
		Double linkEnteranceTime=linkEntranceTime.get(event.getPersonId(), event.getLinkId());
		if (linkEnteranceTime!=null){
			double timeSpendOnLink=GeneralLib.getIntervalDuration(linkEnteranceTime, event.getTime());
			ParametersPSF2.energyStateMaintainer.processVehicleEnergyState(vehicle, timeSpendOnLink, link);
		}
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		linkEntranceTime.put(event.getPersonId(), event.getLinkId(), event.getTime());
	}

}
