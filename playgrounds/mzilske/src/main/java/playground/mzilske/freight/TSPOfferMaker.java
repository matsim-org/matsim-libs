package playground.mzilske.freight;

import java.util.Collection;

public interface TSPOfferMaker {
	public TSPOffer makeOffer(TransportServiceProviderImpl agent, Collection<TSPShipment> shipments);
}
