package freight.offermaker;

import org.matsim.contrib.freight.carrier.CarrierOffer;

import java.util.Collection;

interface OfferSelector {

	public abstract CarrierOffer selectOffer(Collection<CarrierOffer> carrierOffers);

}