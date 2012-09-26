package org.matsim.contrib.freight.mobsim;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierScoringFunctionFactory;
import org.matsim.contrib.freight.carrier.CarrierShipment;
import org.matsim.contrib.freight.carrier.Carriers;
import org.matsim.contrib.freight.carrier.FreightConstants;
import org.matsim.contrib.freight.events.ShipmentDeliveredEvent;
import org.matsim.contrib.freight.events.ShipmentPickedUpEvent;
import org.matsim.core.api.experimental.events.ActivityEndEvent;
import org.matsim.core.api.experimental.events.ActivityStartEvent;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.Event;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.ActivityEndEventHandler;
import org.matsim.core.api.experimental.events.handler.ActivityStartEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
import org.matsim.core.events.EventsUtils;

public class CarrierAgentTracker implements ActivityStartEventHandler, ActivityEndEventHandler, AgentDepartureEventHandler, AgentArrivalEventHandler, LinkLeaveEventHandler, LinkEnterEventHandler {

	private final Carriers carriers;

	private final Network network;

	private final EventsManager eventsManager;

	private final Collection<CarrierAgent> carrierAgents = new ArrayList<CarrierAgent>();
	
	private Map<Id,CarrierAgent> driverAgentMap = new HashMap<Id, CarrierAgent>();

	public CarrierAgentTracker(Carriers carriers, Network network, CarrierScoringFunctionFactory carrierScoringFunctionFactory) {
		this.carriers = carriers;
		this.network = network;
		createCarrierAgents(carrierScoringFunctionFactory);
		eventsManager = EventsUtils.createEventsManager();
	}

	private void createCarrierAgents(CarrierScoringFunctionFactory carrierScoringFunctionFactory) {
		for (Carrier carrier : carriers.getCarriers().values()) {
			CarrierAgent carrierAgent = new CarrierAgent(this, carrier, carrierScoringFunctionFactory);
			carrierAgents.add(carrierAgent);
		}
	}

	public EventsManager getEventsManager() {
		return eventsManager;
	}

	public Collection<Plan> createPlans() {
		List<Plan> plans = new ArrayList<Plan>();
		for (CarrierAgent carrierAgent : carrierAgents) {
			List<Plan> plansForCarrier = carrierAgent.createFreightDriverPlans();
			plans.addAll(plansForCarrier);
		}
		return plans;
	}

	public void scoreSelectedPlans() {
		for (Carrier carrier : carriers.getCarriers().values()) {
			CarrierAgent agent = findCarrierAgent(carrier.getId());
			agent.scoreSelectedPlan();
		}
	}

	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub

	}

	private CarrierAgent findCarrierAgent(Id id) {
		for (CarrierAgent agent : carrierAgents) {
			if (agent.getId().equals(id)) {
				return agent;
			}
		}
		return null;
	}

	private void processEvent(Event event) {
		eventsManager.processEvent(event);
	}

	public void notifyPickedUp(Id carrierId, Id driverId, CarrierShipment shipment, double time) {
		processEvent(new ShipmentPickedUpEvent(carrierId, driverId, shipment, time));
	}

	public void notifyDelivered(Id carrierId, Id driverId, CarrierShipment shipment, double time) {
		processEvent(new ShipmentDeliveredEvent(carrierId, driverId, shipment,time));
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		CarrierAgent carrierAgent = getCarrierAgent(event.getPersonId());
		if(carrierAgent == null) return;
		carrierAgent.handleEvent(event);
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		CarrierAgent carrierAgent = getCarrierAgent(event.getPersonId());
		if(carrierAgent == null) return;
		carrierAgent.handleEvent(event);
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		CarrierAgent carrierAgent = getCarrierAgent(event.getPersonId());
		if(carrierAgent == null) return;
		carrierAgent.handleEvent(event);
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		CarrierAgent carrierAgent = getCarrierAgent(event.getPersonId());
		if(carrierAgent == null) return;
		carrierAgent.handleEvent(event);
	}

	@Override
	public void handleEvent(AgentArrivalEvent event) {
		CarrierAgent carrierAgent = getCarrierAgent(event.getPersonId());
		if(carrierAgent == null) return;
		carrierAgent.handleEvent(event);
	}

	@Override
	public void handleEvent(AgentDepartureEvent event) {
		CarrierAgent carrierAgent = getCarrierAgent(event.getPersonId());
		if(carrierAgent == null) return;
		carrierAgent.handleEvent(event);
	}

	private CarrierAgent getCarrierAgent(Id driverId) {
		if(driverAgentMap.containsKey(driverId)){
			return driverAgentMap.get(driverId);
		}
		for(CarrierAgent ca : carrierAgents){
			if(ca.getDriverIds().contains(driverId)){
				driverAgentMap.put(driverId, ca);
				return ca;
			}
		}
		return null;	
	}
	
	CarrierShipment getAssociatedShipment(Id driverId, Activity act, int planElementIndex){
		if(!(act.getType().equals(FreightConstants.PICKUP) || act.getType().equals(FreightConstants.DELIVERY))){
			return null;
		}
		else{
			CarrierAgent a = getCarrierAgent(driverId);
			return a.getShipment(driverId,act,planElementIndex);
		}
	}

}
