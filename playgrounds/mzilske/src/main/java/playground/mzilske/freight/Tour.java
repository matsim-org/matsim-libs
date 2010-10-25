package playground.mzilske.freight;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.matsim.api.core.v01.Id;

public class Tour {

	public static abstract class TourElement {
		
		public abstract String getActivityType();

		public abstract Id getLocation();

		public abstract double getDuration();
		
		public abstract Shipment getShipment();
		
	};
	
	public static class Pickup extends TourElement {

		private Shipment shipment;

		public Pickup(Shipment shipment) {
			this.shipment = shipment;
		}

		@Override
		public String getActivityType() {
			return "pickup";
		}

		@Override
		public Id getLocation() {
			return shipment.getFrom();
		}

		@Override
		public double getDuration() {
			return shipment.getSize();
		}

		@Override
		public Shipment getShipment() {
			return shipment;
		}
	};
	
	public static class Delivery extends TourElement {

		private Shipment shipment;

		public Delivery(Shipment shipment) {
			this.shipment = shipment;
		}

		@Override
		public String getActivityType() {
			return "delivery";
		}

		@Override
		public Id getLocation() {
			return shipment.getTo();
		}

		@Override
		public double getDuration() {
			return shipment.getSize();
		}

		@Override
		public Shipment getShipment() {
			return shipment;
		}
		
	};

	private List<TourElement> tourElements;
	private Id startLinkId;
	private Id endLinkId;

	Tour(Id startLinkId, List<TourElement> tourElements, Id endLinkId) {
		this.startLinkId = startLinkId;
		this.tourElements = Collections.unmodifiableList(tourElements);
		this.endLinkId = endLinkId;
	}

	public List<TourElement> getTourElements() {
		return Collections.unmodifiableList(tourElements);
	}

	public List<Shipment> getShipments() {
		List<Shipment> shipments = new ArrayList<Shipment>();
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

}
