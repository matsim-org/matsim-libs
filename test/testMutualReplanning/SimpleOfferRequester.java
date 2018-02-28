package testMutualReplanning;

import java.util.ArrayList;
import java.util.Collection;

import demand.decoratedLSP.LSPWithOffers;
import demand.demandObject.DemandObject;
import demand.demandObject.OfferRequester;
import demand.offer.Offer;
import lsp.LogisticsSolution;

public class SimpleOfferRequester implements OfferRequester{

	private DemandObject demandObject;
	
	
	@Override
	public Collection<Offer> requestOffers(Collection<LSPWithOffers> lsps) {
		ArrayList<Offer> offerList = new ArrayList<Offer>();
		for(LSPWithOffers lsp : lsps) {
			for(LogisticsSolution solution : lsp.getSelectedPlan().getSolutions()) {
				Offer offer = lsp.getOffer(demandObject, "nonsense", solution.getId());
				offerList.add(offer);
			}
		}
		return offerList;
	}

	@Override
	public void setDemandObject(DemandObject demandObject) {
		this.demandObject = demandObject;
		
	}

}
