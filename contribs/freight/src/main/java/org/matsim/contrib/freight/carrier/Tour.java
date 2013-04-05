package org.matsim.contrib.freight.carrier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Route;
import org.matsim.contrib.freight.carrier.CarrierShipment.TimeWindow;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;

/**
 * This is a tour of a carrier which is a sequence of activities and legs.
 * 
 * 
 * @author sschroeder, mzilske
 *
 */
public class Tour {

	/**
	 * A builder building a tour.
	 * 
	 * 
	 * @author sschroeder
	 *
	 */
	public static class Builder {
		
		/**
		 * Returns a new tour builder.
		 * 
		 * @return the builder
		 */
		public static Builder newInstance(){ return new Builder(); }
		
		private Builder(){
			
		}

		private List<TourElement> tourElements = new ArrayList<TourElement>();

		private Set<CarrierShipment> openPickups = new HashSet<CarrierShipment>();

		private Id startLinkId;

		private Id endLinkId;

		private double earliestDeparture = 0.0;

		private double latestDeparture = Integer.MAX_VALUE;

		private boolean previousElementIsActivity;

		
		/**
		 * Schedules the earliest possible time the tour can start.
		 * 
		 * <p>default is 0.0
		 * 
		 * @param earliestDeparture
		 * @return tour builder
		 */
		public Builder scheduleEarliestDeparture(double earliestDeparture){
			this.earliestDeparture = earliestDeparture;
			return this;
		}
		
		/**
		 * Schedules the earliest possible time the tour can start.
		 * 
		 * <p>default is Integer.MaxValue()
		 * 
		 * @param latestDeparture
		 * @return tour builder
		 */
		public Builder scheduleLatestDeparture(double latestDeparture){
			this.latestDeparture = latestDeparture;
			return this;
		}
		
		/**
		 * Schedules the start of the tour.
		 * 
		 * <p> Tour start should correspond to the locationId of the vehicle that runs the tour. 
		 * 
		 * @param startLinkId
		 * @return the builder again
		 */
		public Builder scheduleStart(Id startLinkId) {
			this.startLinkId = startLinkId;
			previousElementIsActivity = true;
			return this;
		}

		/**
		 * Schedules the end of the tour (in terms of locationId).
		 * 
		 * @param endLinkId
		 * @return the builder
		 */
		public Builder scheduleEnd(Id endLinkId) {
			assertLastElementIsLeg();
			this.endLinkId = endLinkId;
			return this;
		}

		/**
		 * Adds a leg to the currentTour.
		 * 
		 * <p>Consider that a leg follows an activity. Otherwise an exception occurs.
		 * 
		 * @param leg
		 * @throws IllegalStateException if leg is null or if previous element is not an activity.
		 */
		public Builder addLeg(Leg leg) {
			assertIsNotNull(leg);
			if (!previousElementIsActivity) {
				throw new IllegalStateException("cannot add leg, since last tour element is not an activity.");
			}
			tourElements.add(leg);
			previousElementIsActivity = false;
			return this;
		}
		
		/**
		 * Inserts leg at the beginning of a tour.
		 * 
		 * @param leg
		 * @return the builder
		 * @throws IllegalStateException if leg is null
		 */
		public Builder insertLegAtBeginning(Leg leg) {
			assertIsNotNull(leg);
//			if (!previousElementIsActivity) {
//				throw new RuntimeException(
//						"cannot add leg, since last tour element is not an activity.");
//			}
			tourElements.add(0,leg);
//			previousElementIsActivity = false;
			return this;
		}

		private void assertIsNotNull(Object o) {
			if (o == null) {
				throw new IllegalStateException("leg cannot be null");
			}

		}

		/**
		 * Schedules a pickup of a shipment with an expected endTime.
		 * 
		 * @param shipment
		 * @param end_time
		 * @return the builder
		 * @throws IllegalStateException if shipment is null or if shipment has already been picked up or if last element is not a leg.
		 */
		public Builder schedulePickup(CarrierShipment shipment, double end_time) {
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
			return this;
		}
		
