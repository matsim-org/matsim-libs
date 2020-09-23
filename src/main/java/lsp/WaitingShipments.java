package lsp;

import java.util.Collection;

import lsp.shipment.LSPShipment;

public interface WaitingShipments {

	public void addShipment(double time ,LSPShipment shipment);
	
	public Collection<ShipmentWithTime> getSortedShipments();
	
	public Collection<ShipmentWithTime> getShipments();
	
	public void clear();
	
}
