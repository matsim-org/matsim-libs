package playground.mzilske.city2000w;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;

import playground.mzilske.freight.CarrierAgentTracker;
import playground.mzilske.freight.CarrierOffer;
import playground.mzilske.freight.TSPPlan;
import playground.mzilske.freight.TransportChain;
import playground.mzilske.freight.TransportChain.ChainActivity;
import playground.mzilske.freight.TransportChain.ChainLeg;
import playground.mzilske.freight.TransportChain.ChainTriple;
import playground.mzilske.freight.TransportChainBuilder;
import playground.mzilske.freight.TransportServiceProviderImpl;

public class CheapestCarrierChoice {
	
	private Logger logger = Logger.getLogger(CheapestCarrierChoice.class);
	
	private TransportServiceProviderImpl tsp;
	
	private CarrierAgentTracker carrierAgentTracker;

	public CheapestCarrierChoice(TransportServiceProviderImpl tsp) {
		super();
		this.tsp = tsp;
	}

	public void setCarrierAgentTracker(CarrierAgentTracker carrierAgentTracker) {
		this.carrierAgentTracker = carrierAgentTracker;
	}

	public void run() {
		TSPPlan oldPlan = tsp.getSelectedPlan();
		List<TransportChain> transportChains = new ArrayList<TransportChain>();
		for(TransportChain chain : oldPlan.getChains()){
			TransportChainBuilder chainBuilder = new TransportChainBuilder(chain.getShipment());
			for(ChainTriple chainTriple : chain.getChainTriples()){
				ChainActivity firstActivity = chainTriple.getFirstActivity();
				ChainActivity secondActivity = chainTriple.getSecondActivity();
				CarrierOffer acceptedOffer = pickOffer(firstActivity.getLocation(), secondActivity.getLocation(), chain.getShipment().getSize());
				chainBuilder.schedule(firstActivity);
				chainBuilder.schedule(new ChainLeg(acceptedOffer));
				chainBuilder.schedule(secondActivity);
			}
			transportChains.add(chainBuilder.build());
		}
		TSPPlan newPlan = new TSPPlan(transportChains);
		tsp.getPlans().remove(oldPlan);
		tsp.getPlans().add(newPlan);
		tsp.setSelectedPlan(newPlan);
	}

	private CarrierOffer pickOffer(Id sourceLinkId, Id destinationLinkId, int size) {
		ArrayList<CarrierOffer> offers = new ArrayList<CarrierOffer>(carrierAgentTracker.getOffers(sourceLinkId, destinationLinkId, size));
		Collections.sort(offers, new Comparator<CarrierOffer>() {

			@Override
			public int compare(CarrierOffer arg0, CarrierOffer arg1) {
				return arg0.getPrice().compareTo(arg1.getPrice());
			}
			
		});
		CarrierOffer bestOffer = offers.get(0);
		logger.info("pick carrierId=" + offers.get(0).getId() + " for " + sourceLinkId + " to " + destinationLinkId + " with price of " + offers.get(0).getPrice());
		return bestOffer;
	}
	
}
