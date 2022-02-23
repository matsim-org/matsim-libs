package lsp;


/**
 * Serve the purpose of routing a set of {@link lsp.shipment.LSPShipment}s through a set of
 * {@link LogisticsSolution}s, which, in turn, consist of several {@link LogisticsSolutionElement}s
 * and the corresponding {@link lsp.resources.LSPResource}s.
 */
public interface SolutionScheduler {

	void scheduleSolutions();

	void setLSP(LSP lsp);
	
	void setBufferTime(int bufferTime);
}
