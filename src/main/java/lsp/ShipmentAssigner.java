package lsp;

import lsp.shipment.LSPShipment;

/**
 * Takes a {@link LSPShipment} and normally assigns it to something that belongs to an {@link LSP}.
 *
 * Weist {@link LSPShipment}s den {@link LogisticsSolution}s zu.
 */
public interface ShipmentAssigner {

	void assignToSolution(LSPShipment shipment);
	void setLSP(LSP lsp);
//	public LSP getLSP();
}
