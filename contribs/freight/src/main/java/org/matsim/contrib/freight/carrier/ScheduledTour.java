package org.matsim.contrib.freight.carrier;


/**
 * A concrete assignment of a tour, a vehicle and a departure time.
 * 
 * @author sschroeder, mzilske
 *
 */
public class ScheduledTour {

	
	/**
	 * Returns a new instance of ScheduledTour.
	 * 
	 * <p>Look at the builder. It might be easier to build a scheduled tour. 
	 * You get the builder this way: ScheduledTour.Builder.newInstance(carrierVehicle).
	 * 
	 * @param tour
	 * @param vehicle
	 * @param departureTime
	 * @return a scheduledTour
	 * @see ScheduledTour
	 */
	public static ScheduledTour newInstance(Tour tour, CarrierVehicle vehicle, double departureTime){
		return new ScheduledTour(tour,vehicle,departureTime);
	}
	
	private final Tour tour;

	private final CarrierVehicle vehicle;

	private final double departureTime;

	private ScheduledTour(final Tour tour, final CarrierVehicle vehicle, final double departureTime) {
		this.tour = tour;
		this.vehicle = vehicle;
		this.departureTime = departureTime;
	}

	public Tour getTour() {
		return tour;
	}

	public CarrierVehicle getVehicle() {
		return vehicle;
	}

	public double getDeparture() {
		return departureTime;
	}

	@Override
	public String toString() {
		return "[tour="+tour+"][vehicle="+vehicle+"][departureTime="+departureTime+"]";
	}

}
