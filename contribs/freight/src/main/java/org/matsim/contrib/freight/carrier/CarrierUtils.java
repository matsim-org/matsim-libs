package org.matsim.contrib.freight.carrier;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.vehicles.EngineInformation;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CarrierUtils{

	private static Logger log = Logger.getLogger(CarrierUtils.class);

	public static Carrier createCarrier( Id<Carrier> id ){
		return new CarrierImpl(id);
	}

	/**
	 * Adds an carrierVehicle to the CarrierCapabilites of the Carrier.
	 * @param carrier
	 * @param carrierVehicle
	 */
	public static void addCarrierVehicle(Carrier carrier, CarrierVehicle carrierVehicle){
		carrier.getCarrierCapabilities().getCarrierVehicles().put(carrierVehicle.getId(), carrierVehicle);
	}

	public static CarrierVehicle getCarrierVehicle(Carrier carrier, Id<Vehicle> vehicleId){
		if(carrier.getCarrierCapabilities().getCarrierVehicles().containsKey(vehicleId)){
			return carrier.getCarrierCapabilities().getCarrierVehicles().get(vehicleId);
		}
		log.error("Vehicle with Id does not exists", new IllegalStateException("vehicle with id " + vehicleId + " is missing in Carrier: " + carrier.getId()));
		return null;
	}

	/**
	 * Adds an {@link CarrierService} to the {@link Carrier}.
	 * @param carrier
	 * @param carrierService
	 */
	public static void addService(Carrier carrier, CarrierService carrierService){
		carrier.getServices().put(carrierService.getId(), carrierService);
	}

	public static CarrierService getService(Carrier carrier, Id<CarrierService> serviceId){
		if(carrier.getServices().containsKey(serviceId)){
			return carrier.getServices().get(serviceId);
		}
		log.error("Service with Id does not exists", new IllegalStateException("Serice with id " + serviceId + " is missing in Carrier: " + carrier.getId()));
		return null;
	}

	/**
	 * Adds an {@link CarrierShipment} to the {@link Carrier}.
	 * @param carrier
	 * @param carrierShipment
	 */
	public static void addShipment(Carrier carrier, CarrierShipment carrierShipment){
		carrier.getShipments().put(carrierShipment.getId(), carrierShipment);
	}

	public static CarrierShipment getShipment(Carrier carrier, Id<CarrierShipment> serviceId){
		if(carrier.getShipments().containsKey(serviceId)){
			return carrier.getShipments().get(serviceId);
		}
		log.error("Shipment with Id does not exists", new IllegalStateException("Serice with id " + serviceId + " is missing in Carrier: " + carrier.getId()));
		return null;
	}
	
	

	public static CarrierPlan copyPlan( CarrierPlan plan2copy ) {
		List<ScheduledTour> tours = new ArrayList<ScheduledTour>();
		for (ScheduledTour sTour : plan2copy.getScheduledTours()) {
			double depTime = sTour.getDeparture();
			CarrierVehicle vehicle = sTour.getVehicle();
			Tour tour = sTour.getTour().duplicate();
			tours.add(ScheduledTour.newInstance(tour, vehicle, depTime));
		}
		CarrierPlan copiedPlan = new CarrierPlan(plan2copy.getCarrier(), tours);
		double initialScoreOfCopiedPlan = plan2copy.getScore();
		copiedPlan.setScore(initialScoreOfCopiedPlan);
		return copiedPlan;

	}

}
