package testMutualReplanning;

import java.util.ArrayList;
import java.util.Collection;

import demand.decoratedLSP.LSPDecorator;
import demand.decoratedLSP.LSPWithOffers;
import demand.decoratedLSP.LogisticsSolutionDecorator;
import demand.demandObject.DemandObject;
import demand.demandObject.OfferRequester;
import demand.offer.Offer;

public class AllOffersRequester implements OfferRequester{

	private DemandObject demandObject;
	
	public AllOffersRequester() {
		
	}
	
	@Override
	public Collection<Offer> requestOffers(Collection<LSPDecorator> lsps) {
		ArrayList<Offer> offers = new ArrayList<Offer>();
		for(LSPDecorator lsp : lsps) {
			for(LogisticsSolutionDecorator solution : lsp.getSelectedPlan().getSolutionDecorators()) {
				offers.add(lsp.getOffer(demandObject, "linear", solution.getId()));
			}
		}
		return offers;
	}

	@Override
	public void setDemandObject(DemandObject demandObject) {
		this.demandObject = demandObject;
	}

}
