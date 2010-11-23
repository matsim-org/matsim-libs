package playground.mzilske.freight;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class TSPContract {
	private Collection<TSPShipment> shipments = new ArrayList<TSPShipment>();

	public TSPContract(Collection<TSPShipment> shipments) {
		super();
		this.shipments = shipments;
	}

	public Collection<TSPShipment> getShipments() {
		return Collections.unmodifiableCollection(shipments);
	}
}
