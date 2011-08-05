package freight.offermaker;

import java.util.Collection;

import playground.mzilske.freight.CarrierOffer;

interface OfferSelector {

	public abstract CarrierOffer selectOffer(Collection<CarrierOffer> carrierOffers);

}