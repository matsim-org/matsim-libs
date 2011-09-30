package playground.mzilske.freight;

import org.matsim.api.core.v01.Id;

import playground.mzilske.freight.api.Offer;

public interface Contract {
	
	public Id getBuyer();
	
	public Id getSeller();
	
	public Shipment getShipment();
	
	public Offer getOffer();

}
