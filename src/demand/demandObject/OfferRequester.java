package demand.demandObject;

import java.util.Collection;

import demand.decoratedLSP.LSPDecorator;
import demand.offer.Offer;

public interface OfferRequester {

	public Collection<Offer> requestOffers(Collection<LSPDecorator> lsps);
	public void setDemandObject(DemandObject demandObject);
}
