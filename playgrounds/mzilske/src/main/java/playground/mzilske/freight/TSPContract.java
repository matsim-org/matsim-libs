package playground.mzilske.freight;

import org.matsim.api.core.v01.Id;



public class TSPContract implements Contract{
	
	private Id shipperId;
	
	private Id tspId;
	
	private TSPShipment shipment;
	
	private TSPOffer offer;
	
	public TSPContract(TSPShipment shiment, TSPOffer offer) {
		super();
		this.shipment = shiment;
		this.offer = offer;
	}
	
	public TSPContract(Id shipperId, Id tspId, TSPShipment shipment, TSPOffer offer){
		this.shipperId = shipperId;
		this.tspId = tspId;
		this.shipment = shipment;
		this.offer = offer;
	}
	
	public TSPShipment getShipment() {
		return shipment;
	}
	
	public TSPOffer getOffer() {
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
