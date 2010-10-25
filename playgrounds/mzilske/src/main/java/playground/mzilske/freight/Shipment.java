package playground.mzilske.freight;

import org.matsim.api.core.v01.Id;



public class Shipment {
	
	public static class TimeWindow {
		
		private double start;
		
		private double end;
		
		public TimeWindow(double start, double end) {
			this.start = start;
			this.end = end;
		}

		double getStart() {
			return start;
		}

		double getEnd() {
			return end;
		}
		
	}

	private Id from;
	
	private Id to;
	
	private int size;
	
	private TimeWindow pickupTimeWindow;
	
	private TimeWindow deliveryTimeWindow;

	public Shipment(Id from, Id to, int size, TimeWindow pickupTimeWindow, TimeWindow deliveryTimeWindow) {
		super();
		this.from = from;
		this.to = to;
		this.size = size;
		this.pickupTimeWindow = pickupTimeWindow;
		this.deliveryTimeWindow = deliveryTimeWindow;
	}

	Id getFrom() {
		return from;
	}

	Id getTo() {
		return to;
	}

	int getSize() {
		return size;
	}

	TimeWindow getPickupTimeWindow() {
		return pickupTimeWindow;
	}

	TimeWindow getDeliveryTimeWindow() {
		return deliveryTimeWindow;
	}

	@Override
	public String toString() {
		return from.toString() + " -> " + to.toString() + ": " + size;
	}
	
}
