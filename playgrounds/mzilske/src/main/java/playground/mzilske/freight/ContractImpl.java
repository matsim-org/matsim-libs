package playground.mzilske.freight;

import org.matsim.api.core.v01.Id;

import playground.mzilske.freight.api.Offer;
import playground.mzilske.freight.carrier.Shipment;

public class ContractImpl implements Contract{

	private Id buyer;
	
	private Id seller;
	
	private Shipment shipment;
	
	private Offer offer;
	
	public ContractImpl(Id buyer, Id seller, Shipment shipment, Offer offer) {
		super();
		this.buyer = buyer;
		this.seller = seller;
		this.shipment = shipment;
		this.offer = offer;
	}

	@Override
	public Id getBuyer() {
		return buyer;
	}

	@Override
	public Id getSeller() {
		return seller;
	}

	@Override
	public Shipment getShipment() {
		return shipment;
	}

	@Override
	public Offer getOffer() {
		return offer;
	}

}
