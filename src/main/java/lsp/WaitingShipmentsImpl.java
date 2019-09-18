package lsp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;

import lsp.shipment.LSPShipment;
import lsp.shipment.ShipmentComparator;

/* package-private */ class WaitingShipmentsImpl implements WaitingShipments {

		
	private ArrayList<ShipmentTuple> shipments;
	
	WaitingShipmentsImpl() {
		this.shipments = new ArrayList<>();
	}
	
	
	@Override
	public void addShipment(double time, LSPShipment shipment) {
		ShipmentTuple tuple = new ShipmentTuple(time, shipment);
		this.shipments.add(tuple);
		Collections.sort(shipments, new ShipmentComparator());
	}

	@Override
	public Collection <ShipmentTuple> getSortedShipments() {
		Collections.sort(shipments, new ShipmentComparator());
		return shipments;
	}

	public void clear(){
		shipments.clear();
	}

	@Override
	public Collection<ShipmentTuple> getShipments() {
		return shipments;
	}
		
}
