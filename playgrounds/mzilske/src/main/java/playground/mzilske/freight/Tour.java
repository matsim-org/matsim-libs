package playground.mzilske.freight;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.matsim.api.core.v01.Id;

import playground.mzilske.freight.Shipment.TimeWindow;

public class Tour {

	public static abstract class TourElement {
		
		public abstract String getActivityType();

		public abstract Id getLocation();

		public abstract double getDuration();
		
		public abstract Shipment getShipment();
		
		public abstract TimeWindow getTimeWindow();
		
	};
	
	public static class Pickup extends TourElement {

		private Shipment shipment;

		public Pickup(Shipment shipment) {
			this.shipment = shipment;
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
			return shipment.getSize()*60;
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
			return shipment.getSize()*60;
		}

		@Override
		public Shipment getShipment() {
			return shipment;
		}
		
	};
	
	public static class GeneralActivity extends TourElement {

		private String type;
		
		private Id location;
		
		private Double duration;
		
		public GeneralActivity(String type, Id location, Double duration) {
			super();
			this.type = type;
			this.location = location;
			if(duration != null){
				this.duration = duration;
			}
			else{
				this.duration = 0.0;
			}
		}

		@Override
		public String getActivityType() {
			return type;
		}

		@Override
		public Id getLocation() {
			return location;
		}

		@Override
		public double getDuration() {
			return this.duration;
		}

		@Override
		public Shipment getShipment() {
			return null;
		}

		@Override
		public TimeWindow getTimeWindow() {
			return new TimeWindow(0.0, 3600*24);
		}
		
	}

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
