package playground.mzilske.freight.carrier;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.api.experimental.events.ActivityEndEvent;
import org.matsim.core.api.experimental.events.ActivityStartEvent;
import org.matsim.core.api.experimental.events.Event;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.handler.ActivityEndEventHandler;
import org.matsim.core.api.experimental.events.handler.ActivityStartEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.events.EventsUtils;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.mrieser.core.mobsim.api.AgentSource;
import playground.mrieser.core.mobsim.api.PlanAgent;
import playground.mrieser.core.mobsim.impl.DefaultPlanAgent;
import playground.mzilske.freight.api.CarrierAgentFactory;
import playground.mzilske.freight.events.ShipmentDeliveredEvent;
import playground.mzilske.freight.events.ShipmentPickedUpEvent;

public class CarrierAgentTracker implements AgentSource, ActivityEndEventHandler, LinkEnterEventHandler, ActivityStartEventHandler
	 {
	
	private Collection<Carrier> carriers;

	private Collection<CarrierAgent> carrierAgents = new ArrayList<CarrierAgent>();
		
	double weight = 1;

	private Network network;

	private List<PlanAgent> agents;
	
	private CarrierAgentFactory carrierAgentFactory;
	
	private EventsManager eventsManager; 

	public CarrierAgentTracker(Collection<Carrier> carriers, PlanAlgorithm router, Network network, CarrierAgentFactory carrierAgentFactory) {
		this.carriers = carriers;
		this.network = network;
		this.carrierAgentFactory = carrierAgentFactory;
		createCarrierAgents();
		eventsManager = EventsUtils.createEventsManager();
	}
	
	public EventsManager getEventsManager(){
		return eventsManager;
	}

	public void processEvent(Event event){
		eventsManager.processEvent(event);
	}
	
	@Override
	public List<PlanAgent> getAgents() {
		return agents;
	}

	public void createPlanAgents() {
		agents = new ArrayList<PlanAgent>();
		for (CarrierAgent carrierAgent : carrierAgents) {
			List<Plan> plans = carrierAgent.createFreightDriverPlans();
			for (Plan plan : plans) {
				PlanAgent planAgent = new DefaultPlanAgent(plan, weight);
				agents.add(planAgent);
			}
		}
	}



	@Override
	public void handleEvent(ActivityEndEvent event) {
		Id personId = event.getPersonId();
		String activityType = event.getActType();
		for (CarrierAgent carrierAgent : carrierAgents) {
			if (carrierAgent.getDriverIds().contains(personId)) {
				carrierAgent.activityEndOccurs(personId, activityType, event.getTime());
			}
		}
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		Id personId = event.getPersonId();
		Id linkId = event.getLinkId();
		double distance = network.getLinks().get(linkId).getLength();
		for (CarrierAgent carrierAgent : carrierAgents) {
			if (carrierAgent.getDriverIds().contains(personId)) {
				carrierAgent.tellDistance(personId, distance);
				carrierAgent.tellLink(personId, linkId);
			}
		}		
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		Id personId = event.getPersonId();
		String activityType = event.getActType();
		for (CarrierAgent carrierAgent : carrierAgents) {
			if (carrierAgent.getDriverIds().contains(personId)) {
				carrierAgent.activityStartOccurs(personId, activityType, event.getTime());
			}
		}
	}

	private void createCarrierAgents() {
		for (Carrier carrier : carriers) {
			CarrierAgent carrierAgent = carrierAgentFactory.createAgent(this,carrier);
			carrierAgents.add(carrierAgent);
		}
	}

	public void notifyPickup(Id carrierId, Id driverId, Shipment shipment, double time) {
		processEvent(new ShipmentPickedUpEvent(carrierId, driverId, shipment, time));
	}

	public void notifyDelivery(Id carrierId, Id driverId, Shipment shipment, double time) {
		processEvent(new ShipmentDeliveredEvent(carrierId, driverId, shipment, time));
	}
	
	private Carrier findCarrier(Id carrierId) {
		for(Carrier carrier : carriers){
			if(carrier.getId().equals(carrierId)){
				return carrier;
			}
		}
		return null;
	}

	public Carrier getCarrier(Id id) {
		return findCarrier(id);
	}
	
	private CarrierAgent findCarrierAgent(Id id) {
		for(CarrierAgent agent : carrierAgents){
			if(agent.getId().equals(id)){
				return agent;
			}
		}
		return null;
	}

	public void calculateCosts() {
		for(Carrier carrier : carriers){
			CarrierAgent agent = findCarrierAgent(carrier.getId());
			agent.calculateCosts();
		}
		
	}

	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub
		
	}

}
