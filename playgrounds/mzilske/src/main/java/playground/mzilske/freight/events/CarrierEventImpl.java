package playground.mzilske.freight.events;

import org.matsim.api.core.v01.Id;

public abstract class CarrierEventImpl{
	
	private Id carrierId;

	public CarrierEventImpl(Id carrierId) {
		super();
		this.carrierId = carrierId;
	}

	public Id getCarrierId() {
		return carrierId;
	}

	

}
