package lsp;

import lsp.shipment.LSPShipment;

/**
 * Takes a {@code LSPShipment} and normally assigns it to something that belongs to an {@code LSP}.
 */
public interface ShipmentAssigner {

	void assignShipment(LSPShipment shipment);
	void setLSP(LSP lsp);
//	public LSP getLSP();
}
