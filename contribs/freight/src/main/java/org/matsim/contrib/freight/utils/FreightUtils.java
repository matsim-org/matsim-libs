/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
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

package org.matsim.contrib.freight.utils;

import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierImpl;
import org.matsim.contrib.freight.carrier.CarrierService;
import org.matsim.contrib.freight.carrier.CarrierShipment;
import org.matsim.contrib.freight.carrier.Carriers;
import org.matsim.contrib.freight.carrier.ScheduledTour;
import org.matsim.contrib.freight.carrier.Tour.ServiceActivity;
import org.matsim.contrib.freight.carrier.Tour.TourElement;

public class FreightUtils {

	public Carriers createShipmentVRPCarrierFromServiceVRPSolution(Carriers carriers) {
		Carriers carriersWithShipments = new Carriers();
		for (Carrier carrier : carriers.getCarriers().values()){
			Carrier carrierWS = CarrierImpl.newInstance(carrier.getId());
			copyShipments(carrierWS, carrier);
			//			copyPickups(carrierWS, carrier);	//Not implemented yet, kmt Sep18
			//			copyDeliveries(carrierWS, carrier); //Not implemented yet, kmt Sep18
			createShipmentsFromService(carrierWS, carrier);
			carriersWithShipments.addCarrier(carrierWS);
		}


		return carriersWithShipments;

	}

	private void copyDeliveries(Carrier carrierWS, Carrier carrier) {
		// TODO Auto-generated method stub
	}

	private void copyPickups(Carrier carrierWS, Carrier carrier) {
		// TODO Auto-generated method stub
	}

	private void copyShipments(Carrier carrierWS, Carrier carrier) {
		for (CarrierShipment carrierShipment: carrier.getShipments()){
			carrierWS.getShipments().add(carrierShipment);
		}
	}

	private void createShipmentsFromService(Carrier carrierWS, Carrier carrier) {
		TreeMap<Id<CarrierService>, Id<Link>> depotServiceIsdeliveredFrom = new TreeMap<Id<CarrierService>, Id<Link>>();
		for (ScheduledTour tour : carrier.getSelectedPlan().getScheduledTours()) {
			Id<Link> depotForTour = tour.getVehicle().getLocation();
			for (TourElement te : tour.getTour().getTourElements()) {
				if (te instanceof ServiceActivity){
					ServiceActivity act = (ServiceActivity) te;
					depotServiceIsdeliveredFrom.put(act.getService().getId(), depotForTour);
				}
			}

			for (CarrierService carrierService : carrier.getServices()) {
				CarrierShipment carrierShipment = CarrierShipment.Builder.newInstance(Id.create(carrierService.getId().toString(), CarrierShipment.class), 
						depotServiceIsdeliveredFrom.get(carrierService.getId()), 
						carrierService.getLocationLinkId(), 
						carrierService.getCapacityDemand())
						.build();
				carrierWS.getShipments().add(carrierShipment);
			}
		}

	}

}
