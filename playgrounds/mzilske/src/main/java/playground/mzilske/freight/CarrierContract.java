package playground.mzilske.freight;

import org.matsim.api.core.v01.Id;



public class CarrierContract implements Contract{
	
	private Id carrierId;
	
	private Id tspId;
	
	private CarrierShipment shipment;
	
	private CarrierOffer offer;
	
	public CarrierContract(CarrierShipment shipment, CarrierOffer offer) {
		super();
		this.shipment = shipment;
		this.offer = offer;
	}

	public CarrierContract(Id tspId,Id carrierId, CarrierShipment shipment,CarrierOffer offer) {
		super();
		this.carrierId = carrierId;
		this.tspId = tspId;
		this.shipment = shipment;
		this.offer = offer;
	}

	public CarrierShipment getShipment() {
		return shipment;
	}
	
	public CarrierOffer getOffer() {
		return offer;
	}

	@Override
	public Id getBuyer() {
		return tspId;
	}

	@Override
	public Id getSeller() {
		return carrierId;
	}
	
}
