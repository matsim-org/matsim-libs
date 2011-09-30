package playground.mzilske.freight.carrier;

import org.matsim.api.core.v01.Id;

import playground.mzilske.freight.TimeWindow;

public interface Shipment {

	public abstract Id getFrom();

	public abstract Id getTo();

	public abstract int getSize();

	public abstract TimeWindow getPickupTimeWindow();

	public abstract TimeWindow getDeliveryTimeWindow();

}