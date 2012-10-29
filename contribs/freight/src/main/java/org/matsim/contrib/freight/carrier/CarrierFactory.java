package org.matsim.contrib.freight.carrier;

import java.util.ArrayList;
import java.util.Collection;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.freight.carrier.CarrierShipment.TimeWindow;
import org.matsim.contrib.freight.carrier.CarrierVehicleType.VehicleCostInformation;
import org.matsim.core.basic.v01.IdImpl;

public class CarrierFactory {

	public Carrier createCarrier(String carrierId, String linkId) {
		return new CarrierImpl(createId(carrierId), createId(linkId));
	}

	public CarrierVehicle createVehicle(Id vehicleId, Id linkId) {
		return new CarrierVehicle(vehicleId, linkId);
	}

	public CarrierShipment createShipment(Id from, Id to, int size,TimeWindow pickupTW, TimeWindow deliveryTW) {
		return new CarrierShipment(from, to, size, pickupTW, deliveryTW);
	}

	public CarrierShipment createShipment(String from, String to, int size,double pickupStart, double pickupEnd, double deliveryStart,double deliveryEnd) {
		return createShipment(createId(from), createId(to), size,createTimeWindow(pickupStart, pickupEnd),createTimeWindow(deliveryStart, deliveryEnd));
	}

	public TimeWindow createTimeWindow(double start, double end) {
		return new TimeWindow(start, end);
	}

	public CarrierCapabilities createCapabilities() {
		return new CarrierCapabilities();
	}

	public ScheduledTour createScheduledTour(Tour tour, CarrierVehicle vehicle,double departureTime) {
		return new ScheduledTour(tour, vehicle, departureTime);
	}

	public TourBuilder createTourBuilder() {
		return new TourBuilder();
	}

	public Carriers createCarriers() {
		return new Carriers();
	}

	public Id createId(String id) {
		return new IdImpl(id);
	}

	public CarrierPlan createPlan(Carrier carrier, Collection<ScheduledTour> sTours) {
		return new CarrierPlan(carrier, sTours);
	}

	public CarrierVehicleType createVehicleType(String id, double fix,double costPerTimeUnit, double costPerDistanceUnit) {
		CarrierVehicleType t = new CarrierVehicleType(createId(id));
		t.setVehicleCostParams(new VehicleCostInformation(fix,costPerDistanceUnit, costPerTimeUnit));
		return t;
	}

	public CarrierVehicle createVehicle(String vehicleId,String vehicleLocationId, int vehicleCapacity,CarrierVehicleType vehicleType) {
		CarrierVehicle vehicle = new CarrierVehicle(createId(vehicleId),createId(vehicleLocationId));
		vehicle.setCapacity(vehicleCapacity);
		// vehicleType.setFreightCapacity(vehicleCapacity);
		vehicle.setVehicleType(vehicleType);
		return vehicle;
	}

	public CarrierVehicle createAndAddVehicle(Carrier carrier,String vehicleId, String vehicleLocationId, int vehicleCapacity,String type, double earliestOperationStart,double latestOperationEnd) {
		CarrierVehicle v = createAndAddVehicle(carrier, vehicleId,vehicleLocationId, vehicleCapacity, type);
		v.setEarliestStartTime(earliestOperationStart);
		v.setLatestEndTime(latestOperationEnd);
		return v;
	}

	public CarrierVehicle createAndAddVehicle(Carrier carrier,String vehicleId, String vehicleLocationId, int vehicleCapacity,String type) {
		CarrierVehicle vehicle = new CarrierVehicle(createId(vehicleId),createId(vehicleLocationId));
		vehicle.setCapacity(vehicleCapacity);
		CarrierVehicleType vehicleType = new CarrierVehicleType(createId(type));
		// vehicleType.setFreightCapacity(vehicleCapacity);
		vehicle.setVehicleType(vehicleType);
		if (carrier.getCarrierCapabilities() != null) {
			carrier.getCarrierCapabilities().getCarrierVehicles().add(vehicle);
		} else {
			CarrierCapabilities caps = new CarrierCapabilities();
			caps.getCarrierVehicles().add(vehicle);
			carrier.setCarrierCapabilities(caps);
		}
		return vehicle;
	}

	public Collection<CarrierVehicle> getVehicles(CarrierCapabilities carrierCapabilities) {
		return new ArrayList<CarrierVehicle>(carrierCapabilities.getCarrierVehicles());
	}

	public CarrierVehicleType createDefaultVehicleType() {
		return createVehicleType("default", 0, 0.0, 1.0);
	}

}
