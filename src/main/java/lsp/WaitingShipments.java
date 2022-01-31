package lsp;

import java.util.Collection;

import lsp.shipment.LSPShipment;

public interface WaitingShipments {

	void addShipment(double time, LSPShipment shipment);
	
	Collection<ShipmentWithTime> getSortedShipments();
	
	Collection<ShipmentWithTime> getShipments();
	
	void clear();
	
}
