package testMutualReplanning;

import org.matsim.api.core.v01.Id;

import demand.decoratedLSP.LSPDecorator;
import demand.demandObject.DemandObject;
import demand.offer.Offer;
import demand.offer.OfferTransferrer;
import lsp.LogisticsSolution;

public class SimpleOfferTransferrer implements OfferTransferrer{

	private LSPDecorator lsp;
	
	@Override
	public Offer transferOffer(DemandObject object, String type, Id<LogisticsSolution> solutionId) {
		return lsp.getSelectedPlan().getSolutionDecorators().iterator().next().getOffer(object, type);
	}

	@Override
	public void setLSP(LSPDecorator lsp) {
		this.lsp = lsp;
	}

	@Override
	public LSPDecorator getLSP() {
		return lsp;
	}

}
