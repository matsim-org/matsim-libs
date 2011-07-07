package playground.mzilske.freight;



public class TSPContract {
	private TSPShipment shipment;
	
	private TSPOffer offer;
	
	public TSPContract(TSPShipment shiment, TSPOffer offer) {
		super();
		this.shipment = shiment;
		this.offer = offer;
	}
	
	public TSPShipment getShipment() {
		return shipment;
	}
	
	public TSPOffer getOffer() {
		return offer;
	}
}
