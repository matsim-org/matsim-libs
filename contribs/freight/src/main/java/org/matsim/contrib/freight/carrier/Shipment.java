package org.matsim.contrib.freight.carrier;

import org.matsim.api.core.v01.Id;

import org.matsim.contrib.freight.api.TimeWindow;

public interface Shipment {

	public abstract Id getFrom();

	public abstract Id getTo();

	public abstract int getSize();

	public abstract TimeWindow getPickupTimeWindow();

	public abstract TimeWindow getDeliveryTimeWindow();

}