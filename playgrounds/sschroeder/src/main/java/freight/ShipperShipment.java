package freight;

import org.matsim.api.core.v01.Id;

import playground.mzilske.freight.TimeWindow;

public class ShipperShipment implements playground.mzilske.freight.Shipment{
	
	
	private Id from;
	
	private Id to;
	
	private int size;
	
	private TimeWindow pickTimeWindow;
	
	private TimeWindow deliveryTimeWindow;

	public ShipperShipment(Id from, Id to, int size, TimeWindow pickTimeWindow,
			TimeWindow deliveryTimeWindow) {
		super();
		this.from = from;
		this.to = to;
		this.size = size;
		this.pickTimeWindow = pickTimeWindow;
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
		return pickTimeWindow;
	}

	public TimeWindow getDeliveryTimeWindow() {
		return deliveryTimeWindow;
	}
	
	@Override
	public String toString() {
		return "[from=" + from + "][to=" + to + "][size=" + size + "][pickupTimeWindow=" + pickTimeWindow + "][deliveryTimeWindow=" + deliveryTimeWindow + "]";
	}



}
