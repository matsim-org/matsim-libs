package playground.mzilske.freight;

import org.matsim.api.core.v01.Id;

public interface Shipment {

	public abstract Id getFrom();

	public abstract Id getTo();

	public abstract int getSize();

	public abstract TimeWindow getPickupTimeWindow();

	public abstract TimeWindow getDeliveryTimeWindow();

}