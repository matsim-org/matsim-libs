package playground.mzilske.freight.carrier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.matsim.api.core.v01.Id;

import playground.mzilske.freight.TimeWindow;


public class Tour {

	public static abstract class TourElement {
		
		public abstract String getActivityType();

		public abstract Id getLocation();

		public abstract double getDuration();
		
		@Deprecated
		public abstract CarrierShipment getShipment();
		
		public abstract TimeWindow getTimeWindow();
		
	};
	
	public static abstract class ShipmentBasedActivity extends TourElement {
		public abstract CarrierShipment getShipment();
	}
	
	public static class Pickup extends ShipmentBasedActivity {

		private CarrierShipment shipment;

		public Pickup(CarrierShipment shipment) {
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
		public CarrierShipment getShipment() {
			return shipment;
		}
	};
	
	public static class Delivery extends ShipmentBasedActivity {

		private CarrierShipment shipment;

		public Delivery(CarrierShipment shipment) {
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
		public CarrierShipment getShipment() {
			return shipment;
		}
		
	};
	
	public static class GeneralActivity extends TourElement {

		private String type;
		
		private Id location;
		
		private Double duration;
		
		private Double earliestStart;
		
		private Double latestStart;
		
		public GeneralActivity(String type, Id location, Double earliestStart, Double latestStart, Double duration) {
			super();
			this.type = type;
			this.location = location;
			this.duration = duration;
			this.earliestStart = earliestStart;
			this.latestStart = latestStart;
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
		public CarrierShipment getShipment() {
			return null;
		}

		@Override
		public TimeWindow getTimeWindow() {
			return new TimeWindow(earliestStart, latestStart);
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

}
