package playground.mzilske.freight;

import org.matsim.api.core.v01.Id;

public class TSPShipment {
	
	private Id from;
	
	private Id to;
	
	private int size;
	
	private TimeWindow pickUpTimeWindow;
	
	private TimeWindow deliveryTimeWindow;
	
	public TSPShipment(Id from, Id to, int size, TimeWindow pickUpTimeWindow,
			TimeWindow deliveryTimeWindow) {
		super();
		this.from = from;
		this.to = to;
		this.size = size;
		this.pickUpTimeWindow = pickUpTimeWindow;
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

	public TimeWindow getPickUpTimeWindow() {
		return pickUpTimeWindow;
	}

	public TimeWindow getDeliveryTimeWindow() {
		return deliveryTimeWindow;
	}
	
	public String toString(){
		return "shipment([from="+from+"][to="+to+"][size="+size+"])";
	}

	public static class TimeWindow{
		private double start;
		
		private double end;
		
		public TimeWindow(double start, double end){
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
}
