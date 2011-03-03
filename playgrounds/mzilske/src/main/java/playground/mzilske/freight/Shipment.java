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

		public double getStart() {
			return start;
		}

		public double getEnd() {
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

	public Id getFrom() {
		return from;
	}

	public Id getTo() {
		return to;
	}

	public int getSize() {
		return size;
	}

	public TimeWindow getPickupTimeWindow() {
		return pickupTimeWindow;
	}

	public TimeWindow getDeliveryTimeWindow() {
		return deliveryTimeWindow;
	}

	@Override
	public String toString() {
		return from.toString() + " -> " + to.toString() + ": " + size;
	}
	
}
