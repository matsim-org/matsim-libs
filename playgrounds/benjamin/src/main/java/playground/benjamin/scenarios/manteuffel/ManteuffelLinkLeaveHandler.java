/* *********************************************************************** *
 * project: org.matsim.*
 * ManteuffelLinkLeaveHandler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package playground.benjamin.scenarios.manteuffel;

import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.TransitDriverStartsEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.emissions.types.HbefaVehicleAttributes;
import org.matsim.contrib.emissions.types.HbefaVehicleCategory;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;

public class ManteuffelLinkLeaveHandler implements LinkLeaveEventHandler, TransitDriverStartsEventHandler {
	private static final Logger logger = Logger.getLogger(ManteuffelLinkLeaveHandler.class);
	
	Vehicles vehiclesFromPlans;
	Map<Id<TransitLine>, TransitLine> transitLines;

	ManteuffelLinkLeaveHandler(Vehicles vehiclesFromPlans, Map<Id<TransitLine>, TransitLine> transitLines) {
		this.vehiclesFromPlans = vehiclesFromPlans;
		this.transitLines = transitLines;
	}

	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
//		Id<Vehicle> vehicleId = event.getVehicleId();
//		Id<Person> personId = event.getDriverId();
//		Id<Vehicle> vehId = Id.create(personId.toString(), Vehicle.class); //TODO: this should be rather the vehicle, not the person; re-think EmissionModule!
//		if(!vehiclesFromPlans.getVehicles().containsKey(vehId)){
////			logger.warn(vehId);
//			
//			HbefaVehicleCategory vehicleCategory = HbefaVehicleCategory.HEAVY_GOODS_VEHICLE;
//			HbefaVehicleAttributes vehicleAttributes = new HbefaVehicleAttributes();
//			
//			Id<VehicleType> vehTypeId = Id.create(vehicleCategory + ";" + 
//									  vehicleAttributes.getHbefaTechnology() + ";" + 
//									  vehicleAttributes.getHbefaSizeClass() + ";" + 
//									  vehicleAttributes.getHbefaEmConcept(), VehicleType.class);
//			VehicleType vehicleType = VehicleUtils.getFactory().createVehicleType(vehTypeId);
//			
//			if(!(vehiclesFromPlans.getVehicleTypes().containsKey(vehTypeId))){
//				vehiclesFromPlans.addVehicleType(vehicleType);
//			} else {
//				// do nothing
//			}
//			
//			Vehicle vehicle = VehicleUtils.getFactory().createVehicle(vehId, vehicleType);
////			if(vehId.equals("pt_tr_4673_2")){
////				logger.warn("Adding emission vehicle for person " + personId);				
////			}
//			vehiclesFromPlans.addVehicle(vehicle);
//			
//		} else {
//			// do nothing
//		}
	}

	@Override
	public void handleEvent(TransitDriverStartsEvent event) {
		Id<Person> personId = event.getDriverId();
		Id<Vehicle> vehId = Id.create(personId.toString(), Vehicle.class); //TODO: this should be rather the vehicle, not the person; re-think EmissionModule!
		
		TransitLine tl = this.transitLines.get(event.getTransitLineId());
		if(tl.getId().toString().contains("-B-")){
			HbefaVehicleCategory vehicleCategory = HbefaVehicleCategory.HEAVY_GOODS_VEHICLE;
			HbefaVehicleAttributes vehicleAttributes = new HbefaVehicleAttributes();
			
			Id<VehicleType> vehTypeId = Id.create(vehicleCategory + ";" + 
					vehicleAttributes.getHbefaTechnology() + ";" + 
					vehicleAttributes.getHbefaSizeClass() + ";" + 
					vehicleAttributes.getHbefaEmConcept(), VehicleType.class);
			VehicleType vehicleType = VehicleUtils.getFactory().createVehicleType(vehTypeId);
			
			if(!(vehiclesFromPlans.getVehicleTypes().containsKey(vehTypeId))){
				vehiclesFromPlans.addVehicleType(vehicleType);
			} else {
				// do nothing
			}
			
			Vehicle vehicle = VehicleUtils.getFactory().createVehicle(vehId, vehicleType);
			vehiclesFromPlans.addVehicle(vehicle);
		} else {
			
		}
	}
}