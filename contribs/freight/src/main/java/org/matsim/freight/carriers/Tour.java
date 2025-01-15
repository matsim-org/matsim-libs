/*
 *   *********************************************************************** *
 *   project: org.matsim.*
 *   *********************************************************************** *
 *                                                                           *
 *   copyright       : (C)  by the members listed in the COPYING,        *
 *                     LICENSE and WARRANTY file.                            *
 *   email           : info at matsim dot org                                *
 *                                                                           *
 *   *********************************************************************** *
 *                                                                           *
 *     This program is free software; you can redistribute it and/or modify  *
 *     it under the terms of the GNU General Public License as published by  *
 *     the Free Software Foundation; either version 2 of the License, or     *
 *     (at your option) any later version.                                   *
 *     See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                           *
 *   ***********************************************************************
 *
 */

package org.matsim.freight.carriers;

import java.util.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;

/**
 * This is a tour of a carrier which is a sequence of activities and legs.
 * <p>
 *
 * @author sschroeder, mzilske
 *
 */
public class Tour {

	private static final Logger logger = LogManager.getLogger(Tour.class);

	/**
	 * A builder building a tour.
	 * <p>
	 *
	 * @author sschroeder
	 *
	 */
	public static class Builder {

		private final Id<Tour> tourId;
		private final List<TourElement> tourElements = new ArrayList<>();
		private final Set<CarrierShipment> openPickups = new HashSet<>();
		private boolean previousElementIsActivity;


		private Start start;

		private End end;

		/**
		 * Returns a new tour builder.
		 * This now also includes an Id for this tour.
		 *
		 * @param tourId Id of this tour
		 * @return the builder
		 */
		public static Builder newInstance(Id<Tour> tourId){
			return new Builder(tourId);
		}


		private Builder(Id<Tour> tourId) {
			this.tourId = tourId;
		}

		/**
		 * Schedules the start of the tour.
		 *
		 * <p> Tour start should correspond to the locationId of the vehicle that runs the tour.
		 *
		 * @param startLinkId	linkId of the start location
		 * @return 				the builder again
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

		public void scheduleEnd(Id<Link> endLinkId, TimeWindow timeWindow){
			assertLastElementIsLeg();
			this.end = new End(endLinkId, timeWindow);
			previousElementIsActivity = true;
		}

		/**
		 * Schedules the end of the tour (in terms of locationId).
		 *
		 * @param endLinkId	linkId of the end location
		 */
		public void scheduleEnd(Id<Link> endLinkId) {
			scheduleEnd(endLinkId, TimeWindow.newInstance(0.0, Double.MAX_VALUE));
		}

