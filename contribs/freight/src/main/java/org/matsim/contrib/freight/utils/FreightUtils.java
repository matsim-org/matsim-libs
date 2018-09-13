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

import org.apache.log4j.Logger;
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
	
	private static final Logger log = Logger.getLogger(FreightUtils.class);

	/**
	 * Creates a new Carries only with @link{CarrierShipment}s for creating a new VRP. 
	 * As consequence of the transformation of @link{CarrierService}s to @link{CarrierShipment}s the solution of the VRP can have tours with
	 * vehicles returning to the depot and load for another tour instead of creating another vehicle with additional (fix) costs. 
	 * 
	 * @param carriers	carriers with a Solution (result of solving the VRP).
	 * @return Carriers carriersWithShipments
	 */
	public static Carriers createShipmentVRPCarrierFromServiceVRPSolution(Carriers carriers) {
		Carriers carriersWithShipments = new Carriers();
		for (Carrier carrier : carriers.getCarriers().values()){
			Carrier carrierWS = CarrierImpl.newInstance(carrier.getId());
			copyShipments(carrierWS, carrier);
			//			copyPickups(carrierWS, carrier);	//Not implemented yet due to missing CarrierPickup in freight contrib, kmt Sep18
			//			copyDeliveries(carrierWS, carrier); //Not implemented yet due to missing CarrierDelivery in freight contrib, kmt Sep18
			createShipmentsFromServices(carrierWS, carrier); 
			carrierWS.setCarrierCapabilities(carrier.getCarrierCapabilities()); //vehicles and other carrierCapabilites
			carriersWithShipments.addCarrier(carrierWS);
		}
		return carriersWithShipments;
	}

	/**
	 * NOT implemented yet due to missing CarrierDelivery in freight contrib, kmt Sep18
	 * @param carrierWS
	 * @param carrier
	 */
	private void copyDeliveries(Carrier carrierWS, Carrier carrier) {
		log.error("Coping of Deliveries is NOT implemented yet due to missing CarrierDelivery in freight contrib");
	}

	/**
	 * NOT implemented yet due to missing CarrierPickup in freight contrib, kmt Sep18
	 * @param carrierWS
	 * @param carrier
	 */
	private void copyPickups(Carrier carrierWS, Carrier carrier) {
		log.error("Coping of Pickup is NOT implemented yet due to missing CarrierPickup in freight contrib");
	}

	/**
	 * Copy all shipments from the existing carrier to the new carrier with shipments.
	 * @param carrierWS		the "new" carrier with Shipments
	 * @param carrier		the already existing
	 */
	private static void copyShipments(Carrier carrierWS, Carrier carrier) {
		for (CarrierShipment carrierShipment: carrier.getShipments()){
			carrierWS.getShipments().add(carrierShipment);
		}
	}

	/**
	 * Transform all services from the existing carrier to the new carrier with shipments.
	 * The location of the depot from which the "old" carrier starts the tour to the service is used as fromLocation for the new Shipment.
	 * @param carrierWS		the "new" carrier with Shipments
	 * @param carrier		the already existing
	 */
	private static void createShipmentsFromServices(Carrier carrierWS, Carrier carrier) {
		TreeMap<Id<CarrierService>, Id<Link>> depotServiceIsdeliveredFrom = new TreeMap<Id<CarrierService>, Id<Link>>();
		try {
			carrier.getSelectedPlan();
		} catch (Exception e) {
			throw new RuntimeException("Carrier " + carrier.getId() + " has NO selectedPlan. --> CanNOT create a new carrier from solution");
		}
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
