package playground.mzilske.freight;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.matsim.api.core.v01.Id;

import playground.mzilske.freight.Tour.Delivery;
import playground.mzilske.freight.Tour.Pickup;
import playground.mzilske.freight.Tour.TourElement;

public class TourBuilder {

	private List<TourElement> tourElements = new ArrayList<TourElement>();
	
	private Set<Shipment> openPickups = new HashSet<Shipment>();

	private Id startLinkId;

	private Id endLinkId;
	
	public void scheduleStart(Id startLinkId) {
		this.startLinkId = startLinkId;
	}
	
	public void scheduleEnd(Id endLinkId) {
		this.endLinkId = endLinkId;
	}

	public void schedulePickup(Shipment shipment) {
		boolean wasNew = openPickups.add(shipment);
		if (!wasNew) {
			throw new RuntimeException("Trying to deliver something which was already picked up.");
		}
		tourElements.add(createPickup(shipment));
	}

	public void scheduleDelivery(Shipment shipment) {
		boolean wasOpen = openPickups.remove(shipment);
		if (!wasOpen) {
			throw new RuntimeException("Trying to deliver something which was not picked up.");
		}
		tourElements.add(createDelivery(shipment));
	}

	public Tour build() {
		return new Tour(startLinkId, tourElements, endLinkId);
	}

	private Pickup createPickup(Shipment shipment) {
		return new Pickup(shipment);
	}

	private Delivery createDelivery(Shipment shipment) {
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

}
