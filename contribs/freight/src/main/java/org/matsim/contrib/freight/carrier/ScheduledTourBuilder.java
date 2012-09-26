package org.matsim.contrib.freight.carrier;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.matsim.api.core.v01.population.Route;
import org.matsim.contrib.freight.carrier.Tour.Delivery;
import org.matsim.contrib.freight.carrier.Tour.Leg;
import org.matsim.contrib.freight.carrier.Tour.Pickup;
import org.matsim.contrib.freight.carrier.Tour.TourElement;

public class ScheduledTourBuilder {

	private List<TourElement> tourElements = new ArrayList<TourElement>();

	private Set<CarrierShipment> openPickups = new HashSet<CarrierShipment>();

	private double startTime;

	private boolean previousElementIsActivity;

	private CarrierVehicle vehicle;

	public ScheduledTourBuilder(CarrierVehicle vehicle) {
		super();
		this.vehicle = vehicle;
	}

	public void scheduleStart(double time) {
		this.startTime = time;
		previousElementIsActivity = true;
	}

	public void scheduleEnd() {
		assertLastElementIsLeg();
	}

	public void scheduleLeg(Route route, double dep_time, double transportTime) {
		if (!previousElementIsActivity) {
			throw new RuntimeException(
					"cannot add leg, since last tour element is not an activity.");
		}
		Leg leg = createLeg(route, dep_time, transportTime);
		tourElements.add(leg);
		previousElementIsActivity = false;
	}

	private Leg createLeg(Route route, double dep_time, double transportTime) {
		Leg leg = new Leg();
		leg.setRoute(route);
		leg.setDepartureTime(dep_time);
		leg.setExpectedTransportTime(transportTime);
		return leg;
	}

	private void assertIsNotNull(Object o) {
		if (o == null) {
			throw new RuntimeException("leg cannot be null");
		}

	}

	public void schedulePickup(CarrierShipment shipment, double end_time) {
		assertIsNotNull(shipment);
		boolean wasNew = openPickups.add(shipment);
		if (!wasNew) {
			throw new RuntimeException(
					"Trying to deliver something which was already picked up.");
		}
		assertLastElementIsLeg();
		Pickup pickup = createPickup(shipment);
		pickup.setExpectedActEnd(end_time);
		tourElements.add(pickup);
		previousElementIsActivity = true;
	}

	private void assertLastElementIsLeg() {
		if (previousElementIsActivity) {
			throw new RuntimeException(
					"cannot add activity, since last tour element is not a leg.");
		}
	}

	public void scheduleDelivery(CarrierShipment shipment, double end_time) {
		assertIsNotNull(shipment);
		boolean wasOpen = openPickups.remove(shipment);
		if (!wasOpen) {
			throw new RuntimeException(
					"Trying to deliver something which was not picked up.");
		}
		assertLastElementIsLeg();
		Delivery delivery = createDelivery(shipment);
		delivery.setExpectedActEnd(end_time);
		tourElements.add(delivery);
		previousElementIsActivity = true;
	}

	public ScheduledTour build() {
		Tour tour = new Tour(vehicle.getLocation(), tourElements,
				vehicle.getLocation());
		tour.setEarliestDeparture(startTime);
		tour.setLatestDeparture(startTime);
		ScheduledTour sTour = new ScheduledTour(tour, vehicle, startTime);
		return sTour;
	}

	private Pickup createPickup(CarrierShipment shipment) {
		return new Pickup(shipment);
	}

	private Delivery createDelivery(CarrierShipment shipment) {
		return new Delivery(shipment);
	}

}