		/**
		 * Schedules a the pickup of the shipment right at the beginning of the tour.
		 * 
		 * @param shipment
		 * @return the builder
		 * @throws IllegalStateException if shipment is null or shipment has already been picked up.
		 */
		public Builder schedulePickupAtBeginning(CarrierShipment shipment) {
			assertIsNotNull(shipment);
			boolean wasNew = openPickups.add(shipment);
			if (!wasNew) {
				throw new IllegalStateException("Trying to deliver something which was already picked up.");
			}
//			assertLastElementIsLeg();
			Pickup pickup = createPickup(shipment);
			tourElements.add(0, pickup);
//			previousElementIsActivity = true;
			return this;
		}

		/**
		 * Schedules pickup, i.e. adds a pickup to current tour.
		 * 
		 * 
		 * @param shipment to be picked up
		 * @return the builder
		 * @throws IllegalStateException if shipment is null or if shipment has already been picked up or if last element is not a leg.
		 */
		public Builder schedulePickup(CarrierShipment shipment) {
			assertIsNotNull(shipment);
			boolean wasNew = openPickups.add(shipment);
			if (!wasNew) {
				throw new IllegalStateException("Trying to deliver something which was already picked up.");
			}
			assertLastElementIsLeg();
			Pickup pickup = createPickup(shipment);
			tourElements.add(pickup);
			previousElementIsActivity = true;
			return this;
		}

		private void assertLastElementIsLeg() {
			if (previousElementIsActivity) {
				throw new RuntimeException(
						"cannot add activity, since last tour element is not a leg.");
			}
		}

		/**
		 * Schedules a delivery of a shipment with an expected end time, i.e. adds a delivery activity to current tour.
		 * 
		 * @param shipment
		 * @param end_time
		 * @return the builder
		 * @throws IllegalStateException if shipment is null or if shipment has not been picked up yet or if last element is not a leg.
		 */
		public Builder scheduleDelivery(CarrierShipment shipment, double end_time) {
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
			return this;
		}

		/**
		 * Schedules a delivery of a shipment, i.e. adds a delivery activity to current tour.
		 * 
		 * @param shipment
		 * @param end_time
		 * @return the builder
		 * @throws IllegalStateException if shipment is null or if shipment has not been picked up yet or if last element is not a leg.
		 */
		public Builder scheduleDelivery(CarrierShipment shipment) {
			assertIsNotNull(shipment);
			boolean wasOpen = openPickups.remove(shipment);
			if (!wasOpen) {
				throw new IllegalStateException("Trying to deliver something which was not picked up.");
			}
			assertLastElementIsLeg();
			tourElements.add(createDelivery(shipment));
			previousElementIsActivity = true;
			return this;
		}

		/**
		 * Finally builds the tour.
		 * 
		 * @return the tour that has been built
		 */
		public Tour build() {
			Tour tour = new Tour(startLinkId, tourElements, endLinkId);
			tour.setEarliestDeparture(earliestDeparture);
			tour.setLatestDeparture(latestDeparture);
			return new Tour(this);
		}

		private Pickup createPickup(CarrierShipment shipment) {
			return new Pickup(shipment);
		}

		private Delivery createDelivery(CarrierShipment shipment) {
			return new Delivery(shipment);
		}

		/**
		 * Creates and returns an empty leg.
		 * 
		 * @return Leg
		 * @see Leg
		 */
		public Leg createLeg() {
			return new Leg();
		}

		/**
		 * Creates and returns a network route.
		 * 
		 * @param startLinkId
		 * @param linkIds
		 * @param endLinkId
		 * @return NetworkRoute
		 * @see NetworkRoute
		 */
		public NetworkRoute createRoute(Id startLinkId, List<Id> linkIds, Id endLinkId) {
			LinkNetworkRouteImpl linkNetworkRouteImpl = new LinkNetworkRouteImpl(startLinkId, endLinkId);
			if (linkIds != null && !linkIds.isEmpty()) {
				linkNetworkRouteImpl.setLinkIds(startLinkId, linkIds, endLinkId);
			}
			return linkNetworkRouteImpl;

		}

	}
	
