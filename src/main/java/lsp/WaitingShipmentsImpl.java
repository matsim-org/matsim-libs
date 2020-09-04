package lsp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import lsp.shipment.LSPShipment;
import lsp.shipment.ShipmentComparator;

/* package-private */ class WaitingShipmentsImpl implements WaitingShipments {

		
	private ArrayList<ShipmentWithTime> shipments;
	
	WaitingShipmentsImpl() {
		this.shipments = new ArrayList<>();
	}
	
	
	@Override
	public void addShipment(double time, LSPShipment shipment) {
		ShipmentWithTime tuple = new ShipmentWithTime(time, shipment);
		this.shipments.add(tuple);
		Collections.sort(shipments, new ShipmentComparator());
	}

	@Override
	public Collection <ShipmentWithTime> getSortedShipments() {
		Collections.sort(shipments, new ShipmentComparator());
		return shipments;
	}

	public void clear(){
		shipments.clear();
	}

	@Override
	public Collection<ShipmentWithTime> getShipments() {
		return shipments;
	}
		
}
