package playground.mzilske.freight;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.api.experimental.events.ActivityEndEvent;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.handler.ActivityEndEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.mrieser.core.mobsim.api.AgentSource;
import playground.mrieser.core.mobsim.api.PlanAgent;
import playground.mrieser.core.mobsim.impl.DefaultPlanAgent;

public class CarrierAgentTracker implements AgentSource, ActivityEndEventHandler, LinkEnterEventHandler {
	
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
	
	public CarrierAgentTracker(Collection<CarrierImpl> carriers, PlanAlgorithm router) {
		this.carriers = carriers;
		this.router = router;
		createCarrierAgents();
		agents = createPlanAgents();
	}

	@Override
	public List<PlanAgent> getAgents() {
		return agents;
	}

	private List<PlanAgent> createPlanAgents() {
		List<PlanAgent> agents = new ArrayList<PlanAgent>();
		for (CarrierAgent carrierAgent : carrierAgents) {
			List<Plan> plans = carrierAgent.createFreightDriverPlans();
			for (Plan plan : plans) {
				PlanAgent planAgent = new DefaultPlanAgent(plan, weight);
				agents.add(planAgent);
			}
		}
		return agents;
	}

	public void setNetwork(Network network) {
		this.network = network;
	}

	@Override
	public void reset(int iteration) {
		
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		Id personId = event.getPersonId();
		String activityType = event.getActType();
		for (CarrierAgent carrierAgent : carrierAgents) {
			if (carrierAgent.getDriverIds().contains(personId)) {
				carrierAgent.activityOccurs(personId, activityType, event.getTime());
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
			}
		}
		sumOfTotalDistance += distance/1000;
		logger.info("totalDistanceTraveled = " + sumOfTotalDistance + " km");
		
	}

	public void calculateCostsScoreCarriersAndInform() {
		//inclusive cost per shipment
		for(CarrierAgent carrierAgent : carrierAgents){
			carrierAgent.scoreSelectedPlan();
			List<Tuple<Shipment,Double>> shipmentCostTuple = carrierAgent.calculateCostsOfSelectedPlanPerShipment();
			for(Tuple<Shipment,Double> t : shipmentCostTuple){
				informCostListeners(t.getFirst(),t.getSecond());
			}
		}
		
	}

	public Collection<CarrierCostListener> getCostListeners() {
		return costListeners;
	}

	public Collection<ShipmentStatusListener> getShipmentStatusListeners() {
		return shipmentStatusListeners;
	}

	private void informCostListeners(Shipment shipment, Double cost) {
		for(CarrierCostListener cl : costListeners){
			cl.informCost(shipment, cost);
		}
	}

	private void createCarrierAgents() {
		for (CarrierImpl carrier : carriers) {
			CarrierAgent carrierAgent = new CarrierAgent(this, carrier, router);
			carrierAgent.setCostAllocator(new CostAllocator());
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
	
}