		/**
		 * Adds a leg to the currentTour.
		 *
		 * <p>Consider that a leg follows an activity. Otherwise, an exception occurs.
		 *
		 * @param leg 						the leg to be added
		 * @throws IllegalStateException 	if leg is null or if previous element is not an activity.
		 */
		public Builder addLeg(Leg leg) {
			Gbl.assertNotNull(leg);
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
		 * @param leg 						the leg to be inserted
		 * @return 							the builder
		 * @throws IllegalStateException 	if leg is null
		 */
		@Deprecated
		public Builder insertLegAtBeginning(Leg leg) {
			Gbl.assertNotNull(leg);
			tourElements.add(0,leg);
			return this;
		}

		/**
		 * Schedules a pickup of the shipment right at the beginning of the tour.
		 *
		 * @param shipment					the shipment to be picked up
		 * @return 							the builder
		 * @throws IllegalStateException 	if shipment is null or shipment has already been picked up.
		 */
		@Deprecated
		public Builder schedulePickupAtBeginning(CarrierShipment shipment) {
			Gbl.assertNotNull(shipment);
			boolean wasNew = openPickups.add(shipment);
			if (!wasNew) {
				throw new IllegalStateException("Trying to deliver something which was already picked up.");
			}
//			assertLastElementIsLeg();
			Pickup pickup = createPickup(shipment);
			tourElements.add(0,pickup);
//			previousElementIsActivity = true;
			return this;
		}

		/**
		 * Schedules pickup, i.e. adds a pickup to current tour.
		 * <p>
		 *
		 * @param shipment to be picked up
		 * @throws IllegalStateException if shipment is null or if shipment has already been picked up or if last element is not a leg.
		 */
		public void schedulePickup(CarrierShipment shipment) {
			Gbl.assertNotNull(shipment);
			logger.debug("Pickup to get scheduled: {}", shipment);
			boolean wasNew = openPickups.add(shipment);
			if (!wasNew) {
				throw new IllegalStateException("Trying to deliver something which was already picked up.");
			}
			assertLastElementIsLeg();
			Pickup pickup = createPickup(shipment);
			tourElements.add(pickup);
			previousElementIsActivity = true;
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
		 * @param shipment					the shipment to be delivered
		 * @throws IllegalStateException 	if shipment is null or if shipment has not been picked up yet or if last element is not a leg.
		 */
		public void scheduleDelivery(CarrierShipment shipment) {
			Gbl.assertNotNull(shipment);
			logger.debug("Delivery to get scheduled: {}", shipment);
			logger.debug("OpenPickups: {}", openPickups);
			boolean wasOpen = openPickups.remove(shipment);
			if (!wasOpen) {
				throw new IllegalStateException("Trying to deliver something which was not picked up.");
			}
			assertLastElementIsLeg();
			tourElements.add(createDelivery(shipment));
			previousElementIsActivity = true;
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
		 * @param startLinkId	start link id
		 * @param linkIds		list of link ids that form the route
		 * @param endLinkId		end link id
		 * @return 				NetworkRoute
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
	}

	public static abstract class TourActivity extends TourElement {
		// yy why does it make sense to not implement them at this level? kai, oct'19

		public abstract String getActivityType();
		public abstract Id<Link> getLocation();
		public abstract double getDuration();
		public abstract TimeWindow getTimeWindow();
		public abstract void setExpectedArrival(double arrivalTime);
		public abstract double getExpectedArrival();

		@Override public String toString() {
			return "";
		}
	}

	public static abstract class ShipmentBasedActivity extends TourActivity {
		public abstract CarrierShipment getShipment();
	}

	public static class Leg extends TourElement {

		private Route route;
		private double expTransportTime;
		private double departureTime;

		@Override public String toString() {
			return "leg=[ dpTime=" + departureTime + " | expTTime=" + expTransportTime + " | route=" + route + "]" ;
		}

		public Leg() {
		}

		private Leg(Leg leg) {
			this.expTransportTime = leg.getExpectedTransportTime();
			this.departureTime = leg.getExpectedDepartureTime();
			if ( leg.getRoute() == null ) {
				this.route = null ;
			} else{
				this.route = leg.getRoute().clone();
			}
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

		private final CarrierService service;
		private double arrTime;

		@Override public String toString() {
			return "serviceActivity=" + super.toString() + "[arrTime=" + arrTime + "][service=" + service + "]" ;
		}

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
			return CarrierConstants.SERVICE;
		}

		@Override
		public Id<Link> getLocation() {
			return service.getServiceLinkId();
		}

		@Override
		public double getDuration() {
			return service.getServiceDuration();
		}

		@Override
		public TimeWindow getTimeWindow() {
			return service.getServiceStaringTimeWindow();
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

		private final Id<Link> locationLinkId;
		private final TimeWindow timeWindow;

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

		private final Id<Link> locationLinkId;
		private final TimeWindow timeWindow;
		private double arrTime;

		public End(Id<Link> locationLinkId, TimeWindow timeWindow) {
			super();
			this.locationLinkId = locationLinkId;
			this.timeWindow = timeWindow;
		}

		private End(End end) {
			this.locationLinkId = end.getLocation();
			this.timeWindow = end.getTimeWindow();
			this.arrTime = end.getExpectedArrival();
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
			return CarrierConstants.PICKUP;
		}

		@Override
		public TimeWindow getTimeWindow() {
			return shipment.getPickupStartingTimeWindow();
		}

		@Override
		public Id<Link> getLocation() {
			return shipment.getPickupLinkId();
		}

		@Override
		public double getDuration() {
			return shipment.getPickupDuration();
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

	}

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
			return shipment.getDeliveryStartingTimeWindow();
		}

		@Override
		public String getActivityType() {
			return CarrierConstants.DELIVERY;
		}

		@Override
		public Id<Link> getLocation() {
			return shipment.getDeliveryLinkId();
		}

		@Override
		public double getDuration() {
			return shipment.getDeliveryDuration();
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

	}

	private final List<TourElement> tourElements;

	private final Start start;

	private final End end;

	private final Id<Tour> tourId;

	private Tour(Builder builder){
		tourId = builder.tourId;
		tourElements = builder.tourElements;
		start = builder.start;
		end = builder.end;
	}

	private Tour(Tour tour, Id<Tour> newTourId) {
		this.tourId = newTourId;
		this.start = (Start) tour.start.duplicate();
		this.end = (End) tour.end.duplicate();
		List<TourElement> elements = new ArrayList<>();
		for (TourElement element : tour.getTourElements()) {
			elements.add(element.duplicate());
		}
		this.tourElements = elements;
	}

	public Tour duplicate() {
		return new Tour(this, Id.create(this.tourId.toString(), Tour.class));
	}

	/*
	 * returns a copy of the tour, but with a new Tour Id.
	 */
	public Tour duplicateWithNewId(Id<Tour> newTourId) {
		return new Tour(this, newTourId);
	}

	public List<TourElement> getTourElements() {
		return Collections.unmodifiableList(tourElements);
	}

	public Id<Tour> getId(){
		return tourId;
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
		return "[ startLinkId="+getStartLinkId()+" ][ endLinkId="+getEndLinkId()+" ][ #tourElements=" + tourElements.size() + "]";
	}

}
