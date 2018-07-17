package org.matsim.contrib.freight.carrier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jfree.util.Log;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;

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

		private boolean previousElementIsActivity;

		private Start start;
		
		private End end;
		
		/**
		 * Schedules the start of the tour.
		 * 
		 * <p> Tour start should correspond to the locationId of the vehicle that runs the tour. 
		 * 
		 * @param startLinkId
		 * @return the builder again
		 */
		public Builder scheduleStart(Id<Link> startLinkId) {
			scheduleStart(startLinkId, TimeWindow.newInstance(0.0, Double.MAX_VALUE));
			return this;
		}
		
		public Builder scheduleStart(Id<Link> startLinkId, TimeWindow timeWindow){
			this.start = new Start(startLinkId, timeWindow);
			previousElementIsActivity = true;
			return this;
		}
		
		public Builder scheduleEnd(Id<Link> endLinkId, TimeWindow timeWindow){
			assertLastElementIsLeg();
			End end = new End(endLinkId, timeWindow);
			this.end = end;
			previousElementIsActivity = true;
			return this;
		}
		
		/**
		 * Schedules the end of the tour (in terms of locationId).
		 * 
		 * @param endLinkId
		 * @return the builder
		 */
		public Builder scheduleEnd(Id<Link> endLinkId) {
			scheduleEnd(endLinkId, TimeWindow.newInstance(0.0, Double.MAX_VALUE));
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
		
		public Leg createLeg(Route route, double dep_time, double transportTime) {
			Leg leg = new Leg();
			leg.setRoute(route);
			leg.setDepartureTime(dep_time);
			leg.setExpectedTransportTime(transportTime);
			return leg;
		}
		
		/**
		 * Inserts leg at the beginning of a tour.
		 * 
		 * @param leg
		 * @return the builder
		 * @throws IllegalStateException if leg is null
		 */
		@Deprecated
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
		 * Schedules a the pickup of the shipment right at the beginning of the tour.
		 * 
		 * @param shipment
		 * @return the builder
		 * @throws IllegalStateException if shipment is null or shipment has already been picked up.
		 */
		@Deprecated
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
			Log.debug("Pickup to get scheduled: " + shipment.toString());
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
		 * Schedules a delivery of a shipment, i.e. adds a delivery activity to current tour.
		 * 
		 * @param shipment
		 * @param end_time
		 * @return the builder
		 * @throws IllegalStateException if shipment is null or if shipment has not been picked up yet or if last element is not a leg.
		 */
		public Builder scheduleDelivery(CarrierShipment shipment) {
			assertIsNotNull(shipment);
			Log.debug("Delivery to get scheduled: " + shipment.toString());
			Log.debug("OpenPickups: " + openPickups.toString());
			for (CarrierShipment s : openPickups) {
				if (s.equals(shipment)) {
					shipment = s;
				};
			}
			boolean wasOpen = openPickups.remove(shipment);
			if (!wasOpen) {
				throw new IllegalStateException("Trying to deliver something which was not picked up.");
			}
			assertLastElementIsLeg();
			tourElements.add(createDelivery(shipment));
			previousElementIsActivity = true;
			return this;
		}
		
		public Builder scheduleService(CarrierService service){
			ServiceActivity act = new ServiceActivity(service);
			assertLastElementIsLeg();
			tourElements.add(act);
			previousElementIsActivity = true;
			return this;
		}

		/**
		 * Finally builds the tour.
		 * 
		 * @return the tour that has been built
		 */
		public Tour build() {
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
		public NetworkRoute createRoute(Id<Link> startLinkId, List<Id<Link>> linkIds, Id<Link> endLinkId) {
			NetworkRoute linkNetworkRouteImpl = RouteUtils.createLinkNetworkRouteImpl(startLinkId, endLinkId);
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

		public abstract Id<Link> getLocation();

		public abstract double getDuration();

		public abstract TimeWindow getTimeWindow();

		public abstract void setExpectedArrival(double arrivalTime);

		public abstract double getExpectedArrival();
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
			this.departureTime = leg.getExpectedDepartureTime();
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

		public double getExpectedDepartureTime() {
			return departureTime;
		}

		@Override
		public TourElement duplicate() {
			return new Leg(this);
		}
	}
	
	public static class ServiceActivity extends TourActivity {

		private CarrierService service;
		
		private double arrTime;
		
		public ServiceActivity(CarrierService service) {
			super();
			this.service = service;
		}

		private ServiceActivity(ServiceActivity serviceActivity) {
			this.service = serviceActivity.getService();
			this.arrTime = serviceActivity.getExpectedArrival();
		}

		public CarrierService getService(){
			return service;
		}
		
		@Override
		public String getActivityType() {
			return service.getType();
		}

		@Override
		public Id<Link> getLocation() {
			return service.getLocationLinkId();
		}

		@Override
		public double getDuration() {
			return service.getServiceDuration();
		}

		@Override
		public TimeWindow getTimeWindow() {
			return service.getServiceStartTimeWindow();
		}

		@Override
		public void setExpectedArrival(double arrivalTime) {
			this.arrTime = arrivalTime;
		}

		@Override
		public double getExpectedArrival() {
			return arrTime;
		}

		@Override
		public TourElement duplicate() {
			return new ServiceActivity(this);
		}
		
	}

	public static class Start extends TourActivity {

		private Id<Link> locationLinkId;
		
		private TimeWindow timeWindow;
		
		public Start(Id<Link> locationLinkId, TimeWindow timeWindow) {
			super();
			this.locationLinkId = locationLinkId;
			this.timeWindow = timeWindow;
		}

		private Start(Start start) {
			this.locationLinkId = start.getLocation();
			this.timeWindow = start.getTimeWindow();
		}

		@Override
		public String getActivityType() {
			return "start";
		}

		@Override
		public Id<Link> getLocation() {
			return locationLinkId;
		}

		@Override
		public double getDuration() {
			return 0;
		}

		@Override
		public TimeWindow getTimeWindow() {
			return timeWindow;
		}

		@Override
		public void setExpectedArrival(double arrivalTime) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public double getExpectedArrival() {
			return 0;
		}

		@Override
		public TourElement duplicate() {
			return new Start(this);
		}
		
	}
	
	public static class End extends TourActivity {
		private Id<Link> locationLinkId;
		
		private TimeWindow timeWindow;

		private double arrTime;
		
		public End(Id<Link> locationLinkId, TimeWindow timeWindow) {
			super();
			this.locationLinkId = locationLinkId;
			this.timeWindow = timeWindow;
		}

		private End(End end) {
			this.locationLinkId = end.getLocation();
			this.timeWindow = end.getTimeWindow();
		}

		@Override
		public String getActivityType() {
			return "end";
		}

		@Override
		public Id<Link> getLocation() {
			return locationLinkId;
		}

		@Override
		public double getDuration() {
			return 0;
		}

		@Override
		public TimeWindow getTimeWindow() {
			return timeWindow;
		}

		@Override
		public void setExpectedArrival(double arrivalTime) {
			this.arrTime = arrivalTime;
		}

		@Override
		public double getExpectedArrival() {
			return this.arrTime;
		}

		@Override
		public TourElement duplicate() {
			return new End(this);
		}
	}
	
	public static class Pickup extends ShipmentBasedActivity {

		private final CarrierShipment shipment;

		private double expActArrTime;

		public Pickup(CarrierShipment shipment) {
			this.shipment = shipment;
		}

		private Pickup(Pickup pickup) {
			this.shipment = pickup.getShipment();
			this.expActArrTime = pickup.getExpectedArrival();
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
		public Id<Link> getLocation() {
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
		public void setExpectedArrival(double arrivalTime) {
			expActArrTime = arrivalTime;

		}

		@Override
		public double getExpectedArrival() {
			return expActArrTime;
		}

		@Override
		public TourElement duplicate() {
			return new Pickup(this);
		}

	};

	public static class Delivery extends ShipmentBasedActivity {

		private final CarrierShipment shipment;

		private double expArrTime;

		public Delivery(CarrierShipment shipment) {
			this.shipment = shipment;
		}

		Delivery(Delivery delivery) {
			this.shipment = delivery.getShipment();
			this.expArrTime = delivery.getExpectedArrival();
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
		public Id<Link> getLocation() {
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
		public void setExpectedArrival(double arrivalTime) {
			expArrTime = arrivalTime;
		}

		@Override
		public double getExpectedArrival() {
			return expArrTime;
		}
		
		@Override
		public TourElement duplicate() {
			return new Delivery(this);
		}

	};

	private final List<TourElement> tourElements;
	
	private Start start;
	
	private End end;
	
	private Tour(Builder builder){
		tourElements = builder.tourElements;
		start = builder.start;
		end = builder.end;
	}

	private Tour(Tour tour) {
		this.start = (Start) tour.start.duplicate();
		this.end = (End) tour.end.duplicate();
		List<TourElement> elements = new ArrayList<Tour.TourElement>();
		for (TourElement element : tour.getTourElements()) {
			elements.add(element.duplicate());
		}
		this.tourElements = elements;
	}

	public Tour duplicate() {
		return new Tour(this);
	}

	public List<TourElement> getTourElements() {
		return Collections.unmodifiableList(tourElements);
	}
	
	public Start getStart(){
		return start;
	}
	
	public End getEnd(){
		return end;
	}

	public Id<Link> getStartLinkId() {
		return start.getLocation();
	}

	public Id<Link> getEndLinkId() {
		return end.getLocation();
	}
	
	@Override
	public String toString() {
		return "[startLinkId="+getStartLinkId()+"][endLinkId="+getEndLinkId()+"[#tourElements=" + tourElements.size() + "]";
	}

}
