package org.matsim.contrib.freight.carrier;

import java.util.List;

import org.matsim.api.core.v01.Id;

public class CarrierFactory {
	
	public Carrier createCarrier(Id carrierId, Id linkId){
		return new CarrierImpl(carrierId, linkId);
	}
	
	public CarrierVehicle createVehicle(Id vehicleId, Id linkId){
		return new CarrierVehicle(vehicleId, linkId);
	}
	
	public CarrierShipment createShipment(Id from, Id to, int size, TimeWindow pickupTW, TimeWindow deliveryTW){
		return new CarrierShipment(from, to, size, pickupTW, deliveryTW);
	}
	
	public TimeWindow createTimeWindow(double start, double end){
		return new TimeWindow(start,end);
	}
	
	public CarrierCapabilities createCapabilities(){
		return new CarrierCapabilities();
	}
	
	public CarrierContract createContract(CarrierShipment shipment){
		return new CarrierContract(shipment, null);
	}
	
	public ScheduledTour createScheduledTour(Tour tour, CarrierVehicle vehicle, double departureTime){
		return new ScheduledTour(tour, vehicle, departureTime);
	}
	
	public TourBuilder createTourBuilder(){
		return new TourBuilder();
	}

	public Carriers createCarriers() {
		return new Carriers();
	}

	public CarrierPlan createPlan(List<ScheduledTour> sTours) {
		return new CarrierPlan(sTours);
	}

	public void createVehicle(Id vehicleId, Id linkId, int capacity, double earliestDep, double latestArr) {
		CarrierVehicle veh = createVehicle(vehicleId, linkId);
		veh.setCapacity(capacity);
		veh.setEarliestStartTime(earliestDep);
		veh.setLatestEndTime(latestArr);
	}

}
