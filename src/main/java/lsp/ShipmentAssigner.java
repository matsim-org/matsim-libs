package lsp;

import lsp.shipment.LSPShipment;

/**
 * Takes a {@link LSPShipment} and normally assigns it to something that belongs to an {@link LSP}.
 *
 * If there are several {@link LogisticsSolution}s, the {@link LSP} has to assign each {@link LSPShipment} to
 * the suitable one. For this purpose, each LSPPlan contains a pluggable strategy that
 * is contained in classes implementing the interface ShipmentAssigner.
 *
 * Weist {@link LSPShipment}s den {@link LogisticsSolution}s zu.
 */
public interface ShipmentAssigner {

	void assignToSolution(LSPShipment shipment);
	void setLSP(LSP lsp);
}
