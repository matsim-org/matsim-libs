package playground.mzilske.freight;

import org.matsim.api.core.v01.Id;

public interface OfferMaker {

	public abstract Offer requestOffer(Id linkId, Id linkId2, int shipmentSize, Double memorizedPrice);

	public abstract Offer requestOffer(Id from, Id to, int shimpentSize, Double startPickup, Double endPickup, Double startDelivery, Double endDelivery, Double memorizedPrice);
}