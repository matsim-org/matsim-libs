package playground.mzilske.freight;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;
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
import playground.mzilske.freight.events.OfferAcceptEvent;
import playground.mzilske.freight.events.OfferAcceptEventHandler;
import playground.mzilske.freight.events.OfferRejectEvent;
import playground.mzilske.freight.events.OfferRejectEventHandler;
import playground.mzilske.freight.events.QueryOffersEvent;
import playground.mzilske.freight.events.QueryOffersEventHandler;
import playground.mzilske.freight.events.ShipmentDeliveredEvent;
import playground.mzilske.freight.events.ShipmentPickedUpEvent;

public class CarrierAgentTracker implements AgentSource, ActivityEndEventHandler, LinkEnterEventHandler, ActivityStartEventHandler, QueryOffersEventHandler, OfferAcceptEventHandler, OfferRejectEventHandler {
	
	private static Logger logger = Logger.getLogger(CarrierAgentTracker.class);
	
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
	public void reset(int iteration) {
		resetCarrierAgents();
	}

	private void resetCarrierAgents() {
		for(CarrierAgent cA : carrierAgents){
			cA.reset();	
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
	
	public Collection<CarrierOffer> getOffers(Id linkId, Id linkId2, int shipmentSize, double startPickup, double endPickup, double startDelivery, double endDelivery) {
		Collection<CarrierOffer> offers = new ArrayList<CarrierOffer>();
		for (CarrierAgent carrierAgent : carrierAgents) {
			CarrierOffer offer = carrierAgent.requestOffer(linkId, linkId2, shipmentSize, startPickup, endPickup, startDelivery, endDelivery);
			if(offer instanceof NoOffer){
				continue;
			}
			else {
				offers.add(offer);
			}
		}
		return offers;
	}

	public void removeContracts(Collection<Contract> contracts) {
		for(Contract c : contracts){
			Carrier carrier = findCarrier(c.getOffer().getId());
			if(carrier != null){
				carrier.getContracts().remove(c);
				logger.info("remove contract: " + c.getShipment());
			}
			else{
				logger.warn("contract " + c.getShipment() + " could not be removed. No carrier found.");
			}
		}
	}

	private Carrier findCarrier(Id carrierId) {
		for(Carrier carrier : carriers){
			if(carrier.getId().equals(carrierId)){
				return carrier;
			}
		}
		return null;
	}

	public void addContracts(Collection<Contract> contracts) {
		for(Contract c : contracts){
			Carrier carrier = findCarrier(c.getOffer().getId());
			if(carrier != null){
				carrier.getContracts().add(c);
				logger.info("add contract: " + c.getShipment());
			}
			else{
				logger.warn("contract " + c.getShipment() + " could not be added. No carrier found.");
			}
		}
		
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
	public void handleEvent(QueryOffersEvent event) {
		for (CarrierAgent carrierAgent : carrierAgents) {
			CarrierOffer offer = carrierAgent.requestOffer(event.getService().getFrom(), event.getService().getTo(), event.getService().getSize(), 
					event.getService().getStartPickup(), event.getService().getEndPickup(), event.getService().getStartDelivery(), event.getService().getEndDelivery());
			if(offer instanceof NoOffer){
				continue;
			}
			else {
				event.getOffers().add(offer);
			}
		}
	}

	@Override
	public void handleEvent(OfferRejectEvent event) {
		Id carrierId = event.getOffer().getId();
		CarrierAgent agent = findCarrierAgent(carrierId);
		agent.informOfferRejected(event.getOffer());
	}

	@Override
	public void handleEvent(OfferAcceptEvent event) {
		Id carrierId = event.getContract().getOffer().getId();
		CarrierAgent agent = findCarrierAgent(carrierId);
		agent.informOfferAccepted(event.getContract());
		
	}
}
