package lsp.usecase;

import lsp.LSP;
import lsp.LogisticsSolution;
import lsp.ShipmentAssigner;
import lsp.shipment.LSPShipment;
import org.matsim.core.gbl.Gbl;

/**
 * Ganz einfacher {@link ShipmentAssigner}:
 * Voraussetzung: Der {@link lsp.LSPPlan} hat genau 1 {@link LogisticsSolution}.
 *
 * Dann wird das {@link  LSPShipment} diesem zugeordnet.
 *
 * (Falls die Voraussetzung "exakt 1 Solution pro Plan" nicht erfüllt ist, kommt eine RuntimeException)
 */
class DeterministicShipmentAssigner implements ShipmentAssigner {

	private LSP lsp;

	DeterministicShipmentAssigner() {
	}

	public void setLSP(LSP lsp) {
		this.lsp = lsp;
	}
	
	@Override
	public void assignToSolution(LSPShipment shipment) {
		Gbl.assertIf( lsp.getSelectedPlan().getSolutions().size()==1);
		LogisticsSolution singleSolution = lsp.getSelectedPlan().getSolutions().iterator().next();
		singleSolution.assignShipment(shipment);
	}

//	@Override
//	public LSP getLSP() {
//		return lsp;
//	}

}
