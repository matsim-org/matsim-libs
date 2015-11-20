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
package org.matsim.contrib.transEnergySim.pt;

import java.util.HashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.Wait2LinkEventHandler;
import org.matsim.contrib.parking.lib.EventHandlerAtStartupAdder;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.contrib.parking.lib.obj.DoubleValueHashMap;
import org.matsim.contrib.parking.lib.obj.TwoKeyHashMapWithDouble;
import org.matsim.contrib.transEnergySim.vehicles.energyConsumption.EnergyConsumptionModel;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.api.experimental.events.VehicleDepartsAtFacilityEvent;
import org.matsim.core.api.experimental.events.handler.VehicleArrivesAtFacilityEventHandler;
import org.matsim.core.api.experimental.events.handler.VehicleDepartsAtFacilityEventHandler;
import org.matsim.core.controler.Controler;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;

public class ElectricPtSimModule implements VehicleArrivesAtFacilityEventHandler, VehicleDepartsAtFacilityEventHandler,
		LinkEnterEventHandler, LinkLeaveEventHandler, Wait2LinkEventHandler {

	private HashMap<Id<Vehicle>, EnergyConsumptionModel> vehicleEnergyConsumption;
	private DoubleValueHashMap<Id<TransitStopFacility>> chargingPowerAtStops;
	private HashMap<Id<Vehicle>, PtVehicleEnergyState> ptEnergyMangementModels;
	private PtVehicleEnergyControl ptVehicleEnergyControl;
	private DoubleValueHashMap<Id<TransitStopFacility>> stopArrivalTime = new DoubleValueHashMap<>();
	// vehicleId, linkId
	private TwoKeyHashMapWithDouble<Id, Id> linkEnterTime = new TwoKeyHashMapWithDouble<Id, Id>();

	public ElectricPtSimModule(Controler controler, HashMap<Id<Vehicle>, PtVehicleEnergyState> ptEnergyMangementModels,
			PtVehicleEnergyControl ptVehicleEnergyControl) {
		this.ptEnergyMangementModels = ptEnergyMangementModels;
//		this.chargingPowerAtStops = chargingPowerAtStops;
		this.ptVehicleEnergyControl = ptVehicleEnergyControl;
		EventHandlerAtStartupAdder eventHandlerAtStartupAdder = new EventHandlerAtStartupAdder();
		controler.addControlerListener(eventHandlerAtStartupAdder);
		eventHandlerAtStartupAdder.addEventHandler(this);
	}

	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub

	}

	@Override
	public void handleEvent(VehicleDepartsAtFacilityEvent event) {
		double durationOfStayAtStationInSeconds = GeneralLib.getIntervalDuration(stopArrivalTime.get(event.getFacilityId()),
				event.getTime());
		ptVehicleEnergyControl.handleChargingOpportunityAtStation(ptEnergyMangementModels.get(event.getVehicleId()),
				durationOfStayAtStationInSeconds, event.getFacilityId());
	}

	@Override
	public void handleEvent(VehicleArrivesAtFacilityEvent event) {
		stopArrivalTime.put(event.getFacilityId(), event.getTime());
	}

	@Override
	public void handleEvent(VehicleEntersTrafficEvent event) {
		linkEnterTime.put(event.getVehicleId(), event.getLinkId(), event.getTime());
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		double enterTime = linkEnterTime.get(event.getVehicleId(), event.getLinkId());
		double travelTime = GeneralLib.getIntervalDuration(enterTime, event.getTime());
		
		if(ptEnergyMangementModels.containsKey(event.getVehicleId()))
		{
			ptVehicleEnergyControl.handleLinkTravelled(ptEnergyMangementModels.get(event.getVehicleId()), event.getLinkId(), travelTime);
		}
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		linkEnterTime.put(event.getVehicleId(), event.getLinkId(), event.getTime());
	}

}
