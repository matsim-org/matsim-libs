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

/**
 * A concrete assignment of a tour, a vehicle and a departure time.
 * 
 * @author sschroeder, mzilske
 *
 */
public class ScheduledTour {

	/**
	 * A builder building a scheduledTour.
	 * 
	 * @author sschroeder
	 *
	 */
	public static class Builder {

		/**
		 * Returns the builder with a vehicle that runs the tour.
		 * 
		 * @param vehicle
		 * @return Builder
		 */
		public static Builder newInstance(CarrierVehicle vehicle){
			return new Builder(vehicle);
		}
		
		private List<TourElement> tourElements = new ArrayList<TourElement>();

		private Set<CarrierShipment> openPickups = new HashSet<CarrierShipment>();

		private double startTime;

		private boolean previousElementIsActivity;

		private CarrierVehicle vehicle;

		private Builder(CarrierVehicle vehicle) {
			super();
			this.vehicle = vehicle;
		}

		/**
		 * Schedules the start time of the tour.
		 * 
		 * @param time
		 * @return Builder
		 */
		public Builder scheduleStart(double time) {
			this.startTime = time;
			previousElementIsActivity = true;
			return this;
		}

		/**
		 * Schedules the end.
		 * 
		 * <p>This just checks whether the last tour-element so far is a leg.
		 * @return the builder
		 * @throws IllegalStateException if the last tour-element is not a leg.
		 */
		public Builder scheduleEnd() {
			assertLastElementIsLeg();
			return this;
		}

		/**
		 * Schedules a leg with a route, a departure-time and an expected transport-time.
		 * 
		 * @param route
		 * @param dep_time
		 * @param transportTime
		 * @return the builder
		 * @throws IllegalStateException if previous element is not an activity.
		 */
		public Builder scheduleLeg(Route route, double dep_time, double transportTime) {
			if (!previousElementIsActivity) {
				throw new IllegalStateException("cannot add leg, since last tour element is not an activity.");
			}
			Leg leg = createLeg(route, dep_time, transportTime);
			tourElements.add(leg);
			previousElementIsActivity = false;
			return this;
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
				throw new IllegalStateException("leg cannot be null");
			}

		}

		/**
		 * Schedules the pickup of a shipment with an expected end-time, i.e. adds a pickup activity to current tour.
		 * 
		 * @param shipment
		 * @param end_time
		 * @return this builder
		 * @throws IllegalStateException if shipment is null or if shipment has already been picked up or if last element is not a leg.
		 */
		public void schedulePickup(CarrierShipment shipment, double end_time) {
			assertIsNotNull(shipment);
			boolean wasNew = openPickups.add(shipment);
			if (!wasNew) {
				throw new IllegalStateException("Trying to deliver something which was already picked up.");
			}
			assertLastElementIsLeg();
			Pickup pickup = createPickup(shipment);
			pickup.setExpectedActEnd(end_time);
			tourElements.add(pickup);
			previousElementIsActivity = true;
		}

		private void assertLastElementIsLeg() {
			if (previousElementIsActivity) {
				throw new IllegalStateException("cannot add activity, since last tour element is not a leg.");
			}
		}

		/**
		 * Schedules the delivery of a shipment with an expected end-time, i.e. adds a delivery activity to current tour.
		 * 
		 * @param shipment
		 * @param end_time
		 * @return this builder
		 * @throws IllegalStateException if shipment is null or if shipment has not yet been picked up or if last element is not a leg.
		 */
		public void scheduleDelivery(CarrierShipment shipment, double end_time) {
			assertIsNotNull(shipment);
			boolean wasOpen = openPickups.remove(shipment);
			if (!wasOpen) {
				throw new IllegalStateException("Trying to deliver something which was not picked up.");
			}
			assertLastElementIsLeg();
			Delivery delivery = createDelivery(shipment);
			delivery.setExpectedActEnd(end_time);
			tourElements.add(delivery);
			previousElementIsActivity = true;
		}

		/**
		 * Finally builds the scheduledTour.
		 * 
		 * <p>Note that this builder builds a closed tour starting and ending at the vehicle's location, i.e. it retrieves the 
		 * locationId of the carrierVehicle and sets tour start end end to that location.
		 * 
		 * @return ScheduledTour
		 * @see ScheduledTour
		 */
		public ScheduledTour build() {
			Tour tour = new Tour(vehicle.getLocation(), tourElements, vehicle.getLocation());
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
