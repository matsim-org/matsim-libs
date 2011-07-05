package city2000w;

import java.util.Collection;

import org.matsim.api.core.v01.Id;

import playground.mzilske.freight.CarrierAgentTracker;
import playground.mzilske.freight.Offer;
import playground.mzilske.freight.TSPOffer;
import playground.mzilske.freight.TSPOfferMaker;
import playground.mzilske.freight.TSPShipment;
import playground.mzilske.freight.TransportServiceProviderImpl;

public class CarrierCostRequester implements TSPOfferMaker{

	private TransportServiceProviderImpl tsp;
	
	private CarrierAgentTracker carrierAgentTracker;
	
	public CarrierCostRequester(CarrierAgentTracker carrierAgentTracker) {
		super();
		this.carrierAgentTracker = carrierAgentTracker;
	}

	@Override
	public void setTSP(TransportServiceProviderImpl tsp) {
		this.tsp = tsp;
	}

	public TSPOffer getOffer(TransportServiceProviderImpl tsp, Collection<TSPShipment> shipments) {
		TSPShipment shipment = shipments.iterator().next();
		Collection<Offer> offers = carrierAgentTracker.getOffers(shipment.getFrom(), shipment.getTo(), shipment.getSize());
		Offer bestOffer = pickBest(offers);
		double priceForTspShipments = bestOffer.getPrice() * shipments.size();
		TSPOffer tspOffer = new TSPOffer();
		tspOffer.setPrice(priceForTspShipments);
		tspOffer.setTspId(tsp.getId());
		return tspOffer;
	}

	private Offer pickBest(Collection<Offer> offers) {
		Offer bestOffer = null;
		for(Offer o : offers){
			if(bestOffer == null){
				bestOffer = o;
			}
			else{
				if(o.getPrice() < bestOffer.getPrice()){
					bestOffer = o;
				}
			}
		}
		return bestOffer;
	}

	@Override
	public TSPOffer getOffer(Id from, Id to, int size, double memorizedPrice) {
		Collection<Offer> offers = carrierAgentTracker.getOffers(from, to, size);
		Offer bestOffer = pickBest(offers);
		TSPOffer tspOffer = new TSPOffer();
		tspOffer.setPrice(bestOffer.getPrice());
		tspOffer.setTspId(tsp.getId());
		return tspOffer;
	}

}
