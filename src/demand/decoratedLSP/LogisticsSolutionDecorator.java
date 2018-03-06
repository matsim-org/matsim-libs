package demand.decoratedLSP;

import demand.demandObject.DemandObject;
import demand.offer.Offer;
import demand.offer.OfferFactory;
import lsp.LogisticsSolution;

public interface LogisticsSolutionDecorator  extends LogisticsSolution {

	public Offer getOffer(DemandObject object, String type);
	public void setOfferFactory(OfferFactory factory);
	public OfferFactory getOfferFactory();
	public LSPDecorator getLSP();
	
}
