package org.matsim.contrib.freight.carrier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Route;
import org.matsim.contrib.freight.carrier.CarrierShipment.TimeWindow;
import org.matsim.core.population.routes.NetworkRoute;

public class Tour {

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

		public Leg(Leg leg) {
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

		Pickup(Pickup pickup) {
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

	Tour(final Id startLinkId, final List<TourElement> tourElements,
			final Id endLinkId) {
		this.startLinkId = startLinkId;
		this.tourElements = Collections.unmodifiableList(tourElements);
		this.endLinkId = endLinkId;
		this.earliestDeparture = 0.0;
		this.latestDeparture = 0.0;
	}

	Tour(Tour tour) {
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

}
