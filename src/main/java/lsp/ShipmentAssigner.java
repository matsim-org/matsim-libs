package lsp;

import lsp.shipment.LSPShipment;

public interface ShipmentAssigner {

	public void assignShipment(LSPShipment shipment);
	public void setLSP(LSP lsp);
	public LSP getLSP();
}
