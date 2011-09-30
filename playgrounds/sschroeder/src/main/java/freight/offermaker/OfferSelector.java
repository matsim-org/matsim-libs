package freight.offermaker;

import java.util.Collection;

import playground.mzilske.freight.carrier.CarrierOffer;

interface OfferSelector {

	public abstract CarrierOffer selectOffer(Collection<CarrierOffer> carrierOffers);

}