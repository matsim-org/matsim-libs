package freight.listener;

import org.matsim.api.core.v01.Id;

public class ShipperEventImpl {
	
	private Id shipperId;

	public ShipperEventImpl(Id shipperId) {
		super();
		this.shipperId = shipperId;
	}

	public Id getShipperId() {
		return shipperId;
	}
	
	

}
