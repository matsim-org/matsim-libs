package org.matsim.contrib.freight.api;

import org.matsim.api.core.v01.Id;

import org.matsim.contrib.freight.carrier.Shipment;

public interface Contract {
	
	public Id getBuyer();
	
	public Id getSeller();
	
	public Shipment getShipment();
	
	public Offer getOffer();

}
