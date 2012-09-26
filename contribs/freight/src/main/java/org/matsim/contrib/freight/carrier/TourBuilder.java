package org.matsim.contrib.freight.carrier;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.freight.carrier.Tour.Delivery;
import org.matsim.contrib.freight.carrier.Tour.Leg;
import org.matsim.contrib.freight.carrier.Tour.Pickup;
import org.matsim.contrib.freight.carrier.Tour.TourElement;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;

public class TourBuilder {

	private List<TourElement> tourElements = new ArrayList<TourElement>();

	private Set<CarrierShipment> openPickups = new HashSet<CarrierShipment>();

	private Id startLinkId;

	private Id endLinkId;

	private double earliestDeparture;

	private double latestDeparture;

	private boolean previousElementIsActivity;

	public void scheduleStart(Id startLinkId, double earliestDeparture,
			double latestDeparture) {
		this.startLinkId = startLinkId;
		this.earliestDeparture = earliestDeparture;
		this.latestDeparture = latestDeparture;
		previousElementIsActivity = true;
	}

	public void scheduleEnd(Id endLinkId) {
		assertLastElementIsLeg();
		this.endLinkId = endLinkId;
	}

	public void addLeg(Leg leg) {
		assertIsNotNull(leg);
		if (!previousElementIsActivity) {
			throw new RuntimeException(
					"cannot add leg, since last tour element is not an activity.");
		}
		tourElements.add(leg);
		previousElementIsActivity = false;
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

	public void schedulePickup(CarrierShipment shipment) {
		assertIsNotNull(shipment);
		boolean wasNew = openPickups.add(shipment);
		if (!wasNew) {
			throw new RuntimeException(
					"Trying to deliver something which was already picked up.");
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

	public void scheduleDelivery(CarrierShipment shipment) {
		assertIsNotNull(shipment);
		boolean wasOpen = openPickups.remove(shipment);
		if (!wasOpen) {
			throw new RuntimeException(
					"Trying to deliver something which was not picked up.");
		}
		assertLastElementIsLeg();
		tourElements.add(createDelivery(shipment));
		previousElementIsActivity = true;
	}

	public Tour build() {
		Tour tour = new Tour(startLinkId, tourElements, endLinkId);
		tour.setEarliestDeparture(earliestDeparture);
		tour.setLatestDeparture(latestDeparture);
		return tour;
	}

	private Pickup createPickup(CarrierShipment shipment) {
		return new Pickup(shipment);
	}

	private Delivery createDelivery(CarrierShipment shipment) {
		return new Delivery(shipment);
	}

	public Leg createLeg() {
		return new Leg();
	}

	public NetworkRoute createRoute(Id startLinkId, List<Id> linkIds,
			Id endLinkId) {
		LinkNetworkRouteImpl linkNetworkRouteImpl = new LinkNetworkRouteImpl(
				startLinkId, endLinkId);
		if (linkIds != null && !linkIds.isEmpty()) {
			linkNetworkRouteImpl.setLinkIds(startLinkId, linkIds, endLinkId);
		}
		return linkNetworkRouteImpl;

	}

}
