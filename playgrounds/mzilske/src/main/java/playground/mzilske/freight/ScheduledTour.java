package playground.mzilske.freight;


public class ScheduledTour {

	private Tour tour;
	private CarrierVehicle vehicle;
	private double departure;

	public ScheduledTour(Tour tour, CarrierVehicle firstVehicle, double departure) {
		this.tour = tour;
		this.vehicle = firstVehicle;
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
