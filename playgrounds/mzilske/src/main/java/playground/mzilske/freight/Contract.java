package playground.mzilske.freight;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class Contract {
	
	private Collection<Shipment> shipments = new ArrayList<Shipment>();
	
	public Contract(Collection<Shipment> shipments) {
		super();
		this.shipments = shipments;
	}

	public Collection<Shipment> getShipments() {
		return Collections.unmodifiableCollection(shipments);
	}
	
}