	public static abstract class TourElement {

		public abstract TourElement duplicate();

	};

	public static abstract class TourActivity extends TourElement {

		public abstract String getActivityType();

		public abstract Id getLocation();

		public abstract double getDuration();

		public abstract TimeWindow getTimeWindow();

		public abstract void setExpectedActStart(double startTime);

		public abstract double getExpectedActStart();

		public abstract void setExpectedArrival(double arrivalTime);

		public abstract double getExpectedArrival();

		public abstract void setExpectedActEnd(double currTime);

		public abstract double getExpectedActEnd();
	}

	public static abstract class ShipmentBasedActivity extends TourActivity {
		public abstract CarrierShipment getShipment();
	}

	public static class Leg extends TourElement {

		private Route route;

		private double expTransportTime;

		private double departureTime;

		public Leg() {
		}

		private Leg(Leg leg) {
			this.expTransportTime = leg.getExpectedTransportTime();
			this.departureTime = leg.getDepartureTime();
			this.route = leg.getRoute().clone();
		}

		public Route getRoute() {
			return route;
		}

		public void setRoute(Route route) {
			this.route = route;
		}

		public double getExpectedTransportTime() {
			return expTransportTime;
		}

		public void setExpectedTransportTime(double transportTime) {
			this.expTransportTime = transportTime;
		}

		public void setDepartureTime(double currTime) {
			this.departureTime = currTime;
		}

		public double getDepartureTime() {
			return departureTime;
		}

		@Override
		public TourElement duplicate() {
			return new Leg(this);
		}
	}

	public static class Pickup extends ShipmentBasedActivity {

		private final CarrierShipment shipment;

		private double expActStartTime;

		private double expActArrTime;

		private double expActEndTime;

		public Pickup(CarrierShipment shipment) {
			this.shipment = shipment;
		}

		private Pickup(Pickup pickup) {
			this.shipment = pickup.getShipment();
			this.expActArrTime = pickup.getExpectedArrival();
			this.expActEndTime = pickup.getExpectedActEnd();
			this.expActStartTime = pickup.getExpectedActStart();
		}

		@Override
		public String getActivityType() {
			return FreightConstants.PICKUP;
		}

		@Override
		public TimeWindow getTimeWindow() {
			return shipment.getPickupTimeWindow();
		}

		@Override
		public Id getLocation() {
			return shipment.getFrom();
		}

		@Override
		public double getDuration() {
			return shipment.getPickupServiceTime();
		}

		@Override
		public CarrierShipment getShipment() {
			return shipment;
		}

		@Override
		public void setExpectedActStart(double startTime) {
			expActStartTime = startTime;
		}

		@Override
		public double getExpectedActStart() {
			return expActStartTime;
		}

		@Override
		public void setExpectedArrival(double arrivalTime) {
			expActArrTime = arrivalTime;

		}

		@Override
		public double getExpectedArrival() {
			return expActArrTime;
		}

		@Override
		public void setExpectedActEnd(double currTime) {
			this.expActEndTime = currTime;
		}

		@Override
		public double getExpectedActEnd() {
			return this.expActEndTime;

		}

		@Override
		public TourElement duplicate() {
			return new Pickup(this);
		}

	};

	public static class Delivery extends ShipmentBasedActivity {

		private final CarrierShipment shipment;

		private double expActStartTime;

		private double expArrTime;

		private double expActEndTime;

		public Delivery(CarrierShipment shipment) {
			this.shipment = shipment;
		}

		Delivery(Delivery delivery) {
			this.shipment = delivery.getShipment();
			this.expArrTime = delivery.getExpectedArrival();
			this.expActEndTime = delivery.getExpectedActEnd();
			this.expActStartTime = delivery.getExpectedActStart();
		}

