package demand.demandObject;

import java.util.Collection;

import demand.decoratedLSP.LSPWithOffers;
import demand.offer.Offer;

public interface OfferRequester {

	public Collection<Offer> requestOffers(Collection<LSPWithOffers> lsps);
	public void setDemandObject(DemandObject demandObject);
}
