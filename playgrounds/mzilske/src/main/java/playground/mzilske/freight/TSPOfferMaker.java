package playground.mzilske.freight;

import org.matsim.api.core.v01.Id;

public interface TSPOfferMaker {
	
	public TSPOffer getOffer(Id from, Id to, int size, double memorizedPrice);
	
	public void setTSP(TransportServiceProviderImpl tsp);
}
