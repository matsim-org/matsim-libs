package playground.mzilske.freight.events;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.Event;

import playground.mzilske.freight.Contract;

public class CarrierOfferAcceptEvent implements Event{
	
	private Id buyer;
	
	private Id seller;
	
	private Service service;
	
	private Contract contract;
		
	public CarrierOfferAcceptEvent(Id buyer, Id seller, Service service) {
		super();
		this.buyer = buyer;
		this.seller = seller;
		this.service = service;
	}

	public CarrierOfferAcceptEvent(Contract contract) {
		super();
		this.contract = contract;
	}

	public Contract getContract() {
		return contract;
	}

	public Id getBuyer() {
		return buyer;
	}

	public Id getSeller() {
		return seller;
	}

	public Service getService() {
		return service;
	}

	@Override
	public double getTime() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Map<String, String> getAttributes() {
		// TODO Auto-generated method stub
		return null;
	}

}
