package playground.mzilske.freight;

import org.matsim.api.core.v01.Id;

import playground.mzilske.freight.api.Offer;
import playground.mzilske.freight.carrier.Shipment;

public interface Contract {
	
	public Id getBuyer();
	
	public Id getSeller();
	
	public Shipment getShipment();
	
	public Offer getOffer();

}
