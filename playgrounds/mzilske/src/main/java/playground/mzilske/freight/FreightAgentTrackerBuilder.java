package playground.mzilske.freight;

import java.util.ArrayList;
import java.util.Collection;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.population.algorithms.PlanAlgorithm;


public class FreightAgentTrackerBuilder {
	
	private Collection<CarrierImpl> carriers;
	
	private PlanAlgorithm router;

	private EventsManager eventsManager;
	
	private Network network;
	
	private Collection<CarrierCostListener> costListeners = new ArrayList<CarrierCostListener>();
	
	private boolean carriersInitialized = false;
	
	private boolean routerInitialized = false;
	
	private boolean networkInitialized = false;
	
	private boolean eventsManagerInitialized = false;
	
	public void setCarriers(Collection<CarrierImpl> carriers){
		this.carriers = carriers;
		carriersInitialized=true;
	}
	
	public void setRouter(PlanAlgorithm router) {
		this.router = router;
		routerInitialized=true;
	}

	public void setEventsManager(EventsManager eventsManager) {
		this.eventsManager = eventsManager;
		eventsManagerInitialized=true;
	}

	public void setNetwork(Network network) {
		this.network = network;
		networkInitialized=true;
	}
	
	public void addCarrierCostListener(CarrierCostListener costListener){
		costListeners.add(costListener);
	}

	public FreightAgentTracker build(){
		if(carriersInitialized && routerInitialized && networkInitialized && eventsManagerInitialized){
			FreightAgentTracker tracker = new FreightAgentTracker(carriers, router, eventsManager);
			tracker.setNetwork(network);
			for(CarrierCostListener ccl : costListeners){
				tracker.getCostListeners().add(ccl);
			}
			return tracker;
		}
		throw new IllegalStateException("FreightAgentTracker has not been initialised correctly");
	}
}
