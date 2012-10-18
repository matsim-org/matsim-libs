package org.matsim.contrib.freight.carrier;

public class ScheduledTour {

	private final Tour tour;

	private final CarrierVehicle vehicle;

	private final double departure;

	public ScheduledTour(final Tour tour, final CarrierVehicle vehicle, final double departure) {
		this.tour = tour;
		this.vehicle = vehicle;
		this.departure = departure;
	}

	public Tour getTour() {
		return tour;
	}

	public CarrierVehicle getVehicle() {
		return vehicle;
	}

	public double getDeparture() {
		return departure;
	}

	@Override
	public String toString() {
		return tour.toString() + " on vehicle " + vehicle.toString();
	}

}
