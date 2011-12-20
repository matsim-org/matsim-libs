package org.matsim.contrib.freight.carrier;

import org.matsim.api.core.v01.Id;

public interface Contract {
	
	public Id getBuyer();
	
	public Id getSeller();
	
	public Shipment getShipment();
	
	public Offer getOffer();

}
