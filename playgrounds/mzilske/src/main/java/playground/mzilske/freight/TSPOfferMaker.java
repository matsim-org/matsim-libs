package playground.mzilske.freight;

import org.matsim.api.core.v01.Id;

public interface TSPOfferMaker {
	
	public TSPOffer requestOffer(Id from, Id to, int size, Double startPickup, Double endPickup, Double startDelivery, Double endDelivery, Double memorizedPrice);
	
}
