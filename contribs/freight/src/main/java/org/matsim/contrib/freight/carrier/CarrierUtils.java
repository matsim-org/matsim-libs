package org.matsim.contrib.freight.carrier;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.vehicles.Vehicle;

import java.util.ArrayList;
import java.util.List;

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

	private static final String CARRIER_MODE = "carrierMode" ;
	public static String getCarrierMode( Carrier carrier ) {
		String result = (String) carrier.getAttributes().getAttribute( CARRIER_MODE );
		if ( result == null ){
			return TransportMode.car ;
		} else {
			return result ;
		}
	}
	public static void setCarrierMode( Carrier carrier,  String carrierMode ) {
		carrier.getAttributes().putAttribute( CARRIER_MODE, carrierMode ) ;
	}

	private static final String JSPRIT_ITERATIONS="jspritIterations" ;
	public static int getJspritIterations( Carrier carrier ) {
		Integer result = (Integer) carrier.getAttributes().getAttribute( JSPRIT_ITERATIONS );
		return (int) result ;
	}
	public static void setJspritIterations( Carrier carrier, int jspritIterations ) {
		carrier.getAttributes().putAttribute( JSPRIT_ITERATIONS , jspritIterations ) ;
	}

}
