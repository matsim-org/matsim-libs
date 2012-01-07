package org.matsim.contrib.freight.carrier;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.freight.carrier.Tour.Delivery;
import org.matsim.contrib.freight.carrier.Tour.GeneralActivity;
import org.matsim.contrib.freight.carrier.Tour.Pickup;
import org.matsim.contrib.freight.carrier.Tour.TourElement;


public class TourBuilder {

	private List<TourElement> tourElements = new ArrayList<TourElement>();
	
	private Set<CarrierShipment> openPickups = new HashSet<CarrierShipment>();

	private Id startLinkId;

	private Id endLinkId;
	
	private double earliestDeparture;
	
	private double latestDeparture;
	
	private boolean departureSet = false;
	
	public void scheduleStart(Id startLinkId) {
		this.startLinkId = startLinkId;
	}
	
	public void scheduleEnd(Id endLinkId) {
		this.endLinkId = endLinkId;
	}

	public void schedulePickup(CarrierShipment shipment) {
		boolean wasNew = openPickups.add(shipment);
		if (!wasNew) {
			throw new RuntimeException("Trying to deliver something which was already picked up.");
		}
		tourElements.add(createPickup(shipment));
	}

	public void scheduleDelivery(CarrierShipment shipment) {
		boolean wasOpen = openPickups.remove(shipment);
		if (!wasOpen) {
			throw new RuntimeException("Trying to deliver something which was not picked up.");
		}
		tourElements.add(createDelivery(shipment));
	}

	public Tour build() {
		Tour tour = new Tour(startLinkId, tourElements, endLinkId);
		if(departureSet){
			tour.setEarliestDeparture(earliestDeparture);
			tour.setLatestDeparture(latestDeparture);
		}
		return tour;
	}

	private Pickup createPickup(CarrierShipment shipment) {
		return new Pickup(shipment);
	}

	private Delivery createDelivery(CarrierShipment shipment) {
		return new Delivery(shipment);
	}

	public void schedule(TourElement tourElement) {
		if (tourElement instanceof Pickup) {
			schedulePickup(tourElement.getShipment());
		} else if (tourElement instanceof Delivery) {
			scheduleDelivery(tourElement.getShipment());
		} else {
			throw new RuntimeException("Cannot happen.");
		}
	}
	
	public void scheduleGeneralActivity(String type, Id locationLink, Double earliestStart, Double latestStart, Double duration){
		tourElements.add(createGeneralActivity(type,locationLink,earliestStart,latestStart,duration));
	}

	private TourElement createGeneralActivity(String type, Id locationLink,Double earliestStart, Double latestStart, Double duration) {
		GeneralActivity act = new GeneralActivity(type, locationLink, earliestStart, latestStart, duration);
		return act;
	}

	public void setTourStartTimeWindow(double earliestDeparture, double latestDeparture) {
		this.earliestDeparture = earliestDeparture;
		this.latestDeparture = latestDeparture;
		departureSet = true;
	}

}
