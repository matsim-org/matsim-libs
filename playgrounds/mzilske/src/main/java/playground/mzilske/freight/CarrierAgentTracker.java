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
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.handler.ActivityEndEventHandler;
import org.matsim.core.api.experimental.events.handler.ActivityStartEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.population.algorithms.PlanAlgorithm;

import com.sleepycat.je.rep.monitor.NewMasterEvent;

import playground.mrieser.core.mobsim.api.AgentSource;
import playground.mrieser.core.mobsim.api.PlanAgent;
import playground.mrieser.core.mobsim.impl.DefaultPlanAgent;
import playground.mzilske.freight.CarrierTotalCostListener.CarrierCostEvent;
import playground.mzilske.freight.api.CarrierAgentFactory;

public class CarrierAgentTracker implements AgentSource, ActivityEndEventHandler, LinkEnterEventHandler, ActivityStartEventHandler {
	
	private static Logger logger = Logger.getLogger(CarrierAgentTracker.class);
	
	private Collection<CarrierImpl> carriers;

	private Collection<CarrierAgent> carrierAgents = new ArrayList<CarrierAgent>();
	
	private Collection<CarrierCostListener> costListeners = new ArrayList<CarrierCostListener>();
	
	private Collection<ShipmentStatusListener> shipmentStatusListeners = new ArrayList<ShipmentStatusListener>();
		
	double weight = 1;

	private PlanAlgorithm router;

	private Network network;

	private List<PlanAgent> agents;
	
	private double sumOfTotalDistance = 0.0;
	
	private CarrierAgentFactory carrierAgentFactory;

	private Collection<CarrierTotalCostListener> totalCostListeners = new ArrayList<CarrierTotalCostListener>();
	
	private Collection<CarrierEventListener> eventListeners = new ArrayList<CarrierEventListener>();


	public Collection<CarrierEventListener> getEventListeners() {
		return eventListeners;
	}


	public Collection<CarrierTotalCostListener> getTotalCostListeners() {
		return totalCostListeners;
	}
	

	public CarrierAgentTracker(Collection<CarrierImpl> carriers, PlanAlgorithm router, Network network, CarrierAgentFactory carrierAgentFactory) {
		this.carriers = carriers;
		this.router = router;
		this.network = network;
		this.carrierAgentFactory = carrierAgentFactory;
		createCarrierAgents();
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
		sumOfTotalDistance = 0.0;
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
		sumOfTotalDistance += distance/1000;
//		logger.info("Link Enter: " + linkId + " CarrierId=" + event.getPersonId() + " length="+ distance);
		// logger.info("totalDistanceTraveled = " + sumOfTotalDistance + " km");
		
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

//	public void calculateCostsScoreCarriersAndInform() {
//		//inclusive cost per shipment
//		for(CarrierAgent carrierAgent : carrierAgents){
//			carrierAgent.scoreSelectedPlan();
//			List<Tuple<Shipment,Double>> shipmentCostTuple = carrierAgent.calculateCostsPerShipment();
//			for(Tuple<Shipment,Double> t : shipmentCostTuple){
//				informCostListeners(t.getFirst(),t.getSecond());
//			}
//		}
//		
//	}

	public Collection<CarrierCostListener> getCostListeners() {
		return costListeners;
	}

	public Collection<ShipmentStatusListener> getShipmentStatusListeners() {
		return shipmentStatusListeners;
	}

	private void createCarrierAgents() {
		for (CarrierImpl carrier : carriers) {
			CarrierAgent carrierAgent = carrierAgentFactory.createAgent(this,carrier);
			carrierAgents.add(carrierAgent);
		}
	}

	public void notifyPickup(Shipment shipment, double time) {
		for (ShipmentStatusListener listener: shipmentStatusListeners) {
			listener.shipmentPickedUp(shipment, time);
		}
	}

	public void notifyDelivery(Shipment shipment, double time) {
		for (ShipmentStatusListener listener: shipmentStatusListeners) {
			listener.shipmentDelivered(shipment, time);
		}
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
			CarrierImpl carrier = findCarrier(c.getOffer().getId());
			if(carrier != null){
				carrier.getContracts().remove(c);
				logger.info("remove contract: " + c.getShipment());
			}
			else{
				logger.warn("contract " + c.getShipment() + " could not be removed. No carrier found.");
			}
		}
	}

	private CarrierImpl findCarrier(Id carrierId) {
		for(CarrierImpl carrier : carriers){
			if(carrier.getId().equals(carrierId)){
				return carrier;
			}
		}
		return null;
	}

	public void addContracts(Collection<Contract> contracts) {
		for(Contract c : contracts){
			CarrierImpl carrier = findCarrier(c.getOffer().getId());
			if(carrier != null){
				carrier.getContracts().add(c);
				logger.info("add contract: " + c.getShipment());
			}
			else{
				logger.warn("contract " + c.getShipment() + " could not be added. No carrier found.");
			}
		}
		
	}

	public CarrierImpl getCarrier(Id id) {
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

	public void memorizeCost(Id id, Id from, Id to, int size, Double cost) {
		CarrierAgent agent = findCarrierAgent(id);
		if(agent != null){
			agent.memorizeCost(from, to, size, cost);
		}
		
	}

	public void calculateCosts() {
		for(CarrierImpl carrier : carriers){
			CarrierAgent agent = findCarrierAgent(carrier.getId());
			agent.calculateCosts();
		}
		
	}
	

	public void informTotalCost(Id id, CarrierCostEvent costEvent) {
		for(CarrierTotalCostListener l : totalCostListeners ){
			l.inform(id,costEvent);
		}
	}
	
	public void processEvents(DriverEvent event){
		for(CarrierEventListener l : eventListeners){
			if(l instanceof DriverEventListener){
				((DriverEventListener)l).processEvent(event);
			}
		}
	}
	
}
