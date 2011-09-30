package playground.mzilske.freight.carrier;

import org.matsim.api.core.v01.Id;

import playground.mzilske.freight.TimeWindow;



public class CarrierShipment implements Shipment {
	
	private Id from;
	
	private Id to;
	
	private int size;
	
	private TimeWindow pickupTimeWindow;
	
	private TimeWindow deliveryTimeWindow;

	public CarrierShipment(Id from, Id to, int size, TimeWindow pickupTimeWindow, TimeWindow deliveryTimeWindow) {
		super();
		this.from = from;
		this.to = to;
		this.size = size;
		this.pickupTimeWindow = pickupTimeWindow;
		this.deliveryTimeWindow = deliveryTimeWindow;
	}

	/* (non-Javadoc)
	 * @see playground.mzilske.freight.Shipment#getFrom()
	 */
	@Override
	public Id getFrom() {
		return from;
	}

	/* (non-Javadoc)
	 * @see playground.mzilske.freight.Shipment#getTo()
	 */
	@Override
	public Id getTo() {
		return to;
	}

	/* (non-Javadoc)
	 * @see playground.mzilske.freight.Shipment#getSize()
	 */
	@Override
	public int getSize() {
		return size;
	}

	/* (non-Javadoc)
	 * @see playground.mzilske.freight.Shipment#getPickupTimeWindow()
	 */
	@Override
	public TimeWindow getPickupTimeWindow() {
		return pickupTimeWindow;
	}

	/* (non-Javadoc)
	 * @see playground.mzilske.freight.Shipment#getDeliveryTimeWindow()
	 */
	@Override
	public TimeWindow getDeliveryTimeWindow() {
		return deliveryTimeWindow;
	}

	@Override
	public String toString() {
		return "[from="+from.toString()+"][to="+ to.toString() + "][size=" + size +"]";
	}
	
}
