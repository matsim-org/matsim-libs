package demand.offer;

import java.util.ArrayList;
import java.util.Collection;

import demand.decoratedLSP.LSPWithOffers;
import demand.demandObject.DemandObject;
import demand.demandObject.OfferRequester;
import lsp.LogisticsSolution;

public class RequirementsOfferRequester implements OfferRequester {

	private DemandObject demandObject;
	
	@Override
	public Collection<Offer> requestOffers(Collection<LSPWithOffers> lsps) {
		ArrayList<Offer> offerList = new ArrayList<Offer>();
		for(LSPWithOffers lsp : lsps) {
			for(LogisticsSolution solution : lsp.getSelectedPlan().getSolutions() ) {
				offerList.add(lsp.getOffer(demandObject, "linear", solution.getId()));
			}
		}
		return offerList;
	}

	public void setDemandObject(DemandObject demandObject) {
		this.demandObject = demandObject;
	}
	
}
