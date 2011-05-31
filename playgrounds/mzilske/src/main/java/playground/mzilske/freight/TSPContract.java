package playground.mzilske.freight;

import java.util.Collection;
import java.util.Collections;


public class TSPContract {
	private TSPShipment shipment;
	
	private TSPOffer offer;

	private Collection<TSPShipment> shipments;
	
	public TSPContract(TSPShipment shiment, TSPOffer offer) {
		super();
		this.shipment = shiment;
		this.offer = offer;
		shipments.add(shiment);
	}
	
	public TSPContract(Collection<TSPShipment> shipments, TSPOffer offer) {
		super();
		this.offer = offer;
		this.shipments = shipments;
	}

	public TSPShipment getShipment() {
		return shipment;
	}
	
	public Collection<TSPShipment> getShipments(){
		return Collections.unmodifiableCollection(shipments);
	}

	public TSPOffer getOffer() {
		return offer;
	}
}
