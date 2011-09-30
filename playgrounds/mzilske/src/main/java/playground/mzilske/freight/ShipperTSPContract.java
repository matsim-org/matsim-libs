package playground.mzilske.freight;

import org.matsim.api.core.v01.Id;

import playground.mzilske.freight.api.Offer;
import playground.mzilske.freight.carrier.Shipment;

public class ShipperTSPContract implements Contract{

	private Id shipperId;
	
	private Id tspId;
	
	private TSPShipment shipment;
	
	private TSPOffer offer;
	
	public ShipperTSPContract(Id shipperId, Id tspId, TSPShipment shipment,
			TSPOffer offer) {
		super();
		this.shipperId = shipperId;
		this.tspId = tspId;
		this.shipment = shipment;
		this.offer = offer;
	}

	@Override
	public Shipment getShipment() {
		return shipment;
	}

	@Override
	public Offer getOffer() {
		return offer;
	}

	@Override
	public Id getBuyer() {
		return shipperId;
	}

	@Override
	public Id getSeller() {
		return tspId;
	}

}
