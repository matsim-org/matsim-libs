package org.matsim.contrib.freight.carrier;

import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;

public class CarrierUtils {
	
	public static CarrierImpl createCarrier(String id, String depotLinkId){
		CarrierImpl carrier = new CarrierImpl(makeId(id), makeId(depotLinkId));
		carrier.setCarrierCapabilities(new CarrierCapabilities());
		return carrier;
	}
	
	public static Id createId(String id){
		return new IdImpl(id);
	}

	public static CarrierVehicle createAndAddVehicle(Carrier carrier, String vehicleId, String vehicleLocationId, int vehicleCapacity){
		CarrierVehicle vehicle = new CarrierVehicle(makeId(vehicleId), makeId(vehicleLocationId));
		vehicle.setCapacity(vehicleCapacity);
		if(carrier.getCarrierCapabilities() != null){
			carrier.getCarrierCapabilities().getCarrierVehicles().add(vehicle);
		}
		else{
			CarrierCapabilities caps = new CarrierCapabilities();
			caps.getCarrierVehicles().add(vehicle);
			carrier.setCarrierCapabilities(caps);
		}
		return vehicle;
	}
	
	public static CarrierVehicle createAndAddVehicle(Carrier carrier, String vehicleId, String vehicleLocationId, int vehicleCapacity, double earliestOperationStart, double latestOperationEnd){
		CarrierVehicle v = createAndAddVehicle(carrier, vehicleId, vehicleLocationId, vehicleCapacity);
		v.setEarliestStartTime(earliestOperationStart);
		v.setLatestEndTime(latestOperationEnd);
		return v;
	}
	
	public static TimeWindow createTimeWindow(double start, double end){
		return makeTW(start,end);
	}
	
	public static Offer createOffer(String carrierId){
		Offer offer = new CarrierOffer();
		offer.setId(makeId(carrierId));
		return offer;
	}
	
	public static CarrierShipment createShipment(Id from, Id to, int size, double startPickup, double endPickup, double startDelivery, double endDelivery){
		TimeWindow startTW = makeTW(startPickup, endPickup);
		TimeWindow endTW = makeTW(startDelivery, endDelivery);
		return new CarrierShipment(from,to,size,startTW,endTW);
	}
	
	public static CarrierShipment createShipment(Id from, Id to, int size, double startPickup, double endPickup, double startDelivery, double endDelivery, double pickupTime, double deliveryTime){
		TimeWindow startTW = makeTW(startPickup, endPickup);
		TimeWindow endTW = makeTW(startDelivery, endDelivery);
		CarrierShipment carrierShipment = new CarrierShipment(from,to,size,startTW,endTW);
		carrierShipment.setPickupServiceTime(pickupTime);
		carrierShipment.setDeliveryServiceTime(deliveryTime);
		return carrierShipment;
	}
	
	public static CarrierContract createContract(Id from, Id to, int size, double startPickup, double endPickup, double startDelivery, double endDelivery, double pickupTime, double deliveryTime){
		return new CarrierContract(createShipment(from,to,size,startPickup,endPickup,startDelivery,endDelivery,pickupTime,deliveryTime),new CarrierOffer());
	}
	
	private static TimeWindow makeTW(double start, double end) {
		return new TimeWindow(start, end);
	}
	
	public static CarrierContract createContract(CarrierShipment shipment){
		return new CarrierContract(shipment, new CarrierOffer());
	}
	
	private static Id makeId(String depotLinkId) {
		return new IdImpl(depotLinkId);
	}

	
	
	

}