		@Override
		public TimeWindow getTimeWindow() {
			return shipment.getDeliveryTimeWindow();
		}

		@Override
		public String getActivityType() {
			return FreightConstants.DELIVERY;
		}

		@Override
		public Id getLocation() {
			return shipment.getTo();
		}

		@Override
		public double getDuration() {
			return shipment.getDeliveryServiceTime();
		}

		@Override
		public CarrierShipment getShipment() {
			return shipment;
		}

		@Override
		public void setExpectedActStart(double startTime) {
			expActStartTime = startTime;
		}

		@Override
		public double getExpectedActStart() {
			return expActStartTime;
		}

		@Override
		public void setExpectedArrival(double arrivalTime) {
			expArrTime = arrivalTime;
		}

		@Override
		public double getExpectedArrival() {
			return expArrTime;
		}

		@Override
		public void setExpectedActEnd(double currTime) {
			this.expActEndTime = currTime;

		}

		@Override
		public double getExpectedActEnd() {
			return this.expActEndTime;
		}

		@Override
		public TourElement duplicate() {
			return new Delivery(this);
		}

	};

	private final List<TourElement> tourElements;

	private final Id startLinkId;

	private final Id endLinkId;

	private double earliestDeparture;

	private double latestDeparture;
	
	private Tour(Builder builder){
		tourElements = builder.tourElements;
		startLinkId = builder.startLinkId;
		endLinkId = builder.endLinkId;
		earliestDeparture = builder.earliestDeparture;
		latestDeparture = builder.latestDeparture;
	}

	Tour(final Id startLinkId, final List<TourElement> tourElements,
			final Id endLinkId) {
		this.startLinkId = startLinkId;
		this.tourElements = Collections.unmodifiableList(tourElements);
		this.endLinkId = endLinkId;
		this.earliestDeparture = 0.0;
		this.latestDeparture = 0.0;
	}

	private Tour(Tour tour) {
		this.startLinkId = tour.getStartLinkId();
		List<TourElement> elements = new ArrayList<Tour.TourElement>();
		for (TourElement element : tour.getTourElements()) {
			elements.add(element.duplicate());
		}
		this.tourElements = elements;
		this.endLinkId = tour.getEndLinkId();
		this.earliestDeparture = tour.getEarliestDeparture();
		this.latestDeparture = tour.getLatestDeparture();
	}

	public Tour duplicate() {
		return new Tour(this);
	}

	public List<TourElement> getTourElements() {
		return Collections.unmodifiableList(tourElements);
	}

	/**
	 * Returns the list of shipments in this tour.
	 * 
	 * <p>It retrieves the shipments by going through the tour-elements. Once a pickup activity occurs the picked shipment is 
	 * added to the list to be returned. 
	 * 
	 * @return a list with carrierShipment
	 * @see CarrierShipment
	 */
	public List<CarrierShipment> getShipments() {
		List<CarrierShipment> shipments = new ArrayList<CarrierShipment>();
		for (TourElement tourElement : tourElements) {
			if (tourElement instanceof Pickup) {
				Pickup pickup = (Pickup) tourElement;
				shipments.add(pickup.shipment);
			}
		}
		return shipments;
	}

	public Id getStartLinkId() {
		return startLinkId;
	}

	public Id getEndLinkId() {
		return endLinkId;
	}

	public double getEarliestDeparture() {
		return earliestDeparture;
	}

	public void setEarliestDeparture(double earliestDeparture) {
		this.earliestDeparture = earliestDeparture;
	}

	public double getLatestDeparture() {
		return latestDeparture;
	}

	public void setLatestDeparture(double latestDeparture) {
		this.latestDeparture = latestDeparture;
	}
	
	@Override
	public String toString() {
		return "[startLinkId="+startLinkId+"][endLinkId="+endLinkId+"[nOfTourElements=" + tourElements.size() + "]" +
				"[earliestDepartureTime=" + earliestDeparture + "][latestDepartureTime=" + latestDeparture + "]";
	}

}
