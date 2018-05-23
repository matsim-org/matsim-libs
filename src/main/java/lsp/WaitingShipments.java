package lsp;

import java.util.Collection;

import lsp.shipment.LSPShipment;

public interface WaitingShipments {

	public void addShipment(double time ,LSPShipment shipment);
	
	public Collection<ShipmentTuple> getSortedShipments();
	
	public Collection<ShipmentTuple> getShipments();
	
	public void clear();
	
}
